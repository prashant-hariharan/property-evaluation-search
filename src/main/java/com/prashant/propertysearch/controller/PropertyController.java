package com.prashant.propertysearch.controller;

import com.prashant.propertysearch.dto.property.PropertyCreateRequest;
import com.prashant.propertysearch.dto.property.PropertyDetailsResponse;
import com.prashant.propertysearch.dto.property.PropertyResponse;
import com.prashant.propertysearch.dto.property.PropertyUpdateRequest;
import com.prashant.propertysearch.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for managing properties through CRUD operations.
 * Provides endpoints for creating, retrieving, updating, and deleting property resources.
 * Supports optional inclusion of associated evaluations when retrieving property details.
 *
 * @author prashant
 */
@RestController
@RequestMapping("/api/properties")
@Tag(name = "Properties", description = "CRUD operations for properties")
@AllArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    @Operation(summary = "Create property")
    public ResponseEntity<PropertyResponse> create(@Valid @RequestBody PropertyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(propertyService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get property by id", description = "Optionally include evaluations with includeEvaluations=true")
    public PropertyDetailsResponse getById(
            @PathVariable UUID id,
            @Parameter(description = "If true, includes evaluations linked to the property")
            @RequestParam(defaultValue = "false") boolean includeEvaluations
    ) {
        return propertyService.getById(id, includeEvaluations);
    }


    @PutMapping("/{id}")
    @Operation(summary = "Update property")
    public PropertyResponse update(@PathVariable UUID id, @Valid @RequestBody PropertyUpdateRequest request) {
        return propertyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete property")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
