package com.prashant.propertysearch.controller;

import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import com.prashant.propertysearch.service.mariadb.MariaDBFullTextSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for performing full-text search operations on property data using MariaDB.
 *
 * This class provides an API endpoint for querying property-related data based on
 * various search criteria using MariaDB's full-text search capabilities.
 *
 * @author prashant
 */
@RestController
@RequestMapping("/api/mariadb-fts")
@Validated
@AllArgsConstructor
@Tag(name = "MariaDB FTS Search", description = "MariaDB full-text search operations")
public class MariaDBFullTextSearchController {

    private final MariaDBFullTextSearchService mariaDbFtsSearchService;

    @PostMapping("/search")
    @Operation(summary = "Search properties using MariaDB full-text search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return mariaDbFtsSearchService.search(request);
    }
}
