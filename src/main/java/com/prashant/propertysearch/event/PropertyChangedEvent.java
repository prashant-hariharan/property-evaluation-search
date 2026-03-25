package com.prashant.propertysearch.event;

import java.util.UUID;

/**
 * Represents an event indicating a change in the state of a property.
 *
 * This event is used to capture changes related to a property, such as creation,
 * update, or deletion. It includes the unique identifier of the affected property
 * and the type of action performed on it.
 *
 * @param propertyId The unique identifier of the property that has changed.
 * @param action     The type of action performed on the property. Can be either
 *                   {@code UPSERT} to indicate an insert or update operation
 *                   or {@code DELETE} to indicate a deletion.
 *
 * @author prashant
 */
public record PropertyChangedEvent(UUID propertyId, Action action) {

    public enum Action {
        UPSERT,
        DELETE
    }
}
