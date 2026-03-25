package com.prashant.propertysearch.service;

import com.prashant.propertysearch.dto.property.PropertyCreateRequest;
import com.prashant.propertysearch.dto.property.PropertyResponse;
import com.prashant.propertysearch.entity.Property;
import com.prashant.propertysearch.entity.PropertyType;
import com.prashant.propertysearch.event.PropertyChangedEvent;
import com.prashant.propertysearch.exception.ResourceNotFoundException;
import com.prashant.propertysearch.mapper.PropertyMapper;
import com.prashant.propertysearch.repository.PropertyEvaluationRepository;
import com.prashant.propertysearch.repository.PropertyRepository;
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
class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private PropertyEvaluationRepository propertyEvaluationRepository;
    @Mock
    private PropertyMapper propertyMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PropertyService propertyService;

    @Test
    void create_savesPropertyPublishesUpsertEventAndReturnsResponse() {
        PropertyCreateRequest request = new PropertyCreateRequest(
                "Test Property",
                "1 Test Street",
                "Berlin",
                "10115",
                BigDecimal.valueOf(80),
                2018,
                PropertyType.APARTMENT,
                "Test description"
        );
        Property entity = Property.builder().title("Test Property").build();
        UUID propertyId = UUID.randomUUID();
        Property saved = Property.builder().id(propertyId).title("Test Property").build();
        PropertyResponse response = new PropertyResponse();
        response.setId(propertyId);

        when(propertyMapper.toEntity(request)).thenReturn(entity);
        when(propertyRepository.save(entity)).thenReturn(saved);
        when(propertyMapper.toResponse(saved)).thenReturn(response);

        PropertyResponse actual = propertyService.create(request);

        assertThat(actual).isSameAs(response);
        ArgumentCaptor<PropertyChangedEvent> eventCaptor = ArgumentCaptor.forClass(PropertyChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().propertyId()).isEqualTo(propertyId);
        assertThat(eventCaptor.getValue().action()).isEqualTo(PropertyChangedEvent.Action.UPSERT);
    }

    @Test
    void findEntityById_throwsWhenPropertyMissing() {
        UUID id = UUID.randomUUID();
        when(propertyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> propertyService.findEntityById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Property not found: " + id);
    }
}
