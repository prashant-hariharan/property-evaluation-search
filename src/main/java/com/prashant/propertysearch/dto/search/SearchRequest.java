package com.prashant.propertysearch.dto.search;

import com.prashant.propertysearch.entity.PropertyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;

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
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        BigDecimal centerLatitude,
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        BigDecimal centerLongitude,
        @DecimalMin(value = "0.1")
        BigDecimal radiusInKilometers,
        @Min(1)
        @Max(1000)
        Integer limit
) {
    @AssertTrue(message = "centerLatitude, centerLongitude, and radiusInKilometers must be provided together or all omitted")
    public boolean isGeoFilterValid() {
        boolean noneProvided = centerLatitude == null && centerLongitude == null && radiusInKilometers == null;
        boolean allProvided = centerLatitude != null && centerLongitude != null && radiusInKilometers != null;
        return noneProvided || allProvided;
    }
}
