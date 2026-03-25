package com.prashant.propertysearch.repository.fulltextsearch;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface MariaDBFullTextSearchRepository {

    List<MariaDbFtsSearchProjection> searchByFullText(
            String queryText,
            String city,
            String postalCode,
            String propertyType,
            BigDecimal minAreaInSquareMeter,
            BigDecimal maxAreaInSquareMeter,
            BigDecimal minMarketValue,
            BigDecimal maxMarketValue,
            Pageable pageable
    );

    long countByFullText(
            String queryText,
            String city,
            String postalCode,
            String propertyType,
            BigDecimal minAreaInSquareMeter,
            BigDecimal maxAreaInSquareMeter,
            BigDecimal minMarketValue,
            BigDecimal maxMarketValue
    );
}
