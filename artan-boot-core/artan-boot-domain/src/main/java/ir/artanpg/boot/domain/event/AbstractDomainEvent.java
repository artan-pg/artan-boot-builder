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
 * identifier (generated through an injectable {@link IdGenerator}), the
 * timestamp of when the event occurred, the event payload, the identifier of
 * the aggregate that produced the event, mutable metadata support and a
 * pluggable {@link PayloadRedactionPolicy} for safe {@code toString()} output.
 *
 * <h2>Event type derivation:</h2>
 * {@link #getEventType()} is implemented here: by default the event type name
 * is derived from the concrete class's simple name (e.g.
 * {@code OrderShippedEvent} → {@code "OrderShippedEvent"}). Subclasses can
 * override this method to provide an explicit, stable type name — which is
 * strongly recommended for event-sourced systems where class renames must not
 * break historical events.
 *
 * <h2>Thread-safety:</h2>
 * All core attributes are immutable. The metadata map is guarded by its own
 * monitor, so metadata mutation is thread-safe while remaining excluded from
 * {@code equals}/{@code hashCode}.
 *
 * @param <I> the type of the aggregate identifier.
 * @param <T> the type of the event payload (a plain immutable data carrier;
 *            no {@code Serializable} constraint — serialization is delegated
 *            to infrastructure {@code EventSerializer} implementations).
 * @author Mohammad Yazdian
 * @version 0.1.0
 */
public abstract class AbstractDomainEvent<I extends Identifier<?>, T> implements DomainEvent<I, T> {

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
     * Cross-cutting metadata (correlation id, causation id, tenant, ...).
     * Guarded by its own monitor; deliberately NOT part of equals/hashCode,
     * because metadata does not change the identity of the domain fact.
     */
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    /**
     * Constructs a new domain event with the specified aggregate identifier and payload,
     * using the current system time and the default {@link IdGenerator#uuid()} strategy.
     *
     * @param aggregateId the identifier of the aggregate producing the event
     * @param payload     the event payload
     * @throws DomainException if aggregateId or payload is {@code null}
     */
    protected AbstractDomainEvent(@NonNull I aggregateId, @NonNull T payload) {
        this(aggregateId, payload, null, null);
    }

    /**
     * Constructs a new domain event with the specified aggregate identifier,
     * payload, and a custom {@link Clock} for determining the event occurrence
     * time, using the default {@link IdGenerator#uuid()} strategy.
     *
     * @param aggregateId the identifier of the aggregate producing the event
     * @param payload     the event payload
     * @param clock       the clock to use for determining the event occurrence time;
     *                    if {@code null}, the current system time is used
     * @throws DomainException if aggregateId or payload is {@code null}
     */
    protected AbstractDomainEvent(@NonNull I aggregateId, @NonNull T payload, @Nullable Clock clock) {
        this(aggregateId, payload, clock, null);
    }

    /**
     * Constructs a new domain event with fully injected collaborators.
     *
     * @param aggregateId the identifier of the aggregate producing the event; must not be {@code null}
     * @param payload     the event payload; must not be {@code null}
     * @param clock       the clock to use for determining the event occurrence time;
     *                    if {@code null}, the current system time is used
     * @param idGenerator the strategy used to generate {@code eventId};
     *                    if {@code null}, {@link IdGenerator#uuid()} is used
     * @throws DomainException      if aggregateId or payload is {@code null}
     * @throws DomainException      if the injected generator produces a {@code null}/blank ID
     * @throws NullPointerException if the injected generator produces a {@code null} ID
     */
    protected AbstractDomainEvent(@NonNull I aggregateId,
                                  @NonNull T payload,
                                  @Nullable Clock clock,
                                  @Nullable IdGenerator idGenerator) {
        if (aggregateId == null) throw new DomainException("The aggregateId cannot be null");
        if (payload == null) throw new DomainException("The payload cannot be null");

        IdGenerator generator = (idGenerator != null) ? idGenerator : IdGenerator.uuid();
        this.eventId = Objects.requireNonNull(generator.nextId(), "The generated eventId cannot be null");
        if (this.eventId.isBlank()) throw new DomainException("The generated eventId cannot be blank");

        this.occurredAt = (clock != null) ? clock.millis() : System.currentTimeMillis();
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    @Override
    public String getEventId() {
        return this.eventId;
    }

    /**
     * Returns the type of this event, derived from the simple name of the
     * concrete event class.
     *
     * <p>Subclasses may override this method to decouple the wire-level event
     * type name from the Java class name (recommended for event sourcing).
     *
     * @return the cached {@link EventType} for this event class
     */
    @Override
    public EventType getEventType() {
        return EventType.valueOf(getClass().getSimpleName());
    }

    @Override
    public Instant getOccurredAt() {
        return Instant.ofEpochMilli(this.occurredAt);
    }

    @Override
    public I getAggregateId() {
        return aggregateId;
    }

    @Override
    public T getPayload() {
        return this.payload;
    }

    // ------------------------------------------------------------------
    // Metadata support
    // ------------------------------------------------------------------

    /**
     * Returns an unmodifiable snapshot of the metadata currently attached to
     * this event.
     *
     * @return an unmodifiable copy of the metadata map; never {@code null}
     */
    @Override
    public Map<String, Object> getMetadata() {
        synchronized (metadata) {
            if (metadata.isEmpty()) return Collections.emptyMap();
            return Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
        }
    }

    /**
     * Associates the given value with the given key in this event's metadata.
     *
     * <p>A {@code null} value removes the entry (same as
     * {@link #removeMetadata(String)}), keeping the map free of null values.
     *
     * @param key   the metadata key; must not be {@code null} or blank
     * @param value the metadata value; {@code null} means "remove the key"
     * @return the previous value associated with the key, or {@code null}
     * @throws DomainException if key is {@code null} or blank
     */
    public @Nullable Object setMetadataValue(@NonNull String key, @Nullable Object value) {
        requireValidKey(key);
        synchronized (metadata) {
            if (value == null) return metadata.remove(key);
            return metadata.put(key, value);
        }
    }

    /**
     * Copies all entries of the given map into this event's metadata.
     *
     * <p>Existing keys are overwritten. A {@code null} argument is ignored.
     *
     * @param values the metadata entries to add; may be {@code null}
     */
    public void putAllMetadata(@Nullable Map<String, Object> values) {
        if (values == null || values.isEmpty()) return;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            setMetadataValue(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes the metadata entry with the given key, if present.
     *
     * @param key the metadata key to remove; may be {@code null} (no-op)
     * @return the value that was removed, or {@code null} if the key was absent
     */
    public @Nullable Object removeMetadata(@Nullable String key) {
        if (key == null) return null;
        synchronized (metadata) {
            return metadata.remove(key);
        }
    }

    /**
     * Removes all metadata entries from this event.
     */
    public void clearMetadata() {
        synchronized (metadata) {
            metadata.clear();
        }
    }

    private static void requireValidKey(String key) {
        if (key == null || key.isBlank()) {
            throw new DomainException("The metadata key cannot be null or blank");
        }
    }

    // ------------------------------------------------------------------
    // Redaction support
    // ------------------------------------------------------------------

    /**
     * Returns the policy used to render the payload inside
     * {@link #toString()}.
     *
     * <p>The default implementation returns {@link PayloadRedactionPolicy#NONE}
     * (full payload rendering). Subclasses carrying sensitive data should
     * override this method — typically simply returning
     * {@link PayloadRedactionPolicy#MASKED} or a custom lambda.
     *
     * @return the redaction policy applied to the payload in {@code toString()};
     *         never {@code null}
     */
    protected PayloadRedactionPolicy redactionPolicy() {
        return PayloadRedactionPolicy.NONE;
    }

    /**
     * Indicates whether some other object is "equal to" this event.
     *
     * <p>Two events are considered equal when all of their core attributes
     * match: {@code eventId}, {@code occurredAt}, {@code aggregateId} and
     * {@code payload}. The comparison is performed through the public getters,
     * so subclasses that override those getters participate in equality.
     * Metadata is intentionally excluded: it is transport/enrichment context,
     * not part of the domain fact.
     *
     * <p>Note: for deduplication purposes (event stores, idempotent consumers)
     * the identity of an event is its {@code eventId} alone; use
     * {@link #isSameEventAs(DomainEvent)} instead of {@code equals} for that check.
     *
     * @param o the reference object to compare with
     * @return {@code true} if this object is equal to {@code o}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractDomainEvent<?, ?> that)) return false;

        return Objects.equals(getEventId(), that.getEventId())
                && Objects.equals(getOccurredAt(), that.getOccurredAt())
                && Objects.equals(getAggregateId(), that.getAggregateId())
                && Objects.equals(getPayload(), that.getPayload());
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code based on all core event attributes
     */
    @Override
    public int hashCode() {
        return Objects.hash(getEventId(), getOccurredAt(), getAggregateId(), getPayload());
    }

    /**
     * Checks whether the given event represents the same occurrence as this one,
     * i.e. whether their {@code eventId}s are equal.
     *
     * <p>This is the intended method for deduplication in event stores and
     * idempotent handlers, because two events published for the same domain
     * fact share the same identifier even if their payloads or timestamps
     * differ.
     *
     * @param other the other domain event to compare with; may be {@code null}
     * @return {@code true} if {@code other} is non-null and has the same
     *         {@code eventId} as this event, {@code false} otherwise
     */
    public final boolean isSameEventAs(@Nullable DomainEvent<?, ?> other) {
        if (other == null) return false;
        return Objects.equals(this.eventId, other.getEventId());
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("eventId='" + getEventId() + "'")
                .add("occurredAt=" + getOccurredAt())
                .add("aggregateId=" + getAggregateId().value())
                .add("payload=" + redactionPolicy().redact(getPayload()))
                .toString();
    }
}
