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
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a significant occurrence within the domain that is of interest
 * to the business and may trigger side effects.
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
 * @param <I> the type of the identifier of the aggregate that originated this event
 * @param <P> the type of the event payload (a plain immutable domain data carrier)
 * @author Mohammad Yazdian
 * @see EventType
 * @see EventMetadata
 * @since 0.1.0
 */
public interface DomainEvent<I extends Identifier<?>, P> {

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
     * @return the aggregate identifier
     */
    I getAggregateId();

    /**
     * Returns the payload containing the data associated with this domain
     * event.
     *
     * @return the event payload
     */
    P getPayload();

    /**
     * Returns the type-safe metadata associated with this domain event.
     *
     * <p>Metadata carries cross-cutting information such as correlation
     * ids, causation ids, tenant, tracing context, and user context.
     *
     * <p>The returned instance is immutable. Implementations must never
     * expose a mutable view of internal state.
     *
     * @return the event metadata
     */
    default EventMetadata getMetadata() {
        return EventMetadata.empty();
    }

    /**
     * Checks if this event is of the specified {@link EventType}.
     *
     * <p>Comparison uses full coordinate equality
     * ({@code name}, {@code category}, {@code version}).
     *
     * @param type the event type to compare against; may be {@code null}
     * @return {@code true} if coordinates match, {@code false} otherwise
     */
    default boolean isOfType(@Nullable EventType type) {
        if (type == null) return false;
        EventType own = getEventType();
        return own != null && Objects.equals(own.getName(), type.getName());
    }
}
