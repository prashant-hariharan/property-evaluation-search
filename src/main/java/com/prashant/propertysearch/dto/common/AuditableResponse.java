package com.prashant.propertysearch.dto.common;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public abstract class AuditableResponse {
    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant modifiedAt;
}
