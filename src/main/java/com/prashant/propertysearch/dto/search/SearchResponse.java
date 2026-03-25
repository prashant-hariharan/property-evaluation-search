package com.prashant.propertysearch.dto.search;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchResponse {
    private int totalHits;
    private List<SearchHitResponse> hits;
}
