package com.prashant.propertysearch.service.mariadb;

import com.prashant.propertysearch.dto.search.SearchHitResponse;
import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import com.prashant.propertysearch.entity.PropertyType;
import com.prashant.propertysearch.mapper.MariaDbFtsSearchMapper;
import com.prashant.propertysearch.repository.fulltextsearch.MariaDBFullTextSearchRepository;
import com.prashant.propertysearch.repository.fulltextsearch.MariaDbFtsSearchProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MariaDBFullTextSearchServiceTest {

    @Mock
    private MariaDBFullTextSearchRepository mariaDBFullTextSearchRepository;
    @Mock
    private MariaDbFtsSearchMapper mariaDbFtsSearchMapper;

    @InjectMocks
    private MariaDBFullTextSearchService mariaDBFullTextSearchService;

    @Test
    void search_mapsHitsAndTotalAndPassesPropertyType() {
        SearchRequest request = new SearchRequest(
                "school metro access",
                "Berlin",
                null,
                PropertyType.APARTMENT,
                null,
                null,
                null,
                null,
                10
        );
        MariaDbFtsSearchProjection row = org.mockito.Mockito.mock(MariaDbFtsSearchProjection.class);
        SearchHitResponse mappedHit = new SearchHitResponse();
        mappedHit.setPropertyId(UUID.randomUUID());

        when(mariaDBFullTextSearchRepository.searchByFullText(
                eq("school metro access"),
                eq("Berlin"),
                eq(null),
                eq("APARTMENT"),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).thenReturn(List.of(row));
        when(mariaDBFullTextSearchRepository.countByFullText(
                eq("school metro access"),
                eq("Berlin"),
                eq(null),
                eq("APARTMENT"),
                eq(null),
                eq(null),
                eq(null),
                eq(null)
        )).thenReturn(1L);
        when(mariaDbFtsSearchMapper.toSearchHit(row)).thenReturn(mappedHit);

        SearchResponse response = mariaDBFullTextSearchService.search(request);

        assertThat(response.getTotalHits()).isEqualTo(1);
        assertThat(response.getHits()).containsExactly(mappedHit);
        verify(mariaDbFtsSearchMapper).toSearchHit(row);
    }

    @Test
    void search_usesDefaultLimitWhenRequestLimitIsNull() {
        SearchRequest request = new SearchRequest(
                "metro access",
                "Berlin",
                null,
                PropertyType.APARTMENT,
                null,
                null,
                null,
                null,
                null
        );

        when(mariaDBFullTextSearchRepository.searchByFullText(
                any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(List.of());
        when(mariaDBFullTextSearchRepository.countByFullText(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(0L);

        mariaDBFullTextSearchService.search(request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(mariaDBFullTextSearchRepository).searchByFullText(
                any(), any(), any(), any(), any(), any(), any(), any(), pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }
}
