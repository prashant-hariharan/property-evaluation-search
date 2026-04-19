package com.prashant.propertysearch.dto.search;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReindexByIdsRequest(
        @NotEmpty
        List<@NotNull UUID> propertyIds
) {
}
