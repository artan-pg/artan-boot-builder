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
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Type-safe, immutable carrier for cross-cutting domain event metadata.
 *
 * <p>Instances are immutable. The properties map is a defensive,
 * unmodifiable snapshot taken at construction time.
 *
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @see AbstractDomainEvent
 * @since 0.1.0
 */
public final class EventMetadata {

    /**
     * Shared empty instance returned by {@link #empty()}.
     */
    private static final EventMetadata EMPTY = new EventMetadata(new Builder());

    /**
     * Identifier of the request or message that originated the command
     * which produced the event.
     */
    @Nullable
    private final String correlationId;

    /**
     * Event identifier of the event that causally produced this event.
     */
    @Nullable
    private final String causationId;

    /**
     * Tenant this event belongs to.
     */
    @Nullable
    private final String tenant;

    /**
     * Distributed-tracing trace identifier.
     */
    @Nullable
    private final String traceId;

    /**
     * Distributed-tracing span identifier.
     */
    @Nullable
    private final String spanId;

    /**
     * Identifier of the user associated with the originating action.
     */
    @Nullable
    private final String userId;

    /**
     * Additional arbitrary metadata entries.
     *
     * <p>Never {@code null}; always an unmodifiable map.
     */
    private final Map<String, Object> properties;

    /**
     * Constructs a new {@code EventMetadata} by copying values from the given
     * builder.
     *
     * <p>The builder's property map is snapshot into an unmodifiable map, so
     * later mutations of the builder do not affect this instance.
     *
     * @param builder the builder supplying the field values
     */
    private EventMetadata(Builder builder) {
        this.correlationId = builder.correlationId;
        this.causationId = builder.causationId;
        this.tenant = builder.tenant;
        this.traceId = builder.traceId;
        this.spanId = builder.spanId;
        this.userId = builder.userId;
        this.properties = snapshotProperties(builder.properties);
    }

    /**
     * Returns the shared empty metadata instance.
     *
     * @return an empty {@code EventMetadata}
     */
    public static EventMetadata empty() {
        return EMPTY;
    }

    /**
     * Creates a new builder for constructing {@link EventMetadata}.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the correlation identifier, or {@code null} when absent.
     *
     * @return the correlation id
     */
    @Nullable
    public String getCorrelationId() {
        return correlationId;
    }

    /**
     * Returns the causation identifier, or {@code null} when absent.
     *
     * @return the causation id
     */
    @Nullable
    public String getCausationId() {
        return causationId;
    }

    /**
     * Returns the tenant identifier, or {@code null} when absent.
     *
     * @return the tenant
     */
    @Nullable
    public String getTenant() {
        return tenant;
    }

    /**
     * Returns the trace identifier, or {@code null} when absent.
     *
     * @return the trace id
     */
    @Nullable
    public String getTraceId() {
        return traceId;
    }

    /**
     * Returns the span identifier, or {@code null} when absent.
     *
     * @return the span id
     */
    @Nullable
    public String getSpanId() {
        return spanId;
    }

    /**
     * Returns the user identifier, or {@code null} when absent.
     *
     * @return the user id
     */
    @Nullable
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the unmodifiable map of extra properties.
     *
     * @return the properties map
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Checks whether this metadata carries no standard fields and
     * no extra properties.
     *
     * <p>Delegates to {@link #hasNoStandardFields()} for the standard-field
     * check and combines it with an emptiness test on the properties map.
     *
     * @return {@code true} if every field is {@code null} and the properties
     *         map is {@code empty}, {@code false} otherwise
     */
    public boolean isEmpty() {
        return hasNoStandardFields() && properties.isEmpty();
    }

    /**
     * Checks whether none of the standard metadata fields are set.
     *
     * <p>Combines the tracing-field and user-field checks.
     *
     * @return {@code true} if no standard field is set, {@code false} otherwise
     */
    private boolean hasNoStandardFields() {
        return hasNoTracingFields() && hasNoUserFields();
    }

    /**
     * Checks whether none of the tracing-related fields are set.
     *
     * @return {@code true} if neither trace nor correlation fields are set, {@code false} otherwise
     */
    private boolean hasNoTracingFields() {
        return hasNoCorrelationOrCausation() && hasNoTraceOrSpan();
    }

    /**
     * Checks whether neither correlation nor causation ids are set.
     *
     * @return {@code true} if both are unset, {@code false} otherwise
     */
    private boolean hasNoCorrelationOrCausation() {
        return !hasCorrelationId() && !hasCausationId();
    }

    /**
     * Checks whether neither trace nor span ids are set.
     *
     * @return {@code true} if both are unset, {@code false} otherwise
     */
    private boolean hasNoTraceOrSpan() {
        return !hasTraceId() && !hasSpanId();
    }

    /**
     * Checks whether neither tenant nor user id are set.
     *
     * @return {@code true} if both are unset, {@code false} otherwise
     */
    private boolean hasNoUserFields() {
        return !hasTenant() && !hasUserId();
    }

    /**
     * Checks whether a correlation id is present.
     *
     * @return {@code true} if set, {@code false} otherwise
     */
    public boolean hasCorrelationId() {
        return correlationId != null;
    }

    /**
     * Checks whether a causation id is present.
     *
     * @return {@code true} if set, {@code false} otherwise
     */
    public boolean hasCausationId() {
        return causationId != null;
    }

    /**
     * Checks whether a tenant is present.
     *
     * @return {@code true} if set, {@code false} otherwise
     */
    public boolean hasTenant() {
        return tenant != null;
    }

    /**
     * Checks whether a trace id is present.
     *
     * @return {@code true} if set, {@code false} otherwise
     */
    public boolean hasTraceId() {
        return traceId != null;
    }

    /**
     * Checks whether a span id is present.
     *
     * @return {@code true} if set, {@code false} otherwise
     */
    public boolean hasSpanId() {
        return spanId != null;
    }

    /**
     * Checks whether a user id is present.
     *
     * @return {@code true} if set, {@code false} otherwise
     */
    public boolean hasUserId() {
        return userId != null;
    }

    /**
     * Returns the extra property stored under the given key, or
     * {@code null} if absent or when {@code key} is {@code null}.
     *
     * @param key the property key
     * @return the property value, or {@code null} when absent
     */
    @Nullable
    public Object getProperty(@Nullable String key) {
        if (key == null) return null;
        return properties.get(key);
    }

    /**
     * Checks whether an extra property exists for the given key.
     *
     * @param key the property key; may be {@code null}
     * @return {@code true} if present, {@code false} otherwise
     */
    public boolean hasProperty(@Nullable String key) {
        if (key == null) return false;
        return properties.containsKey(key);
    }

    /**
     * Returns a new builder pre-populated with this instance's values.
     *
     * @return a builder seeded from this metadata
     */
    public Builder toBuilder() {
        Builder builder = new Builder()
                .correlationId(correlationId)
                .causationId(causationId)
                .tenant(tenant)
                .traceId(traceId)
                .spanId(spanId)
                .userId(userId);
        properties.forEach(builder::properties);
        return builder;
    }

    /**
     * Creates an unmodifiable snapshot of the given properties map.
     *
     * <p>When the source map is empty, {@link Collections#emptyMap()}
     * is returned to avoid allocation. Otherwise, a new
     * {@link LinkedHashMap} preserving iteration order is wrapped
     * with {@link Collections#unmodifiableMap(Map)}.
     *
     * @param source the source map
     * @return an unmodifiable snapshot
     */
    private static Map<String, Object> snapshotProperties(Map<String, Object> source) {
        if (source.isEmpty()) return Collections.emptyMap();
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    /**
     * Safely converts an object to a string for diagnostic output.
     *
     * <p>Falls back to a class-name and identity-hash representation
     * when the object's {@link Object#toString()} throws an
     * exception.
     *
     * @param obj the object to stringify
     * @return a string representation
     */
    private static String safeString(Object obj) {
        try {
            return String.valueOf(obj);
        } catch (Exception _) {
            return obj.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(obj));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventMetadata that)) return false;

        return Objects.equals(correlationId, that.correlationId)
                && Objects.equals(causationId, that.causationId)
                && Objects.equals(tenant, that.tenant)
                && Objects.equals(traceId, that.traceId)
                && Objects.equals(spanId, that.spanId)
                && Objects.equals(userId, that.userId)
                && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correlationId, causationId, tenant, traceId, spanId, userId, properties);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EventMetadata.class.getSimpleName() + "[", "]")
                .add("correlationId='" + correlationId + "'")
                .add("causationId='" + causationId + "'")
                .add("tenant='" + tenant + "'")
                .add("traceId='" + traceId + "'")
                .add("spanId='" + spanId + "'")
                .add("userId='" + userId + "'")
                .add("properties=" + safeString(properties))
                .toString();
    }

    /**
     * Builder for constructing immutable {@link EventMetadata} instances.
     *
     * @author Mohammad Yazdian
     * @since 0.1.0
     */
    public static final class Builder {

        private String correlationId;
        private String causationId;
        private String tenant;
        private String traceId;
        private String spanId;
        private String userId;
        private final Map<String, Object> properties = new LinkedHashMap<>();

        /**
         * Constructs an empty builder.
         *
         * <p>The constructor is private. use {@link EventMetadata#builder()}
         * to obtain a builder instance.
         */
        private Builder() {
        }

        /**
         * Sets the correlation identifier.
         *
         * @param correlationId the correlation id
         * @return this builder instance for method chaining
         */
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        /**
         * Sets the causation identifier.
         *
         * @param causationId the causation id
         * @return this builder instance for method chaining
         */
        public Builder causationId(String causationId) {
            this.causationId = causationId;
            return this;
        }

        /**
         * Sets the tenant identifier.
         *
         * @param tenant the tenant
         * @return this builder instance for method chaining
         */
        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Sets the trace identifier.
         *
         * @param traceId the trace id
         * @return this builder instance for method chaining
         */
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        /**
         * Sets the span identifier.
         *
         * @param spanId the span id
         * @return this builder instance for method chaining
         */
        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        /**
         * Sets the user identifier.
         *
         * @param userId the user id
         * @return this builder instance for method chaining
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Adds or replaces a single extra property.
         *
         * <p>Null values remove any existing mapping for the key
         * (the key is not stored).
         *
         * @param key   the property key
         * @param value the property value
         * @return this builder instance for method chaining
         * @throws DomainEventException if key is {@code null} or {@code blank}
         */
        public Builder properties(String key, Object value) {
            validateKey(key);
            if (value == null) {
                properties.remove(key);
            } else {
                properties.put(key, value);
            }
            return this;
        }

        /**
         * Replaces all extra properties with the given map.
         *
         * <p>Existing properties are cleared first. Null values in
         * {@code props} are dropped.
         *
         * @param props the source properties
         * @return this builder instance for method chaining
         * @throws DomainEventException if any key in props is {@code null} or {@code blank}
         */
        public Builder properties(Map<String, Object> props) {
            properties.clear();
            if (props != null) {
                props.forEach((k, v) -> {
                    validateKey(k);
                    if (v != null) properties.put(k, v);
                });
            }
            return this;
        }

        /**
         * Builds an immutable {@link EventMetadata}.
         *
         * <p>When every standard field is {@code null} and the properties map
         * is {@code empty}, the shared {@link EventMetadata#empty()} instance
         * is returned.
         *
         * @return the constructed metadata
         */
        public EventMetadata build() {
            if (hasNoStandardFields() && properties.isEmpty()) return EventMetadata.empty();
            return new EventMetadata(this);
        }

        private boolean hasNoStandardFields() {
            return hasNoTracingFields() && hasNoUserFields();
        }

        private boolean hasNoTracingFields() {
            return hasNoCorrelationOrCausation() && hasNoTraceOrSpan();
        }

        private boolean hasNoCorrelationOrCausation() {
            return correlationId == null && causationId == null;
        }

        private boolean hasNoTraceOrSpan() {
            return traceId == null && spanId == null;
        }

        private boolean hasNoUserFields() {
            return tenant == null && userId == null;
        }

        /**
         * Validates a property key.
         *
         * @param key the key to validate
         * @throws DomainEventException if key is {@code null} or {@code blank}
         */
        private static void validateKey(String key) {
            if (key == null || key.isBlank()) {
                throw new DomainEventException("The property key cannot be null or blank");
            }
        }
    }
}
