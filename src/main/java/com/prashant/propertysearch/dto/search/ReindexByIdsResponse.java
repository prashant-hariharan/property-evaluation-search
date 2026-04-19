package com.prashant.propertysearch.dto.search;

import java.util.List;
import java.util.UUID;

public record ReindexByIdsResponse(
        int indexedDocuments,
        List<UUID> failedPropertyIds
) {
}
