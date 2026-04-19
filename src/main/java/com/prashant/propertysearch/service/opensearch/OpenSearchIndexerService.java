package com.prashant.propertysearch.service.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.propertysearch.entity.Property;
import com.prashant.propertysearch.entity.PropertyEvaluation;
import com.prashant.propertysearch.repository.PropertyEvaluationRepository;
import com.prashant.propertysearch.repository.PropertyRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import static com.prashant.propertysearch.utils.LuceneDocumentUtils.nullSafe;

@Service
@Slf4j
public class OpenSearchIndexerService {

    private static final int REINDEX_PAGE_SIZE = 1000;
    private static final int REINDEX_PROGRESS_LOG_INTERVAL = 10;
    private static final String PROPERTY_TEXT_ANALYZER = "property_text_analyzer";

    private final PropertyRepository propertyRepository;
    private final PropertyEvaluationRepository propertyEvaluationRepository;
    private final RestClient openSearchRestClient;
    private final ObjectMapper objectMapper;
    private final String indexName;
    private final Map<String, Object> analysisSettings;

    public OpenSearchIndexerService(
            PropertyRepository propertyRepository,
            PropertyEvaluationRepository propertyEvaluationRepository,
            RestClient openSearchRestClient,
            ObjectMapper objectMapper,
            @Value("${app.opensearch.index-name:property-evaluation-search}") String indexName,
            @Qualifier("openSearchAnalysisSettings") Map<String, Object> analysisSettings
    ) {
        this.propertyRepository = propertyRepository;
        this.propertyEvaluationRepository = propertyEvaluationRepository;
        this.openSearchRestClient = openSearchRestClient;
        this.objectMapper = objectMapper;
        this.indexName = indexName;
        this.analysisSettings = analysisSettings;
    }

    @PostConstruct
    void init() {
        ensureIndexExists();
        log.info("OpenSearch indexer initialized for index={}", indexName);
    }

    @Transactional(readOnly = true)
    public synchronized int reindexAll() {
        long startNs = System.nanoTime();
        log.info("Starting OpenSearch reindex. index={} pageSize={}", indexName, REINDEX_PAGE_SIZE);
        ensureIndexExists();
        deleteAllDocuments();

        ReindexStats reindexStats = new ReindexStats();
        int batchNumber = 0;
        Pageable pageable = PageRequest.of(0, REINDEX_PAGE_SIZE);
        Page<UUID> page;

        do {
            page = propertyRepository.findAllPropertyIds(pageable);
            List<UUID> propertyIds = page.getContent();
            if (!propertyIds.isEmpty()) {
                batchNumber++;
                reindexPropertyIds(propertyIds, reindexStats);
                log.debug("Indexed OpenSearch batch={} idsInBatch={} totalIndexed={} totalFailed={}",
                        batchNumber,
                        propertyIds.size(),
                        reindexStats.indexedCount,
                        reindexStats.failedPropertyIds.size()
                );
                if (batchNumber % REINDEX_PROGRESS_LOG_INTERVAL == 0 || !page.hasNext()) {
                    log.info("OpenSearch reindex progress. batchesProcessed={} indexedDocuments={} failedDocuments={}",
                            batchNumber,
                            reindexStats.indexedCount,
                            reindexStats.failedPropertyIds.size()
                    );
                }
            }
            pageable = page.nextPageable();
        } while (page.hasNext());

        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
        log.info("OpenSearch reindex completed. indexedDocuments={} failedDocuments={} durationMs={}",
                reindexStats.indexedCount,
                reindexStats.failedPropertyIds.size(),
                durationMs
        );
        return reindexStats.indexedCount;
    }

    @Transactional(readOnly = true)
    public synchronized ReindexByIdsResult reindexByPropertyIds(List<UUID> propertyIds) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return new ReindexByIdsResult(0, List.of());
        }
        ensureIndexExists();

        ReindexStats reindexStats = new ReindexStats();
        reindexPropertyIds(propertyIds, reindexStats);
        log.info("OpenSearch reindex by ids completed. requestedIds={} indexedDocuments={} failedDocuments={}",
                propertyIds.size(),
                reindexStats.indexedCount,
                reindexStats.failedPropertyIds.size()
        );
        return new ReindexByIdsResult(reindexStats.indexedCount, List.copyOf(reindexStats.failedPropertyIds));
    }

    @Transactional
    public synchronized void upsertPropertyDocument(UUID propertyId) {
        ensureIndexExists();
        Property property = propertyRepository.findById(propertyId).orElse(null);
        if (property == null) {
            deletePropertyDocument(propertyId);
            log.debug("Removed OpenSearch document for missing propertyId={}", propertyId);
            return;
        }

        PropertyEvaluation latestEvaluation = propertyEvaluationRepository
                .findTopByPropertyIdOrderByCreatedAtDescIdDesc(propertyId)
                .orElse(null);

        try {
            openSearchRestClient.put()
                    .uri("/{index}/_doc/{id}?refresh=true", indexName, propertyId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toDocumentSource(property, latestEvaluation))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Upserted OpenSearch document for propertyId={}", propertyId);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Failed to upsert OpenSearch document for property: " + propertyId, e);
        }
    }

    public synchronized void deletePropertyDocument(UUID propertyId) {
        ensureIndexExists();
        try {
            openSearchRestClient.delete()
                    .uri("/{index}/_doc/{id}?refresh=true", indexName, propertyId)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Deleted OpenSearch document for propertyId={}", propertyId);
        } catch (HttpClientErrorException.NotFound ignored) {
            log.debug("OpenSearch document already absent for propertyId={}", propertyId);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Failed to delete OpenSearch document for property: " + propertyId, e);
        }
    }

    private void reindexPropertyIds(List<UUID> propertyIds, ReindexStats reindexStats) {
        if (propertyIds == null || propertyIds.isEmpty()) {
            return;
        }

        List<Property> properties = propertyRepository.findAllById(propertyIds);
        List<UUID> existingPropertyIds = properties.stream().map(Property::getId).toList();
        Map<UUID, PropertyEvaluation> latestEvaluationByPropertyId = existingPropertyIds.isEmpty()
                ? Map.of()
                : propertyEvaluationRepository.findLatestByPropertyIds(existingPropertyIds)
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getProperty().getId(),
                        e -> e,
                        this::selectLatestEvaluation
                ));

        bulkIndexProperties(properties, latestEvaluationByPropertyId, reindexStats);
    }

    private void bulkIndexProperties(
            List<Property> properties,
            Map<UUID, PropertyEvaluation> latestEvaluationByPropertyId,
            ReindexStats reindexStats
    ) {
        if (properties.isEmpty()) {
            return;
        }

        Map<UUID, Map<String, Object>> payloadByPropertyId = new LinkedHashMap<>();
        for (Property property : properties) {
            try {
                payloadByPropertyId.put(property.getId(), toDocumentSource(property, latestEvaluationByPropertyId.get(property.getId())));
            } catch (Exception e) {
                reindexStats.failedPropertyIds.add(property.getId());
                log.error("Skipping property during OpenSearch reindex. propertyId={} error={}",
                        property.getId(),
                        e.getMessage()
                );
            }
        }
        if (payloadByPropertyId.isEmpty()) {
            return;
        }

        StringBuilder body = new StringBuilder(payloadByPropertyId.size() * 256);
        try {
            for (Map.Entry<UUID, Map<String, Object>> entry : payloadByPropertyId.entrySet()) {
                body.append(objectMapper.writeValueAsString(Map.of("index", Map.of("_id", entry.getKey().toString())))).append('\n');
                body.append(objectMapper.writeValueAsString(entry.getValue())).append('\n');
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare OpenSearch bulk payload", e);
        }

        String responseBody;
        try {
            responseBody = openSearchRestClient.post()
                    .uri("/{index}/_bulk?refresh=true", indexName)
                    .contentType(MediaType.parseMediaType("application/x-ndjson"))
                    .body(body.toString())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Failed to execute OpenSearch bulk index operation", e);
        }

        handleBulkResponse(responseBody, payloadByPropertyId.keySet(), reindexStats);
    }

    private void handleBulkResponse(String responseBody, Iterable<UUID> requestedIds, ReindexStats reindexStats) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("OpenSearch bulk response body is empty");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            boolean hasErrors = root.path("errors").asBoolean(false);
            if (!hasErrors) {
                int successCount = 0;
                for (UUID ignored : requestedIds) {
                    successCount++;
                }
                reindexStats.indexedCount += successCount;
                return;
            }

            JsonNode items = root.path("items");
            if (!items.isArray()) {
                throw new IllegalStateException("OpenSearch bulk response missing items array");
            }
            for (JsonNode item : items) {
                JsonNode indexItem = item.path("index");
                String id = indexItem.path("_id").asText(null);
                int status = indexItem.path("status").asInt(500);
                if (status >= 200 && status < 300) {
                    reindexStats.indexedCount++;
                    continue;
                }
                if (id != null) {
                    reindexStats.failedPropertyIds.add(UUID.fromString(id));
                }
                log.error("OpenSearch bulk item failed. propertyId={} status={} error={}",
                        id,
                        status,
                        indexItem.path("error").toString()
                );
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse OpenSearch bulk response", e);
        }
    }

    private Map<String, Object> toDocumentSource(Property property, PropertyEvaluation latestEvaluation) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put(PROPERTY_ID, property.getId().toString());
        source.put(PROPERTY_TYPE, property.getPropertyType().name());
        source.put(TITLE, nullSafe(property.getTitle()));
        source.put(CITY, nullSafe(property.getCity()));
        source.put(DESCRIPTION, nullSafe(property.getDescription()));
        source.put(POSTAL_CODE, nullSafe(property.getPostalCode()));
        source.put(POSTAL_CODE_FILTER, normalizeFilterValue(property.getPostalCode()));
        source.put(CITY_FILTER, normalizeFilterValue(property.getCity()));
        source.put(AREA_IN_SQUARE_METER, property.getAreaInSquareMeter().doubleValue());

        if (property.getLatitude() != null && property.getLongitude() != null) {
            double latitude = property.getLatitude().doubleValue();
            double longitude = property.getLongitude().doubleValue();
            source.put(LATITUDE, latitude);
            source.put(LONGITUDE, longitude);
            source.put(GEO_POINT, Map.of("lat", latitude, "lon", longitude));
        }

        if (latestEvaluation != null && latestEvaluation.getMarketValue() != null) {
            source.put(EVALUATION_MARKET_VALUE, latestEvaluation.getMarketValue().doubleValue());
        }
        return source;
    }

    private PropertyEvaluation selectLatestEvaluation(PropertyEvaluation left, PropertyEvaluation right) {
        Comparator<PropertyEvaluation> comparator = Comparator.comparing(
                PropertyEvaluation::getCreatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())
        ).thenComparing(
                PropertyEvaluation::getId,
                Comparator.nullsFirst(Comparator.naturalOrder())
        );
        return comparator.compare(left, right) >= 0 ? left : right;
    }

    private void ensureIndexExists() {
        if (indexExists()) {
            return;
        }
        try {
            openSearchRestClient.put()
                    .uri("/{index}", indexName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createIndexDefinition())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Created OpenSearch index={}", indexName);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Failed to create OpenSearch index: " + indexName, e);
        }
    }

    private Map<String, Object> createIndexDefinition() {
        return Map.of(
                "settings", analysisSettings.get("settings"),
                "mappings", Map.of(
                        "properties", Map.ofEntries(
                                Map.entry(PROPERTY_ID, Map.of("type", "keyword")),
                                Map.entry(PROPERTY_TYPE, Map.of("type", "keyword")),
                                Map.entry(TITLE, Map.of("type", "text", "analyzer", PROPERTY_TEXT_ANALYZER)),
                                Map.entry(CITY, Map.of("type", "text", "analyzer", PROPERTY_TEXT_ANALYZER)),
                                Map.entry(DESCRIPTION, Map.of("type", "text", "analyzer", PROPERTY_TEXT_ANALYZER)),
                                Map.entry(POSTAL_CODE, Map.of("type", "keyword")),
                                Map.entry(POSTAL_CODE_FILTER, Map.of("type", "keyword")),
                                Map.entry(CITY_FILTER, Map.of("type", "keyword")),
                                Map.entry(AREA_IN_SQUARE_METER, Map.of("type", "double")),
                                Map.entry(EVALUATION_MARKET_VALUE, Map.of("type", "double")),
                                Map.entry(GEO_POINT, Map.of("type", "geo_point")),
                                Map.entry(LATITUDE, Map.of("type", "double")),
                                Map.entry(LONGITUDE, Map.of("type", "double"))
                        )
                )
        );
    }

    private boolean indexExists() {
        try {
            openSearchRestClient.head()
                    .uri("/{index}", indexName)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound ignored) {
            return false;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Failed to check OpenSearch index existence: " + indexName, e);
        }
    }

    private void deleteAllDocuments() {
        try {
            openSearchRestClient.post()
                    .uri("/{index}/_delete_by_query?refresh=true&conflicts=proceed", indexName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("query", Map.of("match_all", Map.of())))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Failed to clear OpenSearch index before reindex", e);
        }
    }

    private static final class ReindexStats {
        private int indexedCount;
        private final List<UUID> failedPropertyIds = new ArrayList<>();
    }

    public record ReindexByIdsResult(
            int indexedDocuments,
            List<UUID> failedPropertyIds
    ) {
    }
}
