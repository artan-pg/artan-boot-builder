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

import ir.artanpg.boot.domain.exception.DomainEventException;
import ir.artanpg.boot.domain.model.IdGenerator;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Abstract base class for domain events.
 *
 * <p>Provides common functionality for domain events, including a unique event
 * identifier, the timestamp of when the event occurred, the event payload, and
 * the identifier of the aggregate that produced the event.
 *
 * <h2>Immutability contract:</h2>
 *
 * <p>A domain event represents a fact that already happened, therefore all
 * core state — {@code eventId}, {@code occurredAt}, {@code aggregateId},
 * {@code payload} and {@code metadata} — is captured once at construction time
 * and never changes afterward:
 * <ul>
 *   <li>Every field is {@code final}; there are no setters.</li>
 *   <li>{@code metadata} is an immutable {@link EventMetadata} snapshot, so
 *       {@link #getMetadata()} is safe to share across threads without
 *       synchronization.</li>
 *   <li>The only mutation surface left is subclass payload state; payloads are
 *       expected to be plain immutable data carriers (records preferred).</li>
 * </ul>
 * Because the state can no longer drift after construction, {@code equals} and
 * {@code hashCode} remain consistent for the whole lifetime of the instance,
 * which makes events safe as keys/elements of hash-based collections and
 * reliable for event-sourcing deduplication.
 *
 * @param <I> the type of the aggregate identifier
 * @param <P> the type of the event payload
 * @author Mohammad Yazdian
 * @version 0.1.0
 */
public abstract class AbstractDomainEvent<I extends Identifier<?>, P> implements DomainEvent<I, P> {

    /**
     * The unique identifier for this event.
     */
    private final String eventId;

    /**
     * The timestamp when this event occurred.
     */
    private final Instant occurredAt;

    /**
     * The payload of the event, containing the data associated with the event.
     */
    private final P payload;

    /**
     * The identifier of the aggregate.
     */
    private final I aggregateId;

    private final EventType eventType;

    /**
     * Cross-cutting metadata (correlation id, causation id, tenant, ...).
     *
     * <p>Captured as an immutable {@link EventMetadata} at construction
     * time; deliberately NOT part of equals/hashCode, because metadata
     * does not change the identity of the domain fact.
     */
    private final EventMetadata metadata;

    /**
     * Constructs a new domain event with the default clock and ID
     * generator.
     *
     * @param aggregateId the identifier of the aggregate
     * @param payload     the event payload
     * @param eventType   the event type
     */
    protected AbstractDomainEvent(@NonNull I aggregateId, @NonNull P payload, @NonNull EventType eventType) {
        this(aggregateId, payload, eventType, null, null, null);
    }

    /**
     * Constructs a new domain event with a specific clock.
     *
     * @param aggregateId the identifier of the aggregate
     * @param payload     the event payload
     * @param eventType   the event type
     * @param clock       the clock to use for the timestamp
     */
    protected AbstractDomainEvent(@NonNull I aggregateId,
                                  @NonNull P payload,
                                  @NonNull EventType eventType,
                                  @Nullable Clock clock) {
        this(aggregateId, payload, eventType, clock, null, null);
    }

    /**
     * Constructs a new domain event with a specific clock and ID
     * generator.
     *
     * @param aggregateId the identifier of the aggregate
     * @param payload     the event payload
     * @param eventType   the event type
     * @param clock       the clock to use for the timestamp
     * @param idGenerator the generator for the event ID
     */
    protected AbstractDomainEvent(@NonNull I aggregateId,
                                  @NonNull P payload,
                                  @NonNull EventType eventType,
                                  @Nullable Clock clock,
                                  @Nullable IdGenerator idGenerator) {
        this(aggregateId, payload, eventType, clock, idGenerator, null);
    }

    /**
     * Primary constructor: builds a fully immutable event, optionally
     * seeded with cross-cutting metadata.
     *
     * @param aggregateId the identifier of the aggregate
     * @param payload     the event payload
     * @param eventType   the event type
     * @param clock       the clock to use for the timestamp
     * @param idGenerator the generator for the event ID
     * @param metadata    the cross-cutting metadata; {@code null} is treated as {@link EventMetadata#empty()}
     * @throws DomainEventException if aggregateId or payload is {@code null}, or the generated ID is {@code blank}
     */
    protected AbstractDomainEvent(@NonNull I aggregateId,
                                  @NonNull P payload,
                                  @NonNull EventType eventType,
                                  @Nullable Clock clock,
                                  @Nullable IdGenerator idGenerator,
                                  @Nullable EventMetadata metadata) {
        if (aggregateId == null) throw new DomainEventException("The aggregateId cannot be null");
        if (payload == null) throw new DomainEventException("The payload cannot be null");
        if (eventType == null) throw new DomainEventException("The eventType cannot be null");

        IdGenerator generator = (idGenerator != null) ? idGenerator : IdGenerator.uuid();
        this.eventId = generator.nextId();
        if (this.eventId == null || this.eventId.isBlank()) {
            throw new DomainEventException("The generated eventId cannot be null or blank");
        }

        this.occurredAt = (clock != null) ? clock.instant() : Instant.now();
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.eventType = eventType;
        this.metadata = (metadata != null) ? metadata : EventMetadata.empty();
    }

    @Override
    public String getEventId() {
        return this.eventId;
    }

    @Override
    public EventType getEventType() {
        return this.eventType;
    }

    @Override
    public Instant getOccurredAt() {
        return this.occurredAt;
    }

    @Override
    public I getAggregateId() {
        return this.aggregateId;
    }

    @Override
    public P getPayload() {
        return this.payload;
    }

    @Override
    public EventMetadata getMetadata() {
        return this.metadata;
    }

    /**
     * Returns the policy used to render the payload inside
     * {@link #toString()}.
     *
     * <p>The implementation returns {@link PayloadRedactionPolicy#NONE}.
     * Subclasses carrying sensitive data should override this method.
     *
     * @return the redaction policy applied to the payload
     */
    protected PayloadRedactionPolicy redactionPolicy() {
        return PayloadRedactionPolicy.NONE;
    }

    /**
     * Checks whether the given event represents the same occurrence as this one,
     * i.e. whether their {@code eventId}s are equal.
     *
     * <p><b>Note:</b> {@code eventId} alone does NOT include the aggregate
     * scope. For per-stream deduplication prefer {@link #isSameOccurrenceAs(DomainEvent)}.
     *
     * @param other the other domain event to compare with
     * @return {@code true} if other is non-null and has the same eventId as this event, {@code false} otherwise
     * @see #isSameOccurrenceAs(DomainEvent)
     */
    public final boolean isSameEventAs(@Nullable DomainEvent<?, ?> other) {
        if (other == null) return false;
        return Objects.equals(this.eventId, other.getEventId());
    }

    /**
     * Checks whether the given event denotes the same domain fact within the
     * same aggregate stream: identical {@code eventId} AND {@code aggregateId}.
     *
     * @param other the other domain event to compare with
     * @return {@code true} if both ID and aggregate ID match, {@code false} otherwise
     */
    public final boolean isSameOccurrenceAs(@Nullable DomainEvent<?, ?> other) {
        if (other == null) return false;
        return Objects.equals(this.eventId, other.getEventId())
                && Objects.equals(this.aggregateId, other.getAggregateId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof AbstractDomainEvent<?, ?> that)) return false;
        if (!getClass().equals(that.getClass())) return false;

        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(occurredAt, that.occurredAt) &&
                Objects.equals(payload, that.payload) &&
                Objects.equals(aggregateId, that.aggregateId) &&
                Objects.equals(eventType, that.eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), eventId, occurredAt, aggregateId, payload, eventType);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("eventId='" + eventId + "'")
                .add("occurredAt=" + occurredAt)
                .add("eventType='" + eventType + "'")
                .add("payload=" + redactionPolicy().redact(getPayload()))
                .add("aggregateId=" + aggregateId)
                .toString();
    }
}
