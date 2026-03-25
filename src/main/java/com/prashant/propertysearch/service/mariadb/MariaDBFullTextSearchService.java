package com.prashant.propertysearch.service.mariadb;

import com.prashant.propertysearch.dto.search.SearchHitResponse;
import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import com.prashant.propertysearch.mapper.MariaDbFtsSearchMapper;
import com.prashant.propertysearch.repository.fulltextsearch.MariaDbFtsSearchProjection;
import com.prashant.propertysearch.repository.fulltextsearch.MariaDBFullTextSearchRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class responsible for performing full-text search queries using MariaDB.
 * It integrates with a custom repository interface and a mapper to convert results
 * into application-specific response models.

 * The search results include:
 * - List of mapped search hits (transformed using a dedicated mapper).
 * - Total number of hits matching the query.
 *
 * @author prashant
 */
@Service
@Slf4j
@AllArgsConstructor
public class MariaDBFullTextSearchService {

    private static final int DEFAULT_LIMIT = 20;
    private final MariaDBFullTextSearchRepository mariaDBFullTextSearchRepository;
    private final MariaDbFtsSearchMapper mariaDbFtsSearchMapper;


    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request) {
        int limit = request.limit() == null ? DEFAULT_LIMIT : request.limit();
        Pageable pageable = PageRequest.of(0, limit);
        String propertyType = request.propertyType() == null ? null : request.propertyType().name();

        List<MariaDbFtsSearchProjection> rows = mariaDBFullTextSearchRepository.searchByFullText(
                request.queryText(),
                request.city(),
                request.postalCode(),
                propertyType,
                request.minAreaInSquareMeter(),
                request.maxAreaInSquareMeter(),
                request.minMarketValue(),
                request.maxMarketValue(),
                pageable
        );

        long totalHits = mariaDBFullTextSearchRepository.countByFullText(
                request.queryText(),
                request.city(),
                request.postalCode(),
                propertyType,
                request.minAreaInSquareMeter(),
                request.maxAreaInSquareMeter(),
                request.minMarketValue(),
                request.maxMarketValue()
        );

        List<SearchHitResponse> hits = rows.stream()
          .map(mariaDbFtsSearchMapper::toSearchHit).toList();
        SearchResponse response = new SearchResponse();
        response.setTotalHits((int) totalHits);
        response.setHits(hits);
        log.info("Executed MariaDB FTS search. queryText={} city={} postalCode={} propertyType={} limit={} totalHits={}",
                request.queryText(), request.city(), request.postalCode(), request.propertyType(), limit, totalHits);
        return response;
    }
}
