package com.prashant.propertysearch.mapper;

import com.prashant.propertysearch.dto.search.SearchHitResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface LuceneSearchMapper {

    @Mapping(target = "propertyId", expression = "java(toUuid(propertyId))")
    @Mapping(target = "areaInSquareMeter", expression = "java(toBigDecimal(areaInSquareMeter))")
    @Mapping(target = "evaluationMarketValue", expression = "java(toBigDecimal(evaluationMarketValue))")
    SearchHitResponse toSearchHit(
            String propertyId,
            String title,
            String city,
            String postalCode,
            String propertyType,
            String description,
            Double areaInSquareMeter,
            Double evaluationMarketValue,
            float score
    );

    default UUID toUuid(String value) {
        return UUID.fromString(value);
    }

    default BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
