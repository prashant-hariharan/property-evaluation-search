package com.prashant.propertysearch.utils;

import java.util.List;

public final class LuceneDocumentFields {

    private LuceneDocumentFields() {
    }

    public static final String PROPERTY_ID = "propertyId";
    public static final String TITLE = "title";
    public static final String CITY = "city";
    public static final String POSTAL_CODE = "postalCode";
    public static final String PROPERTY_TYPE = "propertyType";
    public static final String DESCRIPTION = "description";
    public static final String AREA_IN_SQUARE_METER = "areaInSquareMeter";
    public static final String EVALUATION_MARKET_VALUE = "evaluationMarketValue";
    public static final String CITY_FILTER = "cityFilter";
    public static final String POSTAL_CODE_FILTER = "postalCodeFilter";

    public static final List<String> SEARCH_FIELDS = List.of(
            TITLE,
            DESCRIPTION
    );
}
