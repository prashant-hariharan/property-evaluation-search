package com.prashant.propertysearch.utils;

import com.prashant.propertysearch.entity.Property;
import com.prashant.propertysearch.entity.PropertyEvaluation;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.LatLonPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static com.prashant.propertysearch.utils.LuceneDocumentFields.AREA_IN_SQUARE_METER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.CITY;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.CITY_FILTER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.DESCRIPTION;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.EVALUATION_MARKET_VALUE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.GEO_POINT;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.LATITUDE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.LONGITUDE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.POSTAL_CODE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.POSTAL_CODE_FILTER;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.PROPERTY_ID;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.PROPERTY_TYPE;
import static com.prashant.propertysearch.utils.LuceneDocumentFields.TITLE;

public final class LuceneDocumentUtils {

    private LuceneDocumentUtils() {
    }

    public static Document toDocument(Property property, PropertyEvaluation latestEvaluation) {
        Document document = new Document();
        document.add(new StringField(PROPERTY_ID, property.getId().toString(), org.apache.lucene.document.Field.Store.YES));
        document.add(new StringField(PROPERTY_TYPE, property.getPropertyType().name(), org.apache.lucene.document.Field.Store.YES));

        document.add(new TextField(TITLE, nullSafe(property.getTitle()), org.apache.lucene.document.Field.Store.YES));
        document.add(new TextField(CITY, nullSafe(property.getCity()), org.apache.lucene.document.Field.Store.YES));
        document.add(new TextField(DESCRIPTION, nullSafe(property.getDescription()), org.apache.lucene.document.Field.Store.YES));

        document.add(new StoredField(POSTAL_CODE, nullSafe(property.getPostalCode())));
        document.add(new StringField(POSTAL_CODE_FILTER, normalizeFilterValue(property.getPostalCode()), org.apache.lucene.document.Field.Store.NO));
        document.add(new StringField(CITY_FILTER, normalizeFilterValue(property.getCity()), org.apache.lucene.document.Field.Store.NO));

        document.add(new DoublePoint(AREA_IN_SQUARE_METER, property.getAreaInSquareMeter().doubleValue()));
        document.add(new StoredField(AREA_IN_SQUARE_METER, property.getAreaInSquareMeter().doubleValue()));

        if (property.getLatitude() != null && property.getLongitude() != null) {
            double latitude = property.getLatitude().doubleValue();
            double longitude = property.getLongitude().doubleValue();
            document.add(new LatLonPoint(GEO_POINT, latitude, longitude));
            document.add(new StoredField(LATITUDE, latitude));
            document.add(new StoredField(LONGITUDE, longitude));
        }

        BigDecimal latestMarketValue = Optional.ofNullable(latestEvaluation)
                .map(PropertyEvaluation::getMarketValue)
                .orElse(null);

        if (latestMarketValue != null) {
            document.add(new DoublePoint(EVALUATION_MARKET_VALUE, latestMarketValue.doubleValue()));
            document.add(new StoredField(EVALUATION_MARKET_VALUE, latestMarketValue.doubleValue()));
        }
        return document;
    }

    public static String normalizeFilterValue(String value) {
        return nullSafe(value).trim().toLowerCase(Locale.ROOT);
    }

    public static String nullSafe(String value) {
        return value == null ? StringUtils.EMPTY : value;
    }
}
