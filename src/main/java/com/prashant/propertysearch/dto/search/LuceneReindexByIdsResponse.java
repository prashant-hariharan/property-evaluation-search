package com.prashant.propertysearch.dto.search;

import java.util.List;
import java.util.UUID;

public record LuceneReindexByIdsResponse(
        int indexedDocuments,
        List<UUID> failedPropertyIds
) {
}
