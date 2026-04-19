package com.prashant.propertysearch.service.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.propertysearch.dto.search.SearchHitResponse;
import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.prashant.propertysearch.utils.LuceneDocumentFields.AREA_IN_SQUARE_METER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.CITY;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.CITY_FILTER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.DESCRIPTION;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.EVALUATION_MARKET_VALUE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.GEO_POINT;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.LATITUDE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.LONGITUDE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.POSTAL_CODE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.POSTAL_CODE_FILTER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.PROPERTY_ID;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.PROPERTY_TYPE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.TITLE;
import static com.prashant.propertysearch.utils.LuceneDocumentUtils.normalizeFilterValue;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenSearchSearchService {

    private static final float TITLE_BOOST = 5.0f;
    private static final float DESCRIPTION_BOOST = 2.0f;
    private static final float EXACT_QUERY_BOOST = 2.0f;
    private static final float FUZZY_QUERY_BOOST = 0.5f;

    private final RestClient openSearchRestClient;
    private final ObjectMapper objectMapper;

    @Value("${app.opensearch.index-name:property-evaluation-search}")
    private String indexName;

    @Value("${app.search.log-queries:true}")
    private boolean logSearchQueries;

    public SearchResponse search(SearchRequest request) {
        int limit = request.limit() == null ? 20 : request.limit();
        Map<String, Object> searchBody = buildSearchBody(request, limit);
        if (logSearchQueries) {
            log.info("Sending OpenSearch query. query={}", searchBody);
        }

        String responseBody;
        try {
            responseBody = openSearchRestClient.post()
                    .uri("/{index}/_search", indexName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(searchBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Failed to execute OpenSearch search", e);
        }

        return parseSearchResponse(responseBody);
    }

    private Map<String, Object> buildSearchBody(SearchRequest request, int limit) {
        List<Map<String, Object>> mustClauses = new ArrayList<>();
        List<Map<String, Object>> filterClauses = new ArrayList<>();

        mustClauses.add(buildTextQuery(request.queryText()));
        filterClauses.add(buildCityFilterQuery(request.city()));

        if (hasText(request.postalCode())) {
            filterClauses.add(Map.of(
                    "term", Map.of(POSTAL_CODE_FILTER, normalizeFilterValue(request.postalCode()))
            ));
        }
        if (request.propertyType() != null) {
            filterClauses.add(Map.of(
                    "term", Map.of(PROPERTY_TYPE, request.propertyType().name())
            ));
        }
        if (request.minAreaInSquareMeter() != null || request.maxAreaInSquareMeter() != null) {
            Map<String, Object> range = new LinkedHashMap<>();
            if (request.minAreaInSquareMeter() != null) {
                range.put("gte", request.minAreaInSquareMeter().doubleValue());
            }
            if (request.maxAreaInSquareMeter() != null) {
                range.put("lte", request.maxAreaInSquareMeter().doubleValue());
            }
            filterClauses.add(Map.of("range", Map.of(AREA_IN_SQUARE_METER, range)));
        }
        if (request.minMarketValue() != null || request.maxMarketValue() != null) {
            Map<String, Object> range = new LinkedHashMap<>();
            if (request.minMarketValue() != null) {
                range.put("gte", request.minMarketValue().doubleValue());
            }
            if (request.maxMarketValue() != null) {
                range.put("lte", request.maxMarketValue().doubleValue());
            }
            filterClauses.add(Map.of("range", Map.of(EVALUATION_MARKET_VALUE, range)));
        }
        if (request.centerLatitude() != null) {
            filterClauses.add(Map.of(
                    "geo_distance", Map.of(
                            "distance", request.radiusInKilometers().doubleValue() + "km",
                            GEO_POINT, Map.of(
                                    "lat", request.centerLatitude().doubleValue(),
                                    "lon", request.centerLongitude().doubleValue()
                            )
                    )
            ));
        }

        return Map.of(
                "size", limit,
                "query", Map.of(
                        "bool", Map.of(
                                "must", mustClauses,
                                "filter", filterClauses
                        )
                )
        );
    }

    private Map<String, Object> buildTextQuery(String queryText) {
        if (!hasText(queryText)) {
            return Map.of("match_all", Map.of());
        }

        String normalizedText = queryText.trim();
        return Map.of(
                "bool", Map.of(
                        "should", List.of(
                                Map.of(
                                        "multi_match", Map.of(
                                                "query", normalizedText,
                                                "fields", List.of(TITLE + "^" + TITLE_BOOST, DESCRIPTION + "^" + DESCRIPTION_BOOST, CITY),
                                                "operator", "and",
                                                "boost", EXACT_QUERY_BOOST
                                        )
                                ),
                                Map.of(
                                        "multi_match", Map.of(
                                                "query", normalizedText,
                                                "fields", List.of(TITLE + "^" + TITLE_BOOST, DESCRIPTION + "^" + DESCRIPTION_BOOST),
                                                "operator", "and",
                                                "fuzziness", "AUTO",
                                                "boost", FUZZY_QUERY_BOOST
                                        )
                                )
                        ),
                        "minimum_should_match", 1
                )
        );
    }

    private Map<String, Object> buildCityFilterQuery(String city) {
        return Map.of(
                "bool", Map.of(
                        "should", List.of(
                                Map.of("term", Map.of(CITY_FILTER, normalizeFilterValue(city))),
                                Map.of(
                                        "match", Map.of(
                                                CITY, Map.of(
                                                        "query", city,
                                                        "operator", "and",
                                                        "fuzziness", "AUTO"
                                                )
                                        )
                                )
                        ),
                        "minimum_should_match", 1
                )
        );
    }

    private SearchResponse parseSearchResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("OpenSearch search response body is empty");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode hitsNode = root.path("hits");
            int totalHits = hitsNode.path("total").path("value").asInt(0);

            List<SearchHitResponse> hits = new ArrayList<>();
            for (JsonNode hitNode : hitsNode.path("hits")) {
                JsonNode source = hitNode.path("_source");
                SearchHitResponse hit = new SearchHitResponse();
                String propertyId = source.path(PROPERTY_ID).asText(null);
                if (propertyId != null) {
                    hit.setPropertyId(UUID.fromString(propertyId));
                }
                hit.setTitle(source.path(TITLE).asText(null));
                hit.setCity(source.path(CITY).asText(null));
                hit.setPostalCode(source.path(POSTAL_CODE).asText(null));
                hit.setPropertyType(source.path(PROPERTY_TYPE).asText(null));
                hit.setDescription(source.path(DESCRIPTION).asText(null));
                hit.setLatitude(toBigDecimal(source.path(LATITUDE)));
                hit.setLongitude(toBigDecimal(source.path(LONGITUDE)));
                hit.setAreaInSquareMeter(toBigDecimal(source.path(AREA_IN_SQUARE_METER)));
                hit.setEvaluationMarketValue(toBigDecimal(source.path(EVALUATION_MARKET_VALUE)));
                hit.setScore((float) hitNode.path("_score").asDouble(0.0d));
                hits.add(hit);
            }

            SearchResponse response = new SearchResponse();
            response.setTotalHits(totalHits);
            response.setHits(hits);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenSearch search response", e);
        }
    }

    private BigDecimal toBigDecimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return BigDecimal.valueOf(node.asDouble());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
