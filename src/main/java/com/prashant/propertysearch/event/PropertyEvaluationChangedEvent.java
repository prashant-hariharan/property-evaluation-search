package com.prashant.propertysearch.event;

import java.util.UUID;

/**
 * Represents an event indicating that the evaluation related to a specific property has changed.
 *
 * This event is used to notify and capture changes specifically associated with the evaluation
 * of a property. It includes the unique identifier of the property whose evaluation has changed.
 *
 * @param propertyId The unique identifier of the property whose evaluation has changed.
 * @author prashant
 */
public record PropertyEvaluationChangedEvent(UUID propertyId) {
}
