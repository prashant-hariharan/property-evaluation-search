package com.prashant.propertysearch.repository.fulltextsearch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional(readOnly = true)
@Slf4j
public class MariaDBFullTextSearchRepositoryImpl implements MariaDBFullTextSearchRepository {

    private static final String SELECT_CLAUSE = """
            SELECT
                p.id AS propertyId,
                p.title AS title,
                p.city AS city,
                p.postal_code AS postalCode,
                p.property_type AS propertyType,
                p.description AS description,
                p.area_in_square_meter AS areaInSquareMeter,
                pe_latest.market_value AS evaluationMarketValue,
                CASE
                    WHEN :queryText IS NULL OR TRIM(:queryText) = '' THEN 0
                    ELSE MATCH(p.title, p.description) AGAINST(:queryText IN NATURAL LANGUAGE MODE)
                END AS score
            """;

    private static final String FROM_WHERE_CLAUSE = """
            FROM properties p
            LEFT JOIN (
                SELECT property_id, market_value
                FROM (
                    SELECT
                        pe.property_id,
                        pe.market_value,
                        ROW_NUMBER() OVER (
                            PARTITION BY pe.property_id
                            ORDER BY pe.created_at DESC, pe.id DESC
                        ) AS row_num
                    FROM property_evaluations pe
                ) latest_eval
                WHERE latest_eval.row_num = 1
            ) pe_latest ON pe_latest.property_id = p.id
            WHERE (
                :queryText IS NULL
                OR TRIM(:queryText) = ''
                OR MATCH(p.title, p.description) AGAINST(:queryText IN NATURAL LANGUAGE MODE) > 0
            )
            AND LOWER(p.city) = LOWER(:city)
            AND (:postalCode IS NULL OR TRIM(:postalCode) = '' OR p.postal_code = :postalCode)
            AND (:propertyType IS NULL OR p.property_type = :propertyType)
            AND (:minAreaInSquareMeter IS NULL OR p.area_in_square_meter >= :minAreaInSquareMeter)
            AND (:maxAreaInSquareMeter IS NULL OR p.area_in_square_meter <= :maxAreaInSquareMeter)
            AND (:minMarketValue IS NULL OR pe_latest.market_value >= :minMarketValue)
            AND (:maxMarketValue IS NULL OR pe_latest.market_value <= :maxMarketValue)
            """;

    private static final String ORDER_BY_CLAUSE = " ORDER BY score DESC, p.created_at DESC, p.id";
    private static final String SEARCH_SQL = SELECT_CLAUSE + FROM_WHERE_CLAUSE + ORDER_BY_CLAUSE;
    private static final String COUNT_SQL = "SELECT COUNT(*) " + FROM_WHERE_CLAUSE;

    @PersistenceContext
    private EntityManager entityManager;
    @Value("${app.search.log-queries:true}")
    private boolean logSearchQueries;

    @Override
    @SuppressWarnings("unchecked")
    public List<MariaDbFtsSearchProjection> searchByFullText(
            String queryText,
            String city,
            String postalCode,
            String propertyType,
            BigDecimal minAreaInSquareMeter,
            BigDecimal maxAreaInSquareMeter,
            BigDecimal minMarketValue,
            BigDecimal maxMarketValue,
            Pageable pageable
    ) {
        Map<String, Object> params = buildParams(queryText, city, postalCode, propertyType,
                minAreaInSquareMeter, maxAreaInSquareMeter, minMarketValue, maxMarketValue);
        if (logSearchQueries) {
            log.info("MariaDB FTS search SQL: {}", renderSqlForLogging(SEARCH_SQL, params));
        }
        Query query = entityManager.createNativeQuery(SEARCH_SQL);
        bindParameters(query, params);
        if (pageable != null) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }

        List<Object[]> rows = query.getResultList();
        return rows.stream().map(MariaDbFtsSearchRow::fromRow).map(MariaDbFtsSearchProjection.class::cast).toList();
    }

    @Override
    public long countByFullText(
            String queryText,
            String city,
            String postalCode,
            String propertyType,
            BigDecimal minAreaInSquareMeter,
            BigDecimal maxAreaInSquareMeter,
            BigDecimal minMarketValue,
            BigDecimal maxMarketValue
    ) {
        Map<String, Object> params = buildParams(queryText, city, postalCode, propertyType,
                minAreaInSquareMeter, maxAreaInSquareMeter, minMarketValue, maxMarketValue);
        if (logSearchQueries) {
            log.debug("MariaDB FTS count SQL: {}", renderSqlForLogging(COUNT_SQL, params));
        }
        Query query = entityManager.createNativeQuery(COUNT_SQL);
        bindParameters(query, params);
        Number count = (Number) query.getSingleResult();
        return count == null ? 0L : count.longValue();
    }

    private Map<String, Object> buildParams(
            String queryText,
            String city,
            String postalCode,
            String propertyType,
            BigDecimal minAreaInSquareMeter,
            BigDecimal maxAreaInSquareMeter,
            BigDecimal minMarketValue,
            BigDecimal maxMarketValue
    ) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("queryText", queryText);
        params.put("city", city);
        params.put("postalCode", postalCode);
        params.put("propertyType", propertyType);
        params.put("minAreaInSquareMeter", minAreaInSquareMeter);
        params.put("maxAreaInSquareMeter", maxAreaInSquareMeter);
        params.put("minMarketValue", minMarketValue);
        params.put("maxMarketValue", maxMarketValue);
        return params;
    }

    private void bindParameters(Query query, Map<String, Object> params) {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private String renderSqlForLogging(String sql, Map<String, Object> params) {
        String rendered = sql;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            rendered = rendered.replace(":" + entry.getKey(), sqlLiteral(entry.getValue()));
        }
        return rendered;
    }

    private String sqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        String escaped = value.toString().replace("'", "''");
        return "'" + escaped + "'";
    }

    private static final class MariaDbFtsSearchRow implements MariaDbFtsSearchProjection {
        private final byte[] propertyId;
        private final String title;
        private final String city;
        private final String postalCode;
        private final String propertyType;
        private final String description;
        private final BigDecimal areaInSquareMeter;
        private final BigDecimal evaluationMarketValue;
        private final Double score;

        private MariaDbFtsSearchRow(
                byte[] propertyId,
                String title,
                String city,
                String postalCode,
                String propertyType,
                String description,
                BigDecimal areaInSquareMeter,
                BigDecimal evaluationMarketValue,
                Double score
        ) {
            this.propertyId = propertyId;
            this.title = title;
            this.city = city;
            this.postalCode = postalCode;
            this.propertyType = propertyType;
            this.description = description;
            this.areaInSquareMeter = areaInSquareMeter;
            this.evaluationMarketValue = evaluationMarketValue;
            this.score = score;
        }

        private static MariaDbFtsSearchRow fromRow(Object[] row) {
            return new MariaDbFtsSearchRow(
                    (byte[]) row[0],
                    (String) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (String) row[5],
                    toBigDecimal(row[6]),
                    toBigDecimal(row[7]),
                    row[8] == null ? 0.0d : ((Number) row[8]).doubleValue()
            );
        }

        private static BigDecimal toBigDecimal(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal;
            }
            if (value instanceof Number number) {
                return BigDecimal.valueOf(number.doubleValue());
            }
            return new BigDecimal(value.toString());
        }

        @Override
        public byte[] getPropertyId() {
            return propertyId;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getCity() {
            return city;
        }

        @Override
        public String getPostalCode() {
            return postalCode;
        }

        @Override
        public String getPropertyType() {
            return propertyType;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public BigDecimal getAreaInSquareMeter() {
            return areaInSquareMeter;
        }

        @Override
        public BigDecimal getEvaluationMarketValue() {
            return evaluationMarketValue;
        }

        @Override
        public Double getScore() {
            return score;
        }
    }
}
