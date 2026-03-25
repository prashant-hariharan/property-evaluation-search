package com.prashant.propertysearch.mapper;

import com.prashant.propertysearch.dto.property.PropertyCreateRequest;
import com.prashant.propertysearch.dto.property.PropertyDetailsResponse;
import com.prashant.propertysearch.dto.property.PropertyEvaluationEmbeddedResponse;
import com.prashant.propertysearch.dto.property.PropertyResponse;
import com.prashant.propertysearch.dto.property.PropertyUpdateRequest;
import com.prashant.propertysearch.entity.Property;
import com.prashant.propertysearch.entity.PropertyEvaluation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Map struct entity for Property
 *
 * @author prashant
 */
@Mapper(componentModel = "spring")
public interface PropertyMapper {

    @Mapping(target = "id", ignore = true)
    Property toEntity(PropertyCreateRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntity(PropertyUpdateRequest request, @MappingTarget Property property);

    PropertyResponse toResponse(Property property);

    @Mapping(target = "evaluations", source = "evaluations")
    PropertyDetailsResponse toDetailsResponse(
            Property property,
            List<PropertyEvaluationEmbeddedResponse> evaluations
    );

    PropertyEvaluationEmbeddedResponse toEmbeddedEvaluationResponse(PropertyEvaluation evaluation);

    List<PropertyEvaluationEmbeddedResponse> toEmbeddedEvaluationResponseList(List<PropertyEvaluation> evaluations);
}
