package com.prashant.propertysearch.dto.search;

import com.prashant.propertysearch.entity.PropertyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record SearchRequest(
        String queryText,
        @NotBlank
        String city,
        String postalCode,
        PropertyType propertyType,
        BigDecimal minAreaInSquareMeter,
        BigDecimal maxAreaInSquareMeter,
        BigDecimal minMarketValue,
        BigDecimal maxMarketValue,
        @Min(1)
        @Max(1000)
        Integer limit
) {
}
