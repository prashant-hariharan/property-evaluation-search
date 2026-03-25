package com.prashant.propertysearch.dto.property;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PropertyEvaluationEmbeddedResponse {
    private UUID id;
    private BigDecimal marketValue;
}
