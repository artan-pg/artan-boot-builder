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

package ir.artanpg.boot.infrastructure.adapter.driven.event.deserializer;

import ir.artanpg.boot.application.port.driven.event.DomainEventDescriptor;
import ir.artanpg.boot.application.port.driven.event.DomainEventResolver;
import ir.artanpg.boot.application.port.driven.event.EventDeserializer;
import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventMetadata;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.event.support.EventReconstitutionSupport;
import ir.artanpg.boot.domain.exception.event.DomainEventException;
import ir.artanpg.boot.domain.exception.event.SerializationException;
import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.domain.model.IdentifierCodec;
import ir.artanpg.boot.infrastructure.adapter.driven.event.support.EventEnvelope;
import org.jspecify.annotations.NonNull;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jackson-based {@link EventDeserializer} consuming {@code application/json}
 * envelopes.
 *
 * <p>This deserializer reads a JSON document produced by a compatible
 * serializer (typically JacksonEventSerializer) and reconstructs a
 * concrete DomainEvent instance of the requested type. The envelope
 * format is defined by EventEnvelope and is validated before any
 * payload is decoded.
 *
 * <p>Structural type resolution (payload type and identifier type) is
 * delegated to a {@link DomainEventResolver}, which consults the
 * {@link ir.artanpg.boot.application.port.driven.event.DomainEventRegistry}.
 * This eliminates reflective type extraction on every deserialization call
 * and ensures that only registered event types are accepted.
 *
 * <p>This class is stateless apart from the supplied {@link ObjectMapper}
 * and {@link DomainEventResolver}, and is thread-safe as long as both
 * collaborators are thread-safe.
 *
 * @author Mohammad Yazdian
 * @see EventDeserializer
 * @see EventEnvelope
 * @see DomainEventResolver
 * @since 0.1.0
 */
public final class JacksonEventDeserializer implements EventDeserializer {

    /**
     * Canonical format identifier produced by this deserializer.
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
     * Jackson mapper used for JSON parsing and payload conversion.
     */
    private final ObjectMapper objectMapper;

    /**
     * Resolver used to look up the event descriptor from the registry.
     */
    private final DomainEventResolver resolver;

    /**
     * Creates a deserializer with the given ObjectMapper and resolver.
     *
     * @param objectMapper Jackson mapper
     * @param resolver     the event resolver backed by the registry
     * @throws SerializationException if objectMapper or resolver is {@code null}
     */
    public JacksonEventDeserializer(@NonNull ObjectMapper objectMapper,
                                    @NonNull DomainEventResolver resolver) {
        if (objectMapper == null) throw new SerializationException("The ObjectMapper cannot be null");
        if (resolver == null) throw new SerializationException("The DomainEventResolver cannot be null");
        this.objectMapper = objectMapper;
        this.resolver = resolver;
    }

    @Override
    @NonNull
    public <E extends DomainEvent<?, ?>> E deserialize(@NonNull byte[] data, @NonNull Class<E> type) {
        if (data == null || data.length == 0) {
            throw new SerializationException("The serialized data cannot be null or empty");
        }
        if (type == null) throw new SerializationException("The target event type cannot be null");
        if (!AbstractDomainEvent.class.isAssignableFrom(type)) {
            throw new SerializationException(
                    "The target event type must extend AbstractDomainEvent: " + type.getName());
        }

        try {
            EventEnvelope envelope = objectMapper.readValue(data, EventEnvelope.class);
            validateEnvelope(envelope);

            Instant occurredAt = Instant.parse(envelope.getOccurredAt());
            EventType eventType = toEventType(envelope.getEventType());

            DomainEventDescriptor descriptor = resolveDescriptor(eventType);
            validateClassMatch(eventType, descriptor.domainEvent(), type);

            Identifier<?> aggregateId = toAggregateId(envelope.getAggregateId(), descriptor.identifierType());
            Object payload = toPayload(envelope.getPayload(), descriptor.payloadType());
            EventMetadata metadata = toMetadata(envelope.getMetadata());

            return EventReconstitutionSupport.reconstitute(
                    type,
                    envelope.getEventId(),
                    occurredAt,
                    aggregateId,
                    payload,
                    eventType,
                    metadata);
        } catch (SerializationException ex) {
            throw ex;
        } catch (Exception ex) {
            String messageException =
                    String.format("Failed to deserialize event to type %s: %s", type.getName(), ex.getMessage());
            throw new SerializationException(messageException, ex);
        }
    }

    @Override
    public String supportedFormat() {
        return FORMAT;
    }

    /**
     * Resolves the event descriptor from the registry and translates
     * {@link DomainEventException} into {@link SerializationException}.
     *
     * @param eventType the event type extracted from the envelope
     * @return the resolved descriptor
     * @throws SerializationException if the event type is not registered
     */
    private DomainEventDescriptor resolveDescriptor(EventType eventType) {
        try {
            return resolver.resolve(eventType);
        } catch (DomainEventException ex) {
            throw new SerializationException("The event type is not registered: " + ex.getMessage(), ex);
        }
    }

    /**
     * Validates that the class registered for the event type matches
     * the class requested by the caller.
     *
     * @param eventType       the event type
     * @param registeredClass the class registered in the registry
     * @param requestedClass  the class requested by the caller
     * @throws SerializationException if the classes do not match
     */
    private static void validateClassMatch(EventType eventType, Class<?> registeredClass, Class<?> requestedClass) {
        if (!registeredClass.equals(requestedClass)) {
            throw new SerializationException(String.format(
                    "The event type [%s] is registered to class [%s], but the requested type is [%s]",
                    eventType, registeredClass.getName(), requestedClass.getName()));
        }
    }

    /**
     * Validates the mandatory fields of the parsed envelope.
     *
     * @param envelope the parsed envelope
     * @throws SerializationException if the envelope is {@code null},
     *                                or any required field is missing or {@code blank}
     */
    private static void validateEnvelope(EventEnvelope envelope) {
        if (envelope == null) throw new SerializationException("The event envelope cannot be null");
        if (envelope.getFormatVersion() != EventEnvelope.CURRENT_FORMAT_VERSION) {
            throw new SerializationException("Unsupported event envelope formatVersion:" + envelope.getFormatVersion());
        }
        if (envelope.getEventId() == null || envelope.getEventId().isBlank()) {
            throw new SerializationException("The envelope eventId cannot be null or blank");
        }
        if (envelope.getOccurredAt() == null || envelope.getOccurredAt().isBlank()) {
            throw new SerializationException("The envelope occurredAt cannot be null or blank");
        }
        if (envelope.getEventType() == null || envelope.getEventType().getName() == null
                || envelope.getEventType().getName().isBlank()) {
            throw new SerializationException("The envelope eventType.name cannot be null or blank");
        }
        if (envelope.getAggregateId() == null) {
            throw new SerializationException("The envelope aggregateId cannot be null");
        }
        if (envelope.getPayload() == null || envelope.getPayload().isNull()) {
            throw new SerializationException("The envelope payload cannot be null");
        }
    }

    /**
     * Converts an envelope event-type node into an {@link EventType}.
     *
     * @param node the event-type node
     * @return the corresponding event type
     * @throws SerializationException if the node fails validation
     */
    private static EventType toEventType(EventEnvelope.EventTypeNode node) {
        return EventType.of(node.getName(), node.getCategory(), node.getVersion());
    }

    /**
     * Decodes the aggregate identifier from the envelope.
     *
     * @param node           the aggregate-id node
     * @param identifierType the identifier type resolved from the registry
     * @return the decoded identifier
     * @throws SerializationException if any node field is missing or decoding fails
     */
    private Identifier<?> toAggregateId(EventEnvelope.AggregateIdNode node,
                                        Class<? extends Identifier<?>> identifierType) {
        if (node.getIdentifierType() == null || node.getIdentifierType().isBlank()) {
            throw new SerializationException("The aggregateId.identifierType cannot be null or blank");
        }
        if (node.getValueType() == null || node.getValueType().isBlank()) {
            throw new SerializationException("The aggregateId.valueType cannot be null or blank");
        }
        if (node.getValue() == null || node.getValue().isEmpty()) {
            throw new SerializationException("The aggregateId.value cannot be null or empty");
        }

        IdentifierCodec.ValueNode valueNode = mapToValueNode(node.getValue());
        IdentifierCodec.WireFormat wire = new IdentifierCodec.WireFormat(
                node.getIdentifierType(),
                node.getValueType(),
                valueNode);

        try {
            return IdentifierCodec.fromWire(wire, identifierType);
        } catch (RuntimeException ex) {
            throw new SerializationException("Failed to decode aggregateId: " + ex.getMessage(), ex);
        }
    }

    /**
     * Recursively converts a JSON-style map into a
     * {@link IdentifierCodec.ValueNode} tree.
     *
     * @param map the map representation of a value node
     * @return the decoded value node
     * @throws SerializationException if the map does not conform to the expected wire shape
     */
    private static IdentifierCodec.ValueNode mapToValueNode(Map<String, Object> map) {
        Object kind = map.get(KIND_KEY);
        if (!(kind instanceof String kindName)) {
            throw new SerializationException("The aggregateId.value.kind must be a string");
        }

        if (SCALAR_KIND.equals(kindName)) return buildScalarNode(map);
        if (COMPOSITE_KIND.equals(kindName)) return buildCompositeNode(map);

        throw new SerializationException("Unknown aggregateId.value.kind: " + kindName);
    }

    /**
     * Builds a scalar value node from the given map.
     *
     * @param map the map containing scalar data
     * @return the scalar value node
     * @throws SerializationException if type or value is invalid
     */
    private static IdentifierCodec.ValueNode buildScalarNode(Map<String, Object> map) {
        Object type = map.get(TYPE_KEY);
        Object value = map.get(VALUE_KEY);
        if (!(type instanceof String typeName) || typeName.isBlank()) {
            throw new SerializationException("The scalar type cannot be null or blank");
        }
        if (!(value instanceof String stringValue)) {
            throw new SerializationException("The scalar value must be a string");
        }
        return new IdentifierCodec.ValueNode.Scalar(typeName, stringValue);
    }

    /**
     * Builds a composite value node from the given map.
     *
     * @param map the map containing composite data
     * @return the composite value node
     * @throws SerializationException if type or fields are invalid
     */
    private static IdentifierCodec.ValueNode buildCompositeNode(Map<String, Object> map) {
        Object type = map.get(TYPE_KEY);
        Object fieldsObj = map.get(FIELDS_KEY);
        if (!(type instanceof String typeName) || typeName.isBlank()) {
            throw new SerializationException("The composite type cannot be null or blank");
        }
        if (!(fieldsObj instanceof Map<?, ?> rawFields)) {
            throw new SerializationException("The composite fields must be a map");
        }
        Map<String, IdentifierCodec.ValueNode> fields = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawFields.entrySet()) {
            addCompositeField(fields, entry);
        }
        return new IdentifierCodec.ValueNode.Composite(typeName, fields);
    }

    /**
     * Validates and adds a single field to the composite fields map.
     *
     * @param fields the target fields map
     * @param entry  the map entry to process
     * @throws SerializationException if the entry value is invalid
     */
    @SuppressWarnings("unchecked")
    private static void addCompositeField(Map<String, IdentifierCodec.ValueNode> fields, Map.Entry<?, ?> entry) {
        String fieldName = (String) entry.getKey();
        if (!(entry.getValue() instanceof Map<?, ?> nested)) {
            throw new SerializationException("Composite field '" + fieldName + "' must be a value node map");
        }
        fields.put(fieldName, mapToValueNode((Map<String, Object>) nested));
    }

    /**
     * Converts the payload JSON node into the concrete payload type.
     *
     * @param payloadNode the payload node
     * @param payloadType the payload type resolved from the registry
     * @return the converted payload object
     * @throws SerializationException if conversion fails
     */
    private Object toPayload(JsonNode payloadNode, Class<?> payloadType) {
        try {
            return objectMapper.convertValue(payloadNode, payloadType);
        } catch (JacksonException ex) {
            String messageException =
                    String.format("Failed to convert payload to type %s: %s", payloadType.getName(), ex.getMessage());
            throw new SerializationException(messageException, ex);
        }
    }

    /**
     * Converts the envelope metadata node into an {@link EventMetadata}
     * instance.
     *
     * @param node the metadata node
     * @return the metadata
     */
    private static EventMetadata toMetadata(EventEnvelope.MetadataNode node) {
        if (node == null) return EventMetadata.empty();
        EventMetadata.Builder builder = EventMetadata.empty().toBuilder()
                .correlationId(node.getCorrelationId())
                .causationId(node.getCausationId())
                .tenant(node.getTenant())
                .traceId(node.getTraceId())
                .spanId(node.getSpanId())
                .userId(node.getUserId());

        if (node.getProperties() != null && !node.getProperties().isEmpty()) {
            builder.properties(node.getProperties());
        }

        return builder.build();
    }
}
