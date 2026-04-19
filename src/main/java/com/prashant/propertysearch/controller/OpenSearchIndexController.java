package com.prashant.propertysearch.controller;

import com.prashant.propertysearch.dto.search.ReindexByIdsRequest;
import com.prashant.propertysearch.dto.search.ReindexByIdsResponse;
import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import com.prashant.propertysearch.service.opensearch.OpenSearchIndexerService;
import com.prashant.propertysearch.service.opensearch.OpenSearchSearchService;
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

@RestController
@RequestMapping("/api/opensearch")
@Validated
@AllArgsConstructor
@Tag(name = "OpenSearch Index", description = "OpenSearch index operations")
public class OpenSearchIndexController {

    private final OpenSearchIndexerService openSearchIndexerService;
    private final OpenSearchSearchService openSearchSearchService;

    @PostMapping("/reindex")
    @Operation(summary = "Rebuild OpenSearch index from database")
    public ResponseEntity<Map<String, Object>> reindex() {
        int indexedDocuments = openSearchIndexerService.reindexAll();
        return ResponseEntity.ok(Map.of("indexedDocuments", indexedDocuments));
    }

    @PostMapping("/reindex-by-ids")
    @Operation(summary = "Reindex specific OpenSearch documents by property ids")
    public ResponseEntity<ReindexByIdsResponse> reindexByIds(
            @Valid @RequestBody ReindexByIdsRequest request
    ) {
        OpenSearchIndexerService.ReindexByIdsResult result = openSearchIndexerService.reindexByPropertyIds(request.propertyIds());
        return ResponseEntity.ok(new ReindexByIdsResponse(
                result.indexedDocuments(),
                result.failedPropertyIds()
        ));
    }

    @PostMapping("/search")
    @Operation(summary = "Search OpenSearch index")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return openSearchSearchService.search(request);
    }
}
