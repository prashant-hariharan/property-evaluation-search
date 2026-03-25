package com.prashant.propertysearch.service;

import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationCreateRequest;
import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationResponse;
import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationUpdateRequest;
import com.prashant.propertysearch.entity.Property;
import com.prashant.propertysearch.entity.PropertyEvaluation;
import com.prashant.propertysearch.event.PropertyEvaluationChangedEvent;
import com.prashant.propertysearch.exception.ResourceNotFoundException;
import com.prashant.propertysearch.mapper.PropertyEvaluationMapper;
import com.prashant.propertysearch.repository.PropertyEvaluationRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service class responsible for managing property evaluations.
 * This class provides methods to create, retrieve, update, and delete property evaluations.
 * It also interacts with the PropertyService for fetching property data and publishes events
 * when property evaluation changes are made.
 *
 * @author prashant
 */
@Service
@AllArgsConstructor
public class PropertyEvaluationService {

    private final PropertyEvaluationRepository propertyEvaluationRepository;
    private final PropertyService propertyService;
    private final PropertyEvaluationMapper propertyEvaluationMapper;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public PropertyEvaluationResponse create(PropertyEvaluationCreateRequest request) {
        Property property = propertyService.findEntityById(request.propertyId());
        PropertyEvaluation evaluation = propertyEvaluationMapper.toEntity(request);
        evaluation.setProperty(property);
        PropertyEvaluation saved = propertyEvaluationRepository.save(evaluation);
        eventPublisher.publishEvent(new PropertyEvaluationChangedEvent(property.getId()));
        return propertyEvaluationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PropertyEvaluationResponse getById(UUID id) {
        return propertyEvaluationMapper.toResponse(findEntityById(id));
    }


    @Transactional
    public PropertyEvaluationResponse update(UUID id, PropertyEvaluationUpdateRequest request) {
        PropertyEvaluation evaluation = findEntityById(id);
        Property property = propertyService.findEntityById(request.propertyId());
        propertyEvaluationMapper.updateEntity(request, evaluation);
        evaluation.setProperty(property);
        PropertyEvaluation saved = propertyEvaluationRepository.save(evaluation);
        eventPublisher.publishEvent(new PropertyEvaluationChangedEvent(property.getId()));
        return propertyEvaluationMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        PropertyEvaluation evaluation = findEntityById(id);
        UUID propertyId = evaluation.getProperty().getId();
        propertyEvaluationRepository.delete(evaluation);
        eventPublisher.publishEvent(new PropertyEvaluationChangedEvent(propertyId));
    }

    public PropertyEvaluation findEntityById(UUID id) {
        return propertyEvaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property evaluation not found: " + id));
    }
}
