package com.prashant.propertysearch.dto.evaluation;

import com.prashant.propertysearch.dto.common.AuditableResponse;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PropertyEvaluationResponse extends AuditableResponse {
    private UUID id;
    private UUID propertyId;
    private BigDecimal marketValue;
    private String notes;
}
