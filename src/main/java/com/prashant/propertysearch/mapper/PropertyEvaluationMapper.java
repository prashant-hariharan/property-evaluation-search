package com.prashant.propertysearch.mapper;

import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationCreateRequest;
import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationResponse;
import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationUpdateRequest;
import com.prashant.propertysearch.entity.PropertyEvaluation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Map struct entity for Property Evaluation
 *
 * @author prashant
 */
@Mapper(componentModel = "spring")
public interface PropertyEvaluationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "property", ignore = true)
    PropertyEvaluation toEntity(PropertyEvaluationCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "property", ignore = true)
    void updateEntity(PropertyEvaluationUpdateRequest request, @MappingTarget PropertyEvaluation evaluation);

    @Mapping(target = "propertyId", source = "property.id")
    PropertyEvaluationResponse toResponse(PropertyEvaluation evaluation);
}
