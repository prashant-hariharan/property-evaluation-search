package com.prashant.propertysearch.service.lucene;

import com.prashant.propertysearch.dto.search.SearchHitResponse;
import com.prashant.propertysearch.dto.search.SearchRequest;
import com.prashant.propertysearch.dto.search.SearchResponse;
import com.prashant.propertysearch.mapper.LuceneSearchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.prashant.propertysearch.utils.LuceneDocumentFields.AREA_IN_SQUARE_METER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.CITY;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.CITY_FILTER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.DESCRIPTION;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.EVALUATION_MARKET_VALUE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.POSTAL_CODE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.POSTAL_CODE_FILTER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.PROPERTY_ID;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.PROPERTY_TYPE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.SEARCH_FIELDS;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.TITLE;
import static com.prashant.propertysearch.utils.LuceneDocumentUtils.normalizeFilterValue;

/**
 * Service class for executing search queries using Apache Lucene.
 * Provides methods to build and execute queries on an indexed data source.
 * This service supports filtering and searching based on multiple criteria,
 * including textual and numerical fields.
 *
 * The class ensures thread-safety during search operations by synchronizing
 * the search method.
 *
 * @author prashant
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LuceneSearchService {
    private static final int FUZZY_MAX_EDITS = 1;
    private static final float TITLE_BOOST = 5.0f;
    private static final float DESCRIPTION_BOOST = 2f;
    private static final float EXACT_QUERY_BOOST = 2.0f;
    private static final float FUZZY_QUERY_BOOST = 0.5f;


    private final Directory directory;
    private final Analyzer analyzer;
    private final LuceneSearchMapper luceneSearchMapper;
    @Value("${app.search.log-queries:true}")
    private boolean logSearchQueries;


    public SearchResponse search(SearchRequest request) {
        int limit = request.limit() == null ? 20 : request.limit();
        try (DirectoryReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = buildQuery(request);
            if (logSearchQueries) {
                log.debug("Final Lucene query={}", query);
                log.info("Sending Lucene query. query={}", query);
            }
            TopDocs topDocs = searcher.search(query, limit);

            List<SearchHitResponse> hits = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document document = searcher.doc(scoreDoc.doc);
                hits.add(toHit(document, scoreDoc.score));
            }

            SearchResponse response = new SearchResponse();
            response.setTotalHits(Math.toIntExact(topDocs.totalHits.value));
            response.setHits(hits);
            log.debug("Executed Lucene search. queryText={} city={} postalCode={} propertyType={} limit={} totalHits={}",
                    request.queryText(), request.city(), request.postalCode(), request.propertyType(), limit, response.getTotalHits());
            return response;
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("Failed to execute Lucene search", e);
        }
    }

    private Query buildQuery(SearchRequest request) throws ParseException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        Query textQuery = buildTextQuery(request.queryText());
        builder.add(textQuery, BooleanClause.Occur.MUST);
        builder.add(buildCityFilterQuery(request.city()), BooleanClause.Occur.FILTER);

        if (hasText(request.postalCode())) {
            builder.add(
                    new TermQuery(new Term(POSTAL_CODE_FILTER, normalizeFilterValue(request.postalCode()))),
                    BooleanClause.Occur.FILTER
            );
        }
        if (request.propertyType() != null) {
            builder.add(
                    new TermQuery(new Term(PROPERTY_TYPE, request.propertyType().name())),
                    BooleanClause.Occur.FILTER
            );
        }

        if (request.minAreaInSquareMeter() != null || request.maxAreaInSquareMeter() != null) {
            double min = request.minAreaInSquareMeter() != null
                    ? request.minAreaInSquareMeter().doubleValue()
                    : Double.NEGATIVE_INFINITY;
            double max = request.maxAreaInSquareMeter() != null
                    ? request.maxAreaInSquareMeter().doubleValue()
                    : Double.POSITIVE_INFINITY;
            builder.add(
                    DoublePoint.newRangeQuery(AREA_IN_SQUARE_METER, min, max),
                    BooleanClause.Occur.FILTER
            );
        }

        if (request.minMarketValue() != null || request.maxMarketValue() != null) {
            double min = request.minMarketValue() != null
                    ? request.minMarketValue().doubleValue()
                    : Double.NEGATIVE_INFINITY;
            double max = request.maxMarketValue() != null
                    ? request.maxMarketValue().doubleValue()
                    : Double.POSITIVE_INFINITY;
            builder.add(
                    DoublePoint.newRangeQuery(EVALUATION_MARKET_VALUE, min, max),
                    BooleanClause.Occur.FILTER
            );
        }
        return builder.build();
    }

    private Query buildCityFilterQuery(String city) {
        BooleanQuery.Builder cityFilter = new BooleanQuery.Builder();
        cityFilter.add(
                new TermQuery(new Term(CITY_FILTER, normalizeFilterValue(city))),
                BooleanClause.Occur.SHOULD
        );
        List<String> cityTokens = analyzeTerms(city);
        if (!cityTokens.isEmpty()) {
            BooleanQuery.Builder fuzzyCity = new BooleanQuery.Builder();
            for (String token : cityTokens) {
                fuzzyCity.add(new FuzzyQuery(new Term(CITY, token), FUZZY_MAX_EDITS), BooleanClause.Occur.MUST);
            }
            cityFilter.add(fuzzyCity.build(), BooleanClause.Occur.SHOULD);
        }
        cityFilter.setMinimumNumberShouldMatch(1);
        return cityFilter.build();
    }

    private Query buildTextQuery(String queryText) throws ParseException {
        if (!hasText(queryText)) {
            return new MatchAllDocsQuery();
        }
        MultiFieldQueryParser parser = new MultiFieldQueryParser(
                SEARCH_FIELDS.toArray(new String[0]),
                analyzer,
                Map.of(
                        TITLE, TITLE_BOOST,
                        DESCRIPTION, DESCRIPTION_BOOST
                )
        );
        parser.setDefaultOperator(QueryParserBase.AND_OPERATOR);
        Query boostedExactQuery = parser.parse(QueryParserBase.escape(queryText.trim()));
        Query fuzzyQuery = buildFuzzyTextQuery(queryText);

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BoostQuery(boostedExactQuery, EXACT_QUERY_BOOST), BooleanClause.Occur.SHOULD);
        builder.add(new BoostQuery(fuzzyQuery, FUZZY_QUERY_BOOST), BooleanClause.Occur.SHOULD);
        builder.setMinimumNumberShouldMatch(1);
        return builder.build();
    }

    private Query buildFuzzyTextQuery(String queryText) {
        List<String> terms = analyzeTerms(queryText);
        if (terms.isEmpty()) {
            return new MatchAllDocsQuery();
        }

        BooleanQuery.Builder fuzzyQuery = new BooleanQuery.Builder();
        for (String term : terms) {
            BooleanQuery.Builder perTermQuery = new BooleanQuery.Builder();
            perTermQuery.add(new FuzzyQuery(new Term(TITLE, term), FUZZY_MAX_EDITS), BooleanClause.Occur.SHOULD);
            perTermQuery.add(new FuzzyQuery(new Term(DESCRIPTION, term), FUZZY_MAX_EDITS), BooleanClause.Occur.SHOULD);
            fuzzyQuery.add(perTermQuery.build(), BooleanClause.Occur.MUST);
        }
        return fuzzyQuery.build();
    }

    private List<String> analyzeTerms(String queryText) {
        Set<String> terms = new LinkedHashSet<>();
        try (TokenStream tokenStream = analyzer.tokenStream(TITLE, new StringReader(queryText))) {
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                terms.add(charTermAttribute.toString());
            }
            tokenStream.end();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to analyze text query terms", e);
        }
        return new ArrayList<>(terms);
    }

    private SearchHitResponse toHit(Document document, float score) {
        Double areaInSquareMeter = document.getField(AREA_IN_SQUARE_METER) == null
                ? null
                : document.getField(AREA_IN_SQUARE_METER).numericValue().doubleValue();
        Double evaluationMarketValue = document.getField(EVALUATION_MARKET_VALUE) == null
                ? null
                : document.getField(EVALUATION_MARKET_VALUE).numericValue().doubleValue();

        return luceneSearchMapper.toSearchHit(
                document.get(PROPERTY_ID),
                document.get(TITLE),
                document.get(CITY),
                document.get(POSTAL_CODE),
                document.get(PROPERTY_TYPE),
                document.get(DESCRIPTION),
                areaInSquareMeter,
                evaluationMarketValue,
                score
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


}
