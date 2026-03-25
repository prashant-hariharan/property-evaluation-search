package com.prashant.propertysearch.controller;

import com.prashant.propertysearch.dto.search.LuceneReindexByIdsRequest;
import com.prashant.propertysearch.dto.search.LuceneReindexByIdsResponse;
import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import com.prashant.propertysearch.service.lucene.LuceneIndexerService;
import com.prashant.propertysearch.service.lucene.LuceneSearchService;
import com.prashant.propertysearch.service.TestDataGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for handling Lucene indexing and search operations.
 *
 * This class provides endpoints for rebuilding the Lucene index, reindexing specific
 * documents, performing search operations, and generating synthetic test data.
 *
 * @author prashant
 */
@RestController
@RequestMapping("/api/lucene")
@Validated
@AllArgsConstructor
@Tag(name = "Lucene Search", description = "Lucene index and search operations")
public class LuceneSearchController {

    private final LuceneIndexerService luceneIndexerService;
    private final LuceneSearchService luceneSearchService;
    private final TestDataGenerationService testDataGenerationService;

    @PostMapping("/reindex")
    @Operation(summary = "Rebuild Lucene index from database")
    public ResponseEntity<Map<String, Object>> reindex() {
        int indexedDocuments = luceneIndexerService.reindexAll();
        return ResponseEntity.ok(Map.of("indexedDocuments", indexedDocuments));
    }

    @PostMapping("/reindex-by-ids")
    @Operation(summary = "Reindex specific Lucene documents by property ids")
    public ResponseEntity<LuceneReindexByIdsResponse> reindexByIds(
            @Valid @RequestBody LuceneReindexByIdsRequest request
    ) {
        LuceneIndexerService.ReindexByIdsResult result = luceneIndexerService.reindexByPropertyIds(request.propertyIds());
        return ResponseEntity.ok(new LuceneReindexByIdsResponse(
                result.indexedDocuments(),
                result.failedPropertyIds()
        ));
    }

    @PostMapping("/search")
    @Operation(summary = "Search Lucene index")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return luceneSearchService.search(request);
    }

    @PostMapping("/test-data/generate")
    @Operation(summary = "Generate synthetic benchmark data and optionally reindex Lucene")
    public ResponseEntity<Map<String, Object>> generateTestData(
            @RequestParam(defaultValue = "100000") @Min(1) @Max(1000000) int propertyCount,
            @RequestParam(defaultValue = "100") @Min(100) @Max(10000) int batchSize,
            @RequestParam(defaultValue = "true") boolean clearExistingData,
            @RequestParam(defaultValue = "true") boolean reindexAfterGeneration
    ) {
        TestDataGenerationService.TestDataGenerationSummary summary = testDataGenerationService.generate(
                propertyCount,
                batchSize,
                clearExistingData,
                reindexAfterGeneration
        );
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestedProperties", summary.requestedProperties());
        response.put("createdProperties", summary.createdProperties());
        response.put("createdEvaluations", summary.createdEvaluations());
        response.put("reindexedDocuments", summary.reindexedDocuments());
        response.put("durationMs", summary.durationMs());
        return ResponseEntity.ok(response);
    }
}
