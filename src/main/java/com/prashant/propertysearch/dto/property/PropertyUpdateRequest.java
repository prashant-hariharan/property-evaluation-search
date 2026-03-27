package com.prashant.propertysearch.dto.property;

import com.prashant.propertysearch.entity.PropertyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;

import java.math.BigDecimal;

public record PropertyUpdateRequest(
        @NotBlank
        @Size(max = 255)
        String title,
        @NotBlank
        @Size(max = 255)
        String address,
        @NotBlank
        @Size(max = 100)
        String city,
        @NotBlank
        @Size(max = 20)
        String postalCode,
        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal areaInSquareMeter,
        @NotNull
        @Min(1800)
        @Max(2100)
        Integer constructionYear,
        @NotNull
        PropertyType propertyType,
        @Size(max = 2000)
        String description,
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        BigDecimal latitude,
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        BigDecimal longitude
) {
    @AssertTrue(message = "latitude and longitude must both be provided together or both be omitted")
    public boolean isGeoCoordinatePairValid() {
        return (latitude == null && longitude == null) || (latitude != null && longitude != null);
    }
}
