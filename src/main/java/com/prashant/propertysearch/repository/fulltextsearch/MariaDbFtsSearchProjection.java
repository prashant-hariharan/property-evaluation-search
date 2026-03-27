package com.prashant.propertysearch.repository.fulltextsearch;

import java.math.BigDecimal;

public interface MariaDbFtsSearchProjection {
    byte[] getPropertyId();
    String getTitle();
    String getCity();
    String getPostalCode();
    String getPropertyType();
    String getDescription();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    BigDecimal getAreaInSquareMeter();
    BigDecimal getEvaluationMarketValue();
    Double getScore();
}
