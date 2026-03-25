package com.prashant.propertysearch.service;

import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationCreateRequest;
import com.prashant.propertysearch.dto.evaluation.PropertyEvaluationResponse;
import com.prashant.propertysearch.entity.Property;
import com.prashant.propertysearch.entity.PropertyEvaluation;
import com.prashant.propertysearch.event.PropertyEvaluationChangedEvent;
import com.prashant.propertysearch.exception.ResourceNotFoundException;
import com.prashant.propertysearch.mapper.PropertyEvaluationMapper;
import com.prashant.propertysearch.repository.PropertyEvaluationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyEvaluationServiceTest {

    @Mock
    private PropertyEvaluationRepository propertyEvaluationRepository;
    @Mock
    private PropertyService propertyService;
    @Mock
    private PropertyEvaluationMapper propertyEvaluationMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PropertyEvaluationService propertyEvaluationService;

    @Test
    void create_savesEvaluationPublishesEventAndReturnsResponse() {
        UUID propertyId = UUID.randomUUID();
        PropertyEvaluationCreateRequest request = new PropertyEvaluationCreateRequest(
                propertyId,
                BigDecimal.valueOf(450000),
                "Test notes"
        );
        Property property = Property.builder().id(propertyId).build();
        PropertyEvaluation entity = PropertyEvaluation.builder().marketValue(BigDecimal.valueOf(450000)).build();
        PropertyEvaluation saved = PropertyEvaluation.builder().id(UUID.randomUUID()).property(property).build();
        PropertyEvaluationResponse response = new PropertyEvaluationResponse();
        response.setId(saved.getId());

        when(propertyService.findEntityById(propertyId)).thenReturn(property);
        when(propertyEvaluationMapper.toEntity(request)).thenReturn(entity);
        when(propertyEvaluationRepository.save(entity)).thenReturn(saved);
        when(propertyEvaluationMapper.toResponse(saved)).thenReturn(response);

        PropertyEvaluationResponse actual = propertyEvaluationService.create(request);

        assertThat(actual).isSameAs(response);
        ArgumentCaptor<PropertyEvaluationChangedEvent> eventCaptor = ArgumentCaptor.forClass(PropertyEvaluationChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().propertyId()).isEqualTo(propertyId);
    }

    @Test
    void findEntityById_throwsWhenEvaluationMissing() {
        UUID id = UUID.randomUUID();
        when(propertyEvaluationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyEvaluationService.findEntityById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Property evaluation not found: " + id);
    }
}
