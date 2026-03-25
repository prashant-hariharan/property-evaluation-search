package com.prashant.propertysearch.dto.evaluation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record PropertyEvaluationCreateRequest(
        @NotNull
        UUID propertyId,
        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal marketValue,
        @Size(max = 5000)
        String notes
) {
}
