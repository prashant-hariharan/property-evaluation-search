package com.prashant.propertysearch.service;

import com.prashant.propertysearch.dto.property.PropertyCreateRequest;
import com.prashant.propertysearch.dto.property.PropertyDetailsResponse;
import com.prashant.propertysearch.dto.property.PropertyEvaluationEmbeddedResponse;
import com.prashant.propertysearch.dto.property.PropertyResponse;
import com.prashant.propertysearch.dto.property.PropertyUpdateRequest;
import com.prashant.propertysearch.entity.Property;
import com.prashant.propertysearch.event.PropertyChangedEvent;
import com.prashant.propertysearch.exception.ResourceNotFoundException;
import com.prashant.propertysearch.mapper.PropertyMapper;
import com.prashant.propertysearch.repository.PropertyEvaluationRepository;
import com.prashant.propertysearch.repository.PropertyRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service class responsible for managing property-related operations in the system.
 * This includes creating, updating, deleting, and retrieving property information.
 * It also handles publishing events for property changes and mapping entities to
 * responses for client interaction.
 *
 * @author prashant
 */
@Service
@AllArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyEvaluationRepository propertyEvaluationRepository;
    private final PropertyMapper propertyMapper;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public PropertyResponse create(PropertyCreateRequest request) {
        Property property = propertyMapper.toEntity(request);
        Property saved = propertyRepository.save(property);
        eventPublisher.publishEvent(new PropertyChangedEvent(saved.getId(), PropertyChangedEvent.Action.UPSERT));
        return propertyMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PropertyDetailsResponse getById(UUID id, boolean includeEvaluations) {
        Property property = findEntityById(id);
        List<PropertyEvaluationEmbeddedResponse> evaluations = includeEvaluations
                ? propertyMapper.toEmbeddedEvaluationResponseList(
                        propertyEvaluationRepository.findAllByPropertyIdOrderByCreatedAtDesc(id)
                )
                : List.of();
        return propertyMapper.toDetailsResponse(property, evaluations);
    }


    @Transactional
    public PropertyResponse update(UUID id, PropertyUpdateRequest request) {
        Property property = findEntityById(id);
        propertyMapper.updateEntity(request, property);
        Property saved = propertyRepository.save(property);
        eventPublisher.publishEvent(new PropertyChangedEvent(saved.getId(), PropertyChangedEvent.Action.UPSERT));
        return propertyMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Property property = findEntityById(id);
        propertyRepository.delete(property);
        eventPublisher.publishEvent(new PropertyChangedEvent(id, PropertyChangedEvent.Action.DELETE));
    }

    public Property findEntityById(UUID id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found: " + id));
    }
}
