package com.prashant.propertysearch.controller;

import com.prashant.propertysearch.dto.search.LuceneReindexByIdsRequest;
import com.prashant.propertysearch.dto.search.LuceneReindexByIdsResponse;
import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import com.prashant.propertysearch.service.lucene.LuceneIndexerService;
import com.prashant.propertysearch.service.lucene.LuceneSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for handling Lucene indexing and search operations.
 *
 * This class provides endpoints for rebuilding the Lucene index, reindexing specific
 * documents, and performing search operations.
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
}
