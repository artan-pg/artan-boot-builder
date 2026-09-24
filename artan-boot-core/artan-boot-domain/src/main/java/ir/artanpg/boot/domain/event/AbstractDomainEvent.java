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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
 *   <li>{@code metadata} is stored in an unmodifiable defensive copy, so
 *       {@link #getMetadata()} returns a stable snapshot that is safe to share
 *       across threads without synchronization.</li>
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
     * Metadata key carrying the identifier of the request/message that
     * originated the command which produced this event.
     */
    public static final String CORRELATION_ID_KEY = "correlationId";

    /**
     * Metadata key carrying the {@code eventId} of the event that causally
     * produced this event.
     */
    public static final String CAUSATION_ID_KEY = "causationId";

    /**
     * Metadata key carrying the tenant this event belongs to.
     */
    public static final String TENANT_KEY = "tenant";

    /**
     * Exception message for null or blank metadata keys.
     */
    private static final String METADATA_KEY_EXCEPTION_MSG = "The metadata key cannot be null or blank";

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

    /**
     * Cross-cutting metadata (correlation id, causation id, tenant, ...).
     *
     * <p>Captured as an unmodifiable defensive copy at construction time;
     * deliberately NOT part of equals/hashCode, because metadata does not
     * change the identity of the domain fact. Being immutable, it requires no
     * synchronization and its snapshot stays identical to what was published.
     */
    private final Map<String, Object> metadata;

    /**
     * Constructs a new domain event with the default clock and ID
     * generator.
     *
     * @param aggregateId the identifier of the aggregate
     * @param payload     the event payload
     */
    protected AbstractDomainEvent(@NonNull I aggregateId, @NonNull P payload) {
        this(aggregateId, payload, null, null, null);
    }

    /**
     * Constructs a new domain event with a specific clock.
     *
     * @param aggregateId the identifier of the aggregate
     * @param payload     the event payload
     * @param clock       the clock to use for the timestamp
     */
    protected AbstractDomainEvent(@NonNull I aggregateId, @NonNull P payload, @Nullable Clock clock) {
        this(aggregateId, payload, clock, null, null);
    }

    /**
     * Constructs a new domain event with a specific clock and ID
     * generator.
     *
     * @param aggregateId the identifier of the aggregate
     * @param payload     the event payload
     * @param clock       the clock to use for the timestamp
     * @param idGenerator the generator for the event ID
     */
    protected AbstractDomainEvent(@NonNull I aggregateId,
                                  @NonNull P payload,
                                  @Nullable Clock clock,
                                  @Nullable IdGenerator idGenerator) {
        this(aggregateId, payload, clock, idGenerator, null);
    }

    /**
     * Primary constructor: builds a fully immutable event, optionally
     * seeded with cross-cutting metadata.
     *
     * @param aggregateId the identifier of the aggregate
     * @param payload     the event payload
     * @param clock       the clock to use for the timestamp
     * @param idGenerator the generator for the event ID
     * @param metadata    the cross-cutting metadata map
     * @throws DomainEventException if aggregateId or payload is {@code null}, the generated ID is {@code blank},
     *                              or a metadata key is invalid
     */
    protected AbstractDomainEvent(@NonNull I aggregateId,
                                  @NonNull P payload,
                                  @Nullable Clock clock,
                                  @Nullable IdGenerator idGenerator,
                                  @Nullable Map<String, Object> metadata) {
        if (aggregateId == null) throw new DomainEventException("The aggregateId cannot be null");
        if (payload == null) throw new DomainEventException("The payload cannot be null");

        IdGenerator generator = (idGenerator != null) ? idGenerator : IdGenerator.uuid();
        this.eventId = generator.nextId();
        if (this.eventId == null || this.eventId.isBlank()) {
            throw new DomainEventException("The generated eventId cannot be null or blank");
        }

        this.occurredAt = (clock != null) ? clock.instant() : Instant.now();
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.metadata = immutableMetadataCopy(metadata);
    }

    /**
     * Creates an unmodifiable defensive copy of the given metadata.
     *
     * @param source the source metadata map
     * @return an unmodifiable map, or an empty map if source is null
     * @throws DomainEventException if a metadata key is {@code null} or {@code blank}
     */
    private static Map<String, Object> immutableMetadataCopy(@Nullable Map<String, Object> source) {
        if (source == null || source.isEmpty()) return Collections.emptyMap();
        Map<String, Object> copy = LinkedHashMap.newLinkedHashMap(source.size());
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw new DomainEventException(METADATA_KEY_EXCEPTION_MSG);
            }
            if (entry.getValue() != null) copy.put(key, entry.getValue());
        }
        if (copy.isEmpty()) return Collections.emptyMap();
        return Collections.unmodifiableMap(new LinkedHashMap<>(copy));
    }

    @Override
    public String getEventId() {
        return this.eventId;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The wire identity of the event type is derived from the
     * concrete class name and interned through the shared registry.
     */
    @Override
    public EventType eventType() {
        // The wire identity of the event type is derived from the concrete
        // class name, but it is INTERNED through the shared registry so that
        // every instance of the same event class resolves to the very same
        // canonical EventType object (stable equals/hashCode across JVM-wide
        // lookups and safe for strict-mode validation).
        return EventTypeRegistry.shared().valueOfName(getClass().getSimpleName());
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
    public Map<String, Object> getMetadata() {
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
                Objects.equals(aggregateId, that.aggregateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), eventId, occurredAt, aggregateId, payload);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("eventId='" + eventId + "'")
                .add("occurredAt=" + occurredAt)
                .add("payload=" + redactionPolicy().redact(getPayload()))
                .add("aggregateId=" + aggregateId)
                .toString();
    }
}
