package com.prashant.propertysearch.mapper;

import com.prashant.propertysearch.dto.search.SearchHitResponse;
import com.prashant.propertysearch.repository.fulltextsearch.MariaDbFtsSearchProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.nio.ByteBuffer;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface MariaDbFtsSearchMapper {

    @Mapping(target = "propertyId", expression = "java(toUuid(projection.getPropertyId()))")
    @Mapping(target = "score", expression = "java(toScore(projection.getScore()))")
    SearchHitResponse toSearchHit(MariaDbFtsSearchProjection projection);

    default UUID toUuid(byte[] value) {
        if (value == null || value.length != 16) {
            throw new IllegalStateException("Invalid binary UUID from MariaDB result");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(value);
        long mostSignificantBits = byteBuffer.getLong();
        long leastSignificantBits = byteBuffer.getLong();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    default float toScore(Double value) {
        return value == null ? 0f : value.floatValue();
    }
}
