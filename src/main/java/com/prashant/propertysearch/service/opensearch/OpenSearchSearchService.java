package com.prashant.propertysearch.service.opensearch;

import com.prashant.propertysearch.dto.search.OpenSearchSearchResultDto;
import com.prashant.propertysearch.dto.search.SearchHitResponse;
import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import com.prashant.propertysearch.mapper.SearchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.prashant.propertysearch.utils.LuceneDocumentFields.AREA_IN_SQUARE_METER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.CITY;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.CITY_FILTER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.DESCRIPTION;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.EVALUATION_MARKET_VALUE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.GEO_POINT;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.POSTAL_CODE_FILTER;
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
    private final SearchMapper searchMapper;

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

        OpenSearchSearchResultDto responseBody;
        try {
            responseBody = openSearchRestClient.post()
                    .uri("/{index}/_search", indexName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(searchBody)
                    .retrieve()
                    .body(OpenSearchSearchResultDto.class);
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

    private SearchResponse parseSearchResponse(OpenSearchSearchResultDto responseBody) {
        if (responseBody == null || responseBody.getHits() == null) {
            throw new IllegalStateException("OpenSearch search response body is empty");
        }

        OpenSearchSearchResultDto.HitsDto hitsDto = responseBody.getHits();
        int totalHits = hitsDto.getTotal() == null || hitsDto.getTotal().getValue() == null
                ? 0
                : hitsDto.getTotal().getValue();

        List<SearchHitResponse> hits = new ArrayList<>();
        if (hitsDto.getHits() != null) {
            for (OpenSearchSearchResultDto.HitDto hitDto : hitsDto.getHits()) {
                OpenSearchSearchResultDto.SourceDto source = hitDto.getSource();
                SearchHitResponse hit;
                if (source != null) {
                    hit = searchMapper.toSearchHit(
                            source.getPropertyId(),
                            source.getTitle(),
                            source.getCity(),
                            source.getPostalCode(),
                            source.getPropertyType(),
                            source.getDescription(),
                            source.getLatitude(),
                            source.getLongitude(),
                            source.getAreaInSquareMeter(),
                            source.getEvaluationMarketValue(),
                            hitDto.getScore() == null ? 0.0f : hitDto.getScore().floatValue()
                    );
                } else {
                    hit = new SearchHitResponse();
                    hit.setScore(hitDto.getScore() == null ? 0.0f : hitDto.getScore().floatValue());
                }
                hits.add(hit);
            }
        }

        SearchResponse response = new SearchResponse();
        response.setTotalHits(totalHits);
        response.setHits(hits);
        return response;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
