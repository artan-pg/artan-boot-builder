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

package ir.artanpg.boot.infrastructure.adapter.driven.event.serializer;

import ir.artanpg.boot.application.port.driven.event.EventSerializer;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventMetadata;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.event.SerializationException;
import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.domain.model.IdentifierCodec;
import ir.artanpg.boot.infrastructure.adapter.driven.event.support.EventEnvelope;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jackson-based {@link EventSerializer} producing {@code application/json}
 * envelopes.
 *
 * <p>This serializer converts a DomainEvent into a portable JSON document
 * whose structure is defined by EventEnvelope. The envelope carries the
 * event id, occurrence timestamp, event type, aggregate identifier, payload,
 * and metadata.
 *
 * <p>Aggregate identifiers are encoded through {@link IdentifierCodec#toWire}
 * into a nested value tree that mirrors the codec's wire model. Payloads are
 * converted directly by the supplied Jackson mapper.
 *
 * <p>This class is stateless apart from the supplied {@link ObjectMapper} and
 * is thread-safe as long as the mapper is thread-safe.
 *
 * @author Mohammad Yazdian
 * @see EventSerializer
 * @see EventEnvelope
 * @since 0.1.0
 */
public final class JacksonEventSerializer implements EventSerializer {

    /**
     * Canonical format identifier produced by this serializer.
     */
    public static final String FORMAT = "application/json";

    // Constants for JSON wire format keys
    private static final String KIND_KEY = "kind";
    private static final String TYPE_KEY = "type";
    private static final String VALUE_KEY = "value";
    private static final String FIELDS_KEY = "fields";
    private static final String SCALAR_KIND = "scalar";
    private static final String COMPOSITE_KIND = "composite";

    /**
     * Jackson mapper used for JSON generation and payload conversion.
     */
    private final ObjectMapper objectMapper;

    /**
     * Creates a serializer with the given ObjectMapper.
     *
     * @param objectMapper Jackson mapper
     * @throws SerializationException if {@code objectMapper} is {@code null}
     */
    public JacksonEventSerializer(@NonNull ObjectMapper objectMapper) {
        if (objectMapper == null) throw new SerializationException("The ObjectMapper cannot be null");
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(@NonNull DomainEvent<?, ?> event) {
        if (event == null) throw new SerializationException("The event cannot be null");
        try {
            EventEnvelope envelope = toEnvelope(event);
            return objectMapper.writeValueAsBytes(envelope);
        } catch (SerializationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SerializationException(
                    "Failed to serialize event of type " + event.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    public String supportedFormat() {
        return FORMAT;
    }

    /**
     * Converts a domain event into an envelope ready for JSON serialization.
     *
     * @param event the event to convert
     * @return the populated envelope
     * @throws SerializationException if any nested conversion fails
     */
    private EventEnvelope toEnvelope(DomainEvent<?, ?> event) {
        EventEnvelope envelope = new EventEnvelope();
        envelope.setFormatVersion(EventEnvelope.CURRENT_FORMAT_VERSION);
        envelope.setEventId(event.getEventId());
        envelope.setOccurredAt(event.getOccurredAt().toString());
        envelope.setEventType(toEventTypeNode(event.getEventType()));
        envelope.setAggregateId(toAggregateIdNode(event.getAggregateId()));
        envelope.setPayload(objectMapper.valueToTree(event.getPayload()));
        envelope.setMetadata(toMetadataNode(event.getMetadata()));
        return envelope;
    }

    /**
     * Converts an {@link EventType} into its envelope node representation.
     *
     * @param eventType the event type
     * @return the corresponding envelope node
     */
    private static EventEnvelope.EventTypeNode toEventTypeNode(EventType eventType) {
        EventEnvelope.EventTypeNode node = new EventEnvelope.EventTypeNode();
        node.setName(eventType.getName());
        node.setCategory(eventType.getCategory());
        node.setVersion(eventType.getVersion());
        return node;
    }

    /**
     * Converts an aggregate identifier into its envelope node representation.
     *
     * <p>The identifier is first encoded via {@link IdentifierCodec#toWire}
     * and then mapped into the envelope's map-based form.
     *
     * @param aggregateId the aggregate identifier
     * @return the corresponding envelope node
     * @throws SerializationException if the codec produces an unknown node shape
     */
    private EventEnvelope.AggregateIdNode toAggregateIdNode(Identifier<?> aggregateId) {
        IdentifierCodec.WireFormat wire = IdentifierCodec.toWire(aggregateId);
        EventEnvelope.AggregateIdNode node = new EventEnvelope.AggregateIdNode();
        node.setIdentifierType(wire.identifierType());
        node.setValueType(wire.valueType());
        node.setValue(valueNodeToMap(wire.value()));
        return node;
    }

    /**
     * Recursively converts a codec value node into a map representation
     * suitable for Jackson serialization.
     *
     * @param valueNode the value node to convert
     * @return the map representation
     * @throws SerializationException if the node type is unknown
     */
    private static Map<String, Object> valueNodeToMap(IdentifierCodec.ValueNode valueNode) {
        return switch (valueNode) {
            case IdentifierCodec.ValueNode.Scalar(String type, String value) -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put(KIND_KEY, SCALAR_KIND);
                map.put(TYPE_KEY, type);
                map.put(VALUE_KEY, value);
                yield map;
            }
            case IdentifierCodec.ValueNode.Composite(String type, Map<String, IdentifierCodec.ValueNode> fields) -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put(KIND_KEY, COMPOSITE_KIND);
                map.put(TYPE_KEY, type);
                Map<String, Object> nestedFields = new LinkedHashMap<>();
                fields.forEach((name, child) -> nestedFields.put(name, valueNodeToMap(child)));
                map.put(FIELDS_KEY, nestedFields);
                yield map;
            }
        };
    }

    /**
     * Converts {@link EventMetadata} into its envelope node representation.
     *
     * <p>Returns an empty node when the metadata is {@code null} or equal to
     * {@link EventMetadata#empty()}.
     *
     * @param metadata the metadata
     * @return the corresponding envelope node
     */
    private static EventEnvelope.MetadataNode toMetadataNode(EventMetadata metadata) {
        EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();
        if (metadata == null || metadata.equals(EventMetadata.empty())) return node;

        node.setCorrelationId(metadata.getCorrelationId());
        node.setCausationId(metadata.getCausationId());
        node.setTenant(metadata.getTenant());
        node.setTraceId(metadata.getTraceId());
        node.setSpanId(metadata.getSpanId());
        node.setUserId(metadata.getUserId());

        if (!metadata.getProperties().isEmpty()) {
            node.setProperties(new LinkedHashMap<>(metadata.getProperties()));
        }

        return node;
    }
}
