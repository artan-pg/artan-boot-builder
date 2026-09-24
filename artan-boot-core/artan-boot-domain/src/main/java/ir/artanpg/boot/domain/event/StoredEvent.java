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

import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.domain.model.ValueObject;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A domain event as persisted in an event store, enriched with stream metadata.
 *
 * <p>This is the foundation type for Event Sourcing (phase 3).
 *
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @since 0.1.0
 */
public final class StoredEvent implements ValueObject, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String eventId;
    private final Identifier<?> aggregateId;
    private final long streamVersion;
    private final EventType eventType;
    private final Instant occurredAt;
    private final Instant storedAt;
    private final DomainEvent event;

    /**
     * Creates a new stored event wrapper.
     *
     * @param event         the original domain event
     * @param streamVersion the version of this event within the aggregate stream (1-based recommended)
     * @param storedAt      when the event was persisted
     */
    public StoredEvent(@NonNull DomainEvent event, long streamVersion, @NonNull Instant storedAt) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(storedAt, "storedAt must not be null");
        if (streamVersion < 0) {
            throw new DomainException("streamVersion cannot be negative");
        }
        this.event = event;
        this.eventId = event.getEventId();
        this.aggregateId = event.getAggregateId();
        this.streamVersion = streamVersion;
        this.eventType = event.getEventType();
        this.occurredAt = event.getOccurredAt();
        this.storedAt = storedAt;
    }

    public String getEventId() {
        return this.eventId;
    }

    public Identifier<?> getAggregateId() {
        return this.aggregateId;
    }

    public long getStreamVersion() {
        return this.streamVersion;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public Instant getOccurredAt() {
        return this.occurredAt;
    }

    public Instant getStoredAt() {
        return this.storedAt;
    }

    public DomainEvent getEvent() {
        return this.event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StoredEvent that)) {
            return false;
        }
        return this.eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return this.eventId.hashCode();
    }

    @Override
    public String toString() {
        return "StoredEvent[eventId=" + this.eventId
                + ", aggregateId=" + this.aggregateId.value()
                + ", streamVersion=" + this.streamVersion
                + ", type=" + this.eventType.getName() + "]";
    }
}
