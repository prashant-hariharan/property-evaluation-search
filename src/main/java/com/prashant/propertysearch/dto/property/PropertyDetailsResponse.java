package com.prashant.propertysearch.dto.property;

import com.prashant.propertysearch.dto.common.AuditableResponse;
import com.prashant.propertysearch.entity.PropertyType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class PropertyDetailsResponse extends AuditableResponse {
    private UUID id;
    private String title;
    private String address;
    private String city;
    private String postalCode;
    private BigDecimal areaInSquareMeter;
    private Integer constructionYear;
    private PropertyType propertyType;
    private String description;
    private List<PropertyEvaluationEmbeddedResponse> evaluations;
}
