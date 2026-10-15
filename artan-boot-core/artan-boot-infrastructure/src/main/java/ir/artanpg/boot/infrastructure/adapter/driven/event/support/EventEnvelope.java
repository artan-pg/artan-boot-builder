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

package ir.artanpg.boot.infrastructure.adapter.driven.event.support;

import tools.jackson.databind.JsonNode;

import java.util.Map;

/**
 * Portable JSON envelope for a domain event.
 *
 * <p>This class is the wire DTO shared by the Jackson serializer and
 * deserializer. It captures the event's identity, type, aggregate
 * identifier, payload, and metadata in a flat JSON-friendly shape.
 * The class is intentionally mutable to allow Jackson to populate
 * it via bean-style getters and setters.
 *
 * <p>Instances of this class are not thread-safe. Each serialization
 * or deserialization operation should use its own instance.
 *
 * @author Mohammad Yazdian
 * @see ir.artanpg.boot.infrastructure.adapter.driven.event.serializer.JacksonEventSerializer
 * @see ir.artanpg.boot.infrastructure.adapter.driven.event.deserializer.JacksonEventDeserializer
 * @since 0.1.0
 */
public final class EventEnvelope {

    /**
     * Current format version emitted by the serializer and accepted
     * by the deserializer.
     */
    public static final int CURRENT_FORMAT_VERSION = 1;

    /**
     * Format version of this envelope.
     *
     * <p>Defaults to {@link #CURRENT_FORMAT_VERSION}.
     */
    private int formatVersion = CURRENT_FORMAT_VERSION;

    /**
     * Unique event identifier.
     */
    private String eventId;

    /**
     * Occurrence timestamp of the event in ISO-8601 format.
     */
    private String occurredAt;

    /**
     * Structured event type descriptor.
     */
    private EventTypeNode eventType;

    /**
     * Aggregate identifier descriptor.
     */
    private AggregateIdNode aggregateId;

    /**
     * Raw JSON payload of the event.
     */
    private JsonNode payload;

    /**
     * Optional event metadata.
     */
    private MetadataNode metadata;

    /**
     * Constructs a new, empty {@code EventEnvelope}.
     */
    public EventEnvelope() {
        super();
    }

    /**
     * Returns the format version of this envelope.
     *
     * @return the format version
     */
    public int getFormatVersion() {
        return formatVersion;
    }

    /**
     * Sets the format version of this envelope.
     *
     * @param formatVersion the format version to set
     */
    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }

    /**
     * Returns the unique event identifier.
     *
     * @return the event id
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the unique event identifier.
     *
     * @param eventId the event id to set
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Returns the occurrence timestamp of the event.
     *
     * @return the ISO-8601 timestamp
     */
    public String getOccurredAt() {
        return occurredAt;
    }

    /**
     * Sets the occurrence timestamp of the event.
     *
     * @param occurredAt the ISO-8601 timestamp to set
     */
    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }

    /**
     * Returns the structured event type descriptor.
     *
     * @return the event type node
     */
    public EventTypeNode getEventType() {
        return eventType;
    }

    /**
     * Sets the structured event type descriptor.
     *
     * @param eventType the event type node to set
     */
    public void setEventType(EventTypeNode eventType) {
        this.eventType = eventType;
    }

    /**
     * Returns the aggregate identifier descriptor.
     *
     * @return the aggregate id node
     */
    public AggregateIdNode getAggregateId() {
        return aggregateId;
    }

    /**
     * Sets the aggregate identifier descriptor.
     *
     * @param aggregateId the aggregate id node to set
     */
    public void setAggregateId(AggregateIdNode aggregateId) {
        this.aggregateId = aggregateId;
    }

    /**
     * Returns the raw JSON payload of the event.
     *
     * @return the payload node
     */
    public JsonNode getPayload() {
        return payload;
    }

    /**
     * Sets the raw JSON payload of the event.
     *
     * @param payload the payload node to set
     */
    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    /**
     * Returns the optional event metadata.
     *
     * @return the metadata node
     */
    public MetadataNode getMetadata() {
        return metadata;
    }

    /**
     * Sets the optional event metadata.
     *
     * @param metadata the metadata node to set
     */
    public void setMetadata(MetadataNode metadata) {
        this.metadata = metadata;
    }

    /**
     * Structured descriptor for an event type.
     *
     * <p>Mirrors the three coordinates of
     * {@link ir.artanpg.boot.domain.event.EventType}.
     */
    public static final class EventTypeNode {
        private String name;
        private String category;
        private String version;

        /**
         * Constructs a new, empty {@code EventTypeNode}.
         */
        public EventTypeNode() {
            super();
        }

        /**
         * Returns the mandatory logical name of the event type.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the mandatory logical name of the event type.
         *
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Returns the optional category grouping.
         *
         * @return the category
         */
        public String getCategory() {
            return category;
        }

        /**
         * Sets the optional category grouping.
         *
         * @param category the category to set
         */
        public void setCategory(String category) {
            this.category = category;
        }

        /**
         * Returns the optional schema version.
         *
         * @return the version
         */
        public String getVersion() {
            return version;
        }

        /**
         * Sets the optional schema version.
         *
         * @param version the version to set
         */
        public void setVersion(String version) {
            this.version = version;
        }
    }

    /**
     * Structured descriptor for an aggregate identifier.
     *
     * <p>Mirrors {@code IdentifierCodec.WireFormat}. The {@link #value} field
     * holds the recursive value-node map produced by the codec.
     */
    public static final class AggregateIdNode {

        /**
         * Fully qualified name of the identifier implementation.
         */
        private String identifierType;

        /**
         * Fully qualified name of the runtime value type.
         */
        private String valueType;

        /**
         * Recursive value-node map.
         */
        private Map<String, Object> value;

        /**
         * Constructs a new, empty {@code AggregateIdNode}.
         */
        public AggregateIdNode() {
            super();
        }

        /**
         * Returns the fully qualified identifier type name.
         *
         * @return the identifier type
         */
        public String getIdentifierType() {
            return identifierType;
        }

        /**
         * Sets the fully qualified identifier type name.
         *
         * @param identifierType the identifier type to set
         */
        public void setIdentifierType(String identifierType) {
            this.identifierType = identifierType;
        }

        /**
         * Returns the fully qualified value type name.
         *
         * @return the value type
         */
        public String getValueType() {
            return valueType;
        }

        /**
         * Sets the fully qualified value type name.
         *
         * @param valueType the value type to set
         */
        public void setValueType(String valueType) {
            this.valueType = valueType;
        }

        /**
         * Returns the recursive value-node map.
         *
         * @return the value map
         */
        public Map<String, Object> getValue() {
            return value;
        }

        /**
         * Sets the recursive value-node map.
         *
         * @param value the value map to set
         */
        public void setValue(Map<String, Object> value) {
            this.value = value;
        }
    }

    /**
     * Structured descriptor for optional event metadata.
     *
     * <p>All string fields correspond directly to the standard
     * fields of {@link ir.artanpg.boot.domain.event.EventMetadata}.
     * The {@link #properties} map carries arbitrary extra entries.
     * </p>
     */
    public static final class MetadataNode {

        /**
         * Correlation identifier.
         */
        private String correlationId;

        /**
         * Causation identifier.
         */
        private String causationId;

        /**
         * Tenant identifier.
         */
        private String tenant;

        /**
         * Distributed-tracing trace identifier.
         */
        private String traceId;

        /**
         * Distributed-tracing span identifier.
         */
        private String spanId;

        /**
         * User identifier.
         */
        private String userId;

        /**
         * Arbitrary extra metadata entries.
         */
        private Map<String, Object> properties;

        /**
         * Constructs a new, empty {@code MetadataNode}.
         */
        public MetadataNode() {
            super();
        }

        /**
         * Returns the correlation identifier.
         *
         * @return the correlation id
         */
        public String getCorrelationId() {
            return correlationId;
        }

        /**
         * Sets the correlation identifier.
         *
         * @param correlationId the correlation id to set
         */
        public void setCorrelationId(String correlationId) {
            this.correlationId = correlationId;
        }

        /**
         * Returns the causation identifier.
         *
         * @return the causation id
         */
        public String getCausationId() {
            return causationId;
        }

        /**
         * Sets the causation identifier.
         *
         * @param causationId the causation id to set
         */
        public void setCausationId(String causationId) {
            this.causationId = causationId;
        }

        /**
         * Returns the tenant identifier.
         *
         * @return the tenant
         */
        public String getTenant() {
            return tenant;
        }

        /**
         * Sets the tenant identifier.
         *
         * @param tenant the tenant to set
         */
        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        /**
         * Returns the distributed-tracing trace identifier.
         *
         * @return the trace id
         */
        public String getTraceId() {
            return traceId;
        }

        /**
         * Sets the distributed-tracing trace identifier.
         *
         * @param traceId the trace id to set
         */
        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        /**
         * Returns the distributed-tracing span identifier.
         *
         * @return the span id
         */
        public String getSpanId() {
            return spanId;
        }

        /**
         * Sets the distributed-tracing span identifier.
         *
         * @param spanId the span id to set
         */
        public void setSpanId(String spanId) {
            this.spanId = spanId;
        }

        /**
         * Returns the user identifier.
         *
         * @return the user id
         */
        public String getUserId() {
            return userId;
        }

        /**
         * Sets the user identifier.
         *
         * @param userId the user id to set
         */
        public void setUserId(String userId) {
            this.userId = userId;
        }

        /**
         * Returns the arbitrary extra metadata entries.
         *
         * @return the properties map
         */
        public Map<String, Object> getProperties() {
            return properties;
        }

        /**
         * Sets the arbitrary extra metadata entries.
         *
         * @param properties the properties map to set
         */
        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }
}
