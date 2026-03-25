package com.prashant.propertysearch.controller;

import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationCreateRequest;
import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationResponse;
import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationUpdateRequest;
import com.prashant.propertysearch.service.PropertyEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for managing property evaluations.
 * Provides endpoints for creating, retrieving, updating, and deleting property evaluations.
 *
 * @author prashant
 */
@RestController
@RequestMapping("/api/evaluations")
@Tag(name = "Property Evaluations", description = "CRUD operations for property evaluations")
@AllArgsConstructor
public class PropertyEvaluationController {

    private final PropertyEvaluationService propertyEvaluationService;

    @PostMapping
    @Operation(summary = "Create evaluation")
    public ResponseEntity<PropertyEvaluationResponse> create(
            @Valid @RequestBody PropertyEvaluationCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propertyEvaluationService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get evaluation by id")
    public PropertyEvaluationResponse getById(@PathVariable UUID id) {
        return propertyEvaluationService.getById(id);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update evaluation")
    public PropertyEvaluationResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody PropertyEvaluationUpdateRequest request
    ) {
        return propertyEvaluationService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete evaluation")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        propertyEvaluationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
