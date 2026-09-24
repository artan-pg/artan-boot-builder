/*
 * Copyright (c) 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ir.artanpg.boot.domain.event;

import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.domain.model.ValueObject;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a significant occurrence within the domain that is of interest
 * to the business and may trigger side effects.
 *
 * <p>Implementations must be immutable and must override
 * {@link Object#equals(Object)} and {@link Object#hashCode()} based on the
 * {@link #getEventId()} to ensure correct behavior in event stores and
 * deduplication mechanisms.
 *
 * <p>Domain events are a fundamental building block of DDD that enable:
 * <ul>
 *   <li>Decoupling between different parts of the system</li>
 *   <li>Event-driven architectures</li>
 *   <li>Event sourcing and CQRS</li>
 *   <li>Audit trails and logging</li>
 *   <li>Cross-cutting concerns like notifications and integrations</li>
 * </ul>
 *
 * <h2>Characteristics:</h2>
 * <ul>
 *   <li>Immutable - represents a fact that occurred in the past</li>
 *   <li>Timestamped - includes the exact time of occurrence</li>
 *   <li>Aggregate-bound - references the aggregate that generated it</li>
 *   <li>Type-safe - uses {@link EventType} for categorization</li>
 * </ul>
 *
 * <h2>Event Naming Convention:</h2>
 * Domain events should be named using the past tense of a verb that
 * describes the occurrence.
 *
 * @author Mohammad Yazdian
 * @see EventType
 * @since 0.1.0
 */
public interface DomainEvent extends ValueObject {

    /**
     * Returns the unique identifier of this domain event instance.
     *
     * @return the unique event identifier
     */
    String getEventId();

    /**
     * Returns the type of this domain event.
     *
     * <p>The event type is a string that uniquely identifies the kind of
     * event. It can be used for event routing, filtering, and processing.
     *
     * <p>Event types should be:
     * <ul>
     *   <li>Unique across the domain</li>
     *   <li>Descriptive of the occurrence</li>
     *   <li>Stable over time for event sourcing</li>
     * </ul>
     *
     * @return the event type
     */
    EventType getEventType();

    /**
     * Returns the exact timestamp when this event occurred.
     *
     * <p>The timestamp represents the time when the event was generated
     * by the domain, not when it was persisted or processed. This is
     * crucial for maintaining the correct order of events.
     *
     * @return the occurrence timestamp
     */
    Instant getOccurredAt();

    /**
     * Returns the unique identifier of the aggregate root that originated this
     * domain event.
     *
     * @param <I> the type of the identifier
     * @return the aggregate identifier
     */
    <I extends Identifier<?>> I getAggregateId();

    /**
     * Returns the payload containing the data associated with this domain
     * event.
     *
     * <p>The payload must be {@link Serializable} to allow the event to be
     * persisted to an event store or transmitted over a message broker.
     *
     * @param <T> the type of the payload
     * @return the event payload
     */
    <T extends Serializable> T getPayload();

    /**
     * Returns the metadata associated with this domain event.
     *
     * <p>Metadata can include cross-cutting information such as correlation
     * IDs, causation IDs, user context, or tracing information.
     *
     * @return a map of metadata key-value pairs.
     */
    default Map<String, Object> getMetaData() {
        return Collections.emptyMap();
    }

    /**
     * Checks if this event is of the specified {@link EventType}.
     *
     * @param type the event type to compare against
     * @return {@code true} if this event's type matches the specified type, {@code false} otherwise
     */
    default boolean isOfType(EventType type) {
        return Objects.equals(getEventType(), type);
    }
}
