package com.prashant.propertysearch.dto.search;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class SearchHitResponse {
    private UUID propertyId;
    private String title;
    private String city;
    private String postalCode;
    private String propertyType;
    private String description;
    private BigDecimal areaInSquareMeter;
    private BigDecimal evaluationMarketValue;
    private float score;
}
