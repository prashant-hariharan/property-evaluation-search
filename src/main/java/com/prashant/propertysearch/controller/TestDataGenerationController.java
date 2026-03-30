package com.prashant.propertysearch.controller;

import com.prashant.propertysearch.service.TestDataGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for synthetic benchmark test data generation.
 *
 * @author prashant
 */
@RestController
@RequestMapping("/api/test-data")
@Validated
@AllArgsConstructor
@Tag(name = "Test Data", description = "Synthetic test data generation operations")
public class TestDataGenerationController {

    private final TestDataGenerationService testDataGenerationService;

    @PostMapping("/generate")
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
