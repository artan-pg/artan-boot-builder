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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Abstract base class for domain events.
 *
 * <p>Provides common functionality for domain events, including a unique event
 * identifier, the timestamp of when the event occurred, the event payload, and
 * the identifier of the aggregate that produced the event.
 *
 * @param <I> the type of the aggregate identifier.
 * @param <T> the type of the event payload, which must be {@link Serializable}.
 * @author Mohammad Yazdian
 * @version 0.1.0
 */
public abstract class AbstractDomainEvent<I extends Identifier<?>, T extends Serializable> implements DomainEvent {

    @Serial
    private static final long serialVersionUID = -3897488932713723995L;

    /**
     * The unique identifier for this event.
     */
    private final String eventId;

    /**
     * The timestamp (in milliseconds since epoch) when this event occurred.
     */
    private final Long occurredAt;

    /**
     * The payload of the event, containing the data associated with the event.
     */
    private final T payload;

    /**
     * The identifier of the aggregate.
     */
    private final I aggregateId;

    /**
     * Constructs a new domain event with the specified aggregate identifier and payload,
     * using the current system time.
     *
     * @param aggregateId the identifier of the aggregate producing the event
     * @param payload     the event payload
     * @throws DomainException if aggregateId or payload is {@code null}
     */
    protected AbstractDomainEvent(@NonNull I aggregateId, @NonNull T payload) {
        this(aggregateId, payload, null);
    }

    /**
     * Constructs a new domain event with the specified aggregate identifier,
     * payload, and a custom {@link Clock} for determining the event occurrence
     * time.
     *
     * @param aggregateId the identifier of the aggregate producing the event
     * @param payload     the event payload
     * @param clock       the clock to use for determining the event occurrence time;
     *                    if {@code null}, the current system time is used
     * @throws DomainException if aggregateId or payload is {@code null}
     */
    protected AbstractDomainEvent(@NonNull I aggregateId, @NonNull T payload, @Nullable Clock clock) {
        if (aggregateId == null) throw new DomainException("The aggregateId cannot be null");
        if (payload == null) throw new DomainException("The source cannot be null");

        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = (clock != null) ? clock.millis() : System.currentTimeMillis();
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    @Override
    public String getEventId() {
        return this.eventId;
    }

    @Override
    public Instant getOccurredAt() {
        return Instant.ofEpochMilli(this.occurredAt);
    }

    @SuppressWarnings("unchecked")
    @Override
    public I getAggregateId() {
        return aggregateId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getPayload() {
        return this.payload;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractDomainEvent<?, ?> that)) return false;

        return getEventId().equals(that.getEventId());
    }

    @Override
    public int hashCode() {
        return getEventId().hashCode();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("eventId='" + getEventId() + "'")
                .add("occurredAt=" + getOccurredAt())
                .add("aggregateId=" + getAggregateId().value())
                .add("payload=" + getPayload())
                .toString();
    }
}
