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
import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventMetadata;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.event.DomainEventException;
import ir.artanpg.boot.domain.exception.event.SerializationException;
import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.infrastructure.adapter.driven.event.serializer.JacksonEventSerializer;
import ir.artanpg.boot.infrastructure.adapter.driven.event.support.EventEnvelope;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link JacksonEventDeserializer}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("JacksonEventDeserializer")
class JacksonEventDeserializerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DomainEventResolver resolver;
    private JacksonEventDeserializer deserializer;

    @BeforeEach
    void setUp() {
        resolver = mock(DomainEventResolver.class);
        deserializer = new JacksonEventDeserializer(objectMapper, resolver);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should create instance when ObjectMapper and resolver are provided")
        void constructor_ShouldCreateInstance_WhenObjectMapperAndResolverProvided() {
            // given
            ObjectMapper mapper = new ObjectMapper();
            DomainEventResolver res = mock(DomainEventResolver.class);

            // when
            JacksonEventDeserializer result = new JacksonEventDeserializer(mapper, res);

            // then
            then(result)
                    .as("Deserializer instance must be created from valid dependencies")
                    .isNotNull();
            then(result.supportedFormat())
                    .as("Supported format must match FORMAT constant")
                    .isEqualTo(JacksonEventDeserializer.FORMAT);
        }

        @Test
        @DisplayName("Should throw SerializationException when ObjectMapper is null")
        @SuppressWarnings("DataFlowIssue")
        void constructor_ShouldThrowSerializationException_WhenObjectMapperIsNull() {
            // given
            DomainEventResolver res = mock(DomainEventResolver.class);

            // when & then
            thenThrownBy(() -> new JacksonEventDeserializer(null, res))
                    .as("Null ObjectMapper must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The ObjectMapper cannot be null");
        }

        @Test
        @DisplayName("Should throw SerializationException when resolver is null")
        @SuppressWarnings("DataFlowIssue")
        void constructor_ShouldThrowSerializationException_WhenResolverIsNull() {
            // given
            ObjectMapper mapper = new ObjectMapper();

            // when & then
            thenThrownBy(() -> new JacksonEventDeserializer(mapper, null))
                    .as("Null resolver must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The DomainEventResolver cannot be null");
        }
    }

    @Nested
    @DisplayName("deserialize(byte[], Class)")
    class Deserialize {

        @Test
        @DisplayName("Should deserialize valid envelope into target event type")
        void deserialize_ShouldReturnEvent_WhenEnvelopeIsValid() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            byte[] data = toJsonBytes(validEnvelope());

            // when
            SampleEvent event = deserializer.deserialize(data, SampleEvent.class);

            // then
            then(event)
                    .as("Deserialized event must not be null")
                    .isNotNull();
            then(event.getEventId())
                    .as("eventId must be restored from the envelope")
                    .isEqualTo("evt-1");
            then(event.getOccurredAt())
                    .as("occurredAt must be parsed back to an Instant")
                    .isEqualTo(Instant.parse("2026-01-15T10:30:00Z"));
            then(event.getEventType())
                    .as("eventType must be reconstructed from the envelope node")
                    .isEqualTo(EventType.of("SampleCreated", "sample", "1"));
            then(event.getAggregateId().value())
                    .as("aggregate identifier must be decoded from the envelope scalar value")
                    .isEqualTo("agg-1");
            then(event.getPayload().name())
                    .as("payload.name must be converted into the target payload record")
                    .isEqualTo("widget");
            then(event.getPayload().quantity())
                    .as("payload.quantity must be converted into the target payload record")
                    .isEqualTo(3);
            then(event.getMetadata().getCorrelationId())
                    .as("metadata.correlationId must be mapped into EventMetadata")
                    .isEqualTo("corr-1");
            then(event.getMetadata().getCausationId())
                    .as("metadata.causationId must be mapped into EventMetadata")
                    .isEqualTo("cause-1");
            then(event.getMetadata().getTenant())
                    .as("metadata.tenant must be mapped into EventMetadata")
                    .isEqualTo("tenant-1");
            then(event.getMetadata().getTraceId())
                    .as("metadata.traceId must be mapped into EventMetadata")
                    .isEqualTo("trace-1");
            then(event.getMetadata().getSpanId())
                    .as("metadata.spanId must be mapped into EventMetadata")
                    .isEqualTo("span-1");
            then(event.getMetadata().getUserId())
                    .as("metadata.userId must be mapped into EventMetadata")
                    .isEqualTo("user-1");
            then(event.getMetadata().getProperty("extra"))
                    .as("Custom metadata property 'extra' must be preserved")
                    .isEqualTo("extra-value");
        }

        @Test
        @DisplayName("Should use empty metadata when metadata node is null")
        void deserialize_ShouldUseEmptyMetadata_WhenMetadataNodeIsNull() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.setMetadata(null);
            byte[] data = toJsonBytes(envelope);

            // when
            SampleEvent event = deserializer.deserialize(data, SampleEvent.class);

            // then
            then(event.getMetadata())
                    .as("EventMetadata must fall back to empty() when the envelope metadata node is absent")
                    .isEqualTo(EventMetadata.empty());
        }

        @Test
        @DisplayName("Should ignore empty properties map in metadata")
        void deserialize_ShouldIgnoreEmptyProperties_WhenPropertiesMapIsEmpty() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            EventEnvelope.MetadataNode metadata = new EventEnvelope.MetadataNode();
            metadata.setCorrelationId("corr-only");
            metadata.setProperties(Map.of());
            envelope.setMetadata(metadata);
            byte[] data = toJsonBytes(envelope);

            // when
            SampleEvent event = deserializer.deserialize(data, SampleEvent.class);

            // then
            then(event.getMetadata().getCorrelationId())
                    .as("correlationId must be preserved even when the properties map is empty")
                    .isEqualTo("corr-only");
            then(event.getMetadata().getProperties())
                    .as("EventMetadata properties must remain empty when the envelope supplied an empty map")
                    .isEmpty();
        }

        @Test
        @DisplayName("Should ignore null properties map in metadata")
        void deserialize_ShouldIgnoreNullProperties_WhenPropertiesMapIsNull() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            EventEnvelope.MetadataNode metadata = new EventEnvelope.MetadataNode();
            metadata.setCorrelationId("corr-null-props");
            metadata.setProperties(null);
            envelope.setMetadata(metadata);
            byte[] data = toJsonBytes(envelope);

            // when
            SampleEvent event = deserializer.deserialize(data, SampleEvent.class);

            // then
            then(event.getMetadata().getCorrelationId())
                    .as("correlationId must be preserved even when the properties map is null")
                    .isEqualTo("corr-null-props");
            then(event.getMetadata().getProperties())
                    .as("EventMetadata properties must remain empty when the envelope supplied a null map")
                    .isEmpty();
        }

        @Test
        @DisplayName("Should decode composite aggregate id correctly")
        void deserialize_ShouldDecodeCompositeAggregateId_WhenValueIsComposite() {
            // given
            EventType eventType = EventType.of("SampleCreated", "sample", "1");
            DomainEventDescriptor descriptor = mockDescriptor(eventType, CompositeSampleEvent.class,
                    SamplePayload.class, CompositeSampleIdentifier.class);
            doReturn(descriptor).when(resolver).resolve(eventType);
            EventEnvelope envelope = validEnvelope();
            EventEnvelope.AggregateIdNode aggregateId = new EventEnvelope.AggregateIdNode();
            aggregateId.setIdentifierType(CompositeSampleIdentifier.class.getName());
            aggregateId.setValueType(CompositeIdValue.class.getName());
            aggregateId.setValue(compositeValueNode());
            envelope.setAggregateId(aggregateId);
            byte[] data = toJsonBytes(envelope);

            // when
            CompositeSampleEvent event = deserializer.deserialize(data, CompositeSampleEvent.class);

            // then
            then(event.getAggregateId().value().partA())
                    .as("Composite aggregate id 'partA' must be reconstructed from its nested scalar node")
                    .isEqualTo("part-a");
            then(event.getAggregateId().value().partB())
                    .as("Composite aggregate id 'partB' must be reconstructed from its nested scalar node")
                    .isEqualTo("part-b");
        }

        @Test
        @DisplayName("Should throw SerializationException when data is null")
        @SuppressWarnings("DataFlowIssue")
        void deserialize_ShouldThrowSerializationException_WhenDataIsNull() {
            // given

            // when & then
            thenThrownBy(() -> deserializer.deserialize(null, SampleEvent.class))
                    .as("Null data must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The serialized data cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw SerializationException when data is empty")
        void deserialize_ShouldThrowSerializationException_WhenDataIsEmpty() {
            // given
            byte[] data = new byte[0];

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Empty data must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The serialized data cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw SerializationException when target type is null")
        @SuppressWarnings("DataFlowIssue")
        void deserialize_ShouldThrowSerializationException_WhenTypeIsNull() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, null))
                    .as("Null target type must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The target event type cannot be null");
        }

        @Test
        @DisplayName("Should throw SerializationException when type does not extend AbstractDomainEvent")
        void deserialize_ShouldThrowSerializationException_WhenTypeDoesNotExtendAbstractDomainEvent() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, NonAbstractEvent.class))
                    .as("Non-AbstractDomainEvent types must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("The target event type must extend AbstractDomainEvent");
        }

        @Test
        @DisplayName("Should throw SerializationException when registered class differs from requested type")
        void deserialize_ShouldThrowSerializationException_WhenRegisteredClassDiffersFromRequestedType() {
            // given
            stubResolverFor(MultiLevelEvent.class, SamplePayload.class);
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Mismatch between registered and requested class must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("is registered to class")
                    .hasMessageContaining("but the requested type is");
        }

        @Test
        @DisplayName("Should throw SerializationException when resolver throws DomainEventException")
        void deserialize_ShouldThrowSerializationException_WhenResolverThrowsDomainEventException() {
            // given
            EventType eventType = EventType.of("SampleCreated", "sample", "1");
            doThrow(
                    new DomainEventException("No domain event class registered for event type"))
                    .when(resolver).resolve(eventType);
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("DomainEventException from resolver must be translated to SerializationException")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("The event type is not registered");
        }

        @Test
        @DisplayName("Should deserialize event when AbstractDomainEvent is reached via intermediate class")
        void deserialize_ShouldReturnEvent_WhenAbstractDomainEventFoundViaIntermediateClass() {
            // given
            stubResolverFor(MultiLevelEvent.class, SamplePayload.class);
            byte[] data = toJsonBytes(validEnvelope());

            // when
            MultiLevelEvent event = deserializer.deserialize(data, MultiLevelEvent.class);

            // then
            then(event)
                    .as("Deserialized event resolved via a non-generic intermediate superclass must not be null")
                    .isNotNull();
            then(event.getEventId())
                    .as("eventId must be restored through intermediate class hierarchy")
                    .isEqualTo("evt-1");
            then(event.getAggregateId().value())
                    .as("aggregate id must be restored through intermediate class hierarchy")
                    .isEqualTo("agg-1");
            then(event.getPayload().name())
                    .as("payload must be restored through intermediate class hierarchy")
                    .isEqualTo("widget");
        }

        @Test
        @DisplayName("Should deserialize event when payload type is a parameterized type")
        void deserialize_ShouldReturnEvent_WhenPayloadTypeIsParameterizedType() {
            // given
            stubResolverFor(MapPayloadEvent.class, Map.class);
            EventEnvelope envelope = validEnvelope();
            ObjectNode mapPayload = objectMapper.createObjectNode();
            mapPayload.put("key1", "value1");
            mapPayload.put("key2", "value2");
            envelope.setPayload(mapPayload);
            byte[] data = toJsonBytes(envelope);

            // when
            MapPayloadEvent event = deserializer.deserialize(data, MapPayloadEvent.class);

            // then
            then(event)
                    .as("Deserialized event whose payload type is a ParameterizedType must not be null")
                    .isNotNull();
            then(event.getPayload())
                    .as("Payload map must contain every expected JSON entry")
                    .containsEntry("key1", "value1")
                    .containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("Should throw SerializationException when JSON is malformed")
        void deserialize_ShouldThrowSerializationException_WhenJsonIsMalformed() {
            // given
            byte[] data = "{not-json".getBytes(StandardCharsets.UTF_8);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Malformed JSON must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Failed to deserialize event to type");
        }

        @Test
        @DisplayName("Should throw SerializationException when envelope is null")
        void deserialize_ShouldThrowSerializationException_WhenEnvelopeIsNull() {
            // given
            byte[] data = "null".getBytes(StandardCharsets.UTF_8);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Top-level null literal must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The event envelope cannot be null");
        }

        @Test
        @DisplayName("Should throw SerializationException when formatVersion is unsupported")
        void deserialize_ShouldThrowSerializationException_WhenFormatVersionUnsupported() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setFormatVersion(99);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Unsupported formatVersion must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Unsupported event envelope formatVersion");
        }

        @Test
        @DisplayName("Should throw SerializationException when eventId is null")
        void deserialize_ShouldThrowSerializationException_WhenEventIdIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setEventId(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null eventId must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when eventId is blank")
        void deserialize_ShouldThrowSerializationException_WhenEventIdIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setEventId("   ");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Blank eventId must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when eventId is empty string")
        void deserialize_ShouldThrowSerializationException_WhenEventIdIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setEventId("");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Empty eventId must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when occurredAt is null")
        void deserialize_ShouldThrowSerializationException_WhenOccurredAtIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setOccurredAt(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null occurredAt must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope occurredAt cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when occurredAt is blank")
        void deserialize_ShouldThrowSerializationException_WhenOccurredAtIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setOccurredAt("   ");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Blank occurredAt must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope occurredAt cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when occurredAt is empty string")
        void deserialize_ShouldThrowSerializationException_WhenOccurredAtIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setOccurredAt("");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Empty occurredAt must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope occurredAt cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when eventType is null")
        void deserialize_ShouldThrowSerializationException_WhenEventTypeIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setEventType(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null eventType node must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventType.name cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when eventType.name is null")
        void deserialize_ShouldThrowSerializationException_WhenEventTypeNameIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            EventEnvelope.EventTypeNode eventType = new EventEnvelope.EventTypeNode();
            eventType.setName(null);
            envelope.setEventType(eventType);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null eventType.name must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventType.name cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when eventType.name is blank")
        void deserialize_ShouldThrowSerializationException_WhenEventTypeNameIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            EventEnvelope.EventTypeNode eventType = new EventEnvelope.EventTypeNode();
            eventType.setName("   ");
            envelope.setEventType(eventType);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Blank eventType.name must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventType.name cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when eventType.name is empty string")
        void deserialize_ShouldThrowSerializationException_WhenEventTypeNameIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            EventEnvelope.EventTypeNode eventType = new EventEnvelope.EventTypeNode();
            eventType.setName("");
            envelope.setEventType(eventType);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Empty eventType.name must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventType.name cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId is null")
        void deserialize_ShouldThrowSerializationException_WhenAggregateIdIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setAggregateId(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null aggregateId node must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope aggregateId cannot be null");
        }

        @Test
        @DisplayName("Should throw SerializationException when validateEnvelope sees null payload via reflection")
        void deserialize_ShouldThrowSerializationException_WhenValidateEnvelopePayloadIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setPayload(null);
            Method validateEnvelope;
            try {
                validateEnvelope =
                        JacksonEventDeserializer.class.getDeclaredMethod("validateEnvelope", EventEnvelope.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            validateEnvelope.setAccessible(true);

            // when & then
            thenThrownBy(() -> {
                try {
                    validateEnvelope.invoke(null, envelope);
                } catch (InvocationTargetException ex) {
                    throw ex.getCause();
                }
            })
                    .as("Null payload field must be rejected by validateEnvelope")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope payload cannot be null");
        }

        @Test
        @DisplayName("Should throw SerializationException when payload is JSON null node")
        void deserialize_ShouldThrowSerializationException_WhenPayloadIsJsonNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setPayload(NullNode.getInstance());
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("In-memory NullNode payload must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope payload cannot be null");
        }

        @Test
        @DisplayName("Should throw SerializationException when payload is explicit JSON null in wire data")
        void deserialize_ShouldThrowSerializationException_WhenPayloadIsExplicitJsonNull() {
            // given
            String json = """
                    {
                      "formatVersion": 1,
                      "eventId": "evt-1",
                      "occurredAt": "2026-01-15T10:30:00Z",
                      "eventType": { "name": "SampleCreated", "category": "sample", "version": "1" },
                      "aggregateId": {
                        "identifierType": "%s",
                        "valueType": "java.lang.String",
                        "value": { "kind": "scalar", "type": "java.lang.String", "value": "agg-1" }
                      },
                      "payload": null
                    }
                    """.formatted(SampleIdentifier.class.getName());
            byte[] data = json.getBytes(StandardCharsets.UTF_8);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Explicit JSON null payload must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope payload cannot be null");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.identifierType is null")
        void deserialize_ShouldThrowSerializationException_WhenIdentifierTypeIsNull() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setIdentifierType(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null aggregateId.identifierType must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.identifierType cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.identifierType is blank")
        void deserialize_ShouldThrowSerializationException_WhenIdentifierTypeIsBlank() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setIdentifierType("   ");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Blank aggregateId.identifierType must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.identifierType cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.identifierType is empty")
        void deserialize_ShouldThrowSerializationException_WhenIdentifierTypeIsEmpty() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setIdentifierType("");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Empty aggregateId.identifierType must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.identifierType cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.valueType is null")
        void deserialize_ShouldThrowSerializationException_WhenValueTypeIsNull() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValueType(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null aggregateId.valueType must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.valueType cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.valueType is blank")
        void deserialize_ShouldThrowSerializationException_WhenValueTypeIsBlank() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValueType("   ");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Blank aggregateId.valueType must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.valueType cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.valueType is empty")
        void deserialize_ShouldThrowSerializationException_WhenValueTypeIsEmpty() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValueType("");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Empty aggregateId.valueType must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.valueType cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.value is null")
        void deserialize_ShouldThrowSerializationException_WhenAggregateIdValueIsNull() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValue(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null aggregateId.value node must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.value cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.value is empty")
        void deserialize_ShouldThrowSerializationException_WhenAggregateIdValueIsEmpty() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValue(Map.of());
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Empty aggregateId.value node must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.value cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.value.kind is missing")
        void deserialize_ShouldThrowSerializationException_WhenValueKindIsMissing() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", String.class.getName());
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Missing aggregateId.value.kind must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.value.kind must be a string");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId.value.kind is unknown")
        void deserialize_ShouldThrowSerializationException_WhenValueKindIsUnknown() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "unknown");
            value.put("type", String.class.getName());
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Unknown aggregateId.value.kind must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Unknown aggregateId.value.kind");
        }

        @Test
        @DisplayName("Should throw SerializationException when scalar type is blank")
        void deserialize_ShouldThrowSerializationException_WhenScalarTypeIsBlank() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", "   ");
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Blank scalar type must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when scalar type is empty")
        void deserialize_ShouldThrowSerializationException_WhenScalarTypeIsEmpty() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", "");
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Empty scalar type must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when scalar type is null")
        void deserialize_ShouldThrowSerializationException_WhenScalarTypeIsNull() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", null);
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null scalar type must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }


        @Test
        @DisplayName("Should throw SerializationException when scalar type is not a string")
        void deserialize_ShouldThrowSerializationException_WhenScalarTypeIsNotString() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", 123);
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Non-string scalar type must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when scalar value is not a string")
        void deserialize_ShouldThrowSerializationException_WhenScalarValueIsNotString() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", String.class.getName());
            value.put("value", 42);
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Non-string scalar value must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar value must be a string");
        }

        @Test
        @DisplayName("Should throw SerializationException when composite type is blank")
        void deserialize_ShouldThrowSerializationException_WhenCompositeTypeIsBlank() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", "   ");
            value.put("fields", Map.of());
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Blank composite type must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when composite type is empty")
        void deserialize_ShouldThrowSerializationException_WhenCompositeTypeIsEmpty() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", "");
            value.put("fields", Map.of());
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Empty composite type must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when composite type is null")
        void deserialize_ShouldThrowSerializationException_WhenCompositeTypeIsNull() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", null);
            value.put("fields", Map.of());
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Null composite type must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }


        @Test
        @DisplayName("Should throw SerializationException when composite type is not a string")
        void deserialize_ShouldThrowSerializationException_WhenCompositeTypeIsNotString() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", 123);
            value.put("fields", Map.of());
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Non-string composite type must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @Test
        @DisplayName("Should throw SerializationException when composite fields is not a map")
        void deserialize_ShouldThrowSerializationException_WhenCompositeFieldsIsNotMap() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", CompositeIdValue.class.getName());
            value.put("fields", "not-a-map");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Non-map composite fields must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite fields must be a map");
        }

        @Test
        @DisplayName("Should throw SerializationException when composite field value is not a map")
        void deserialize_ShouldThrowSerializationException_WhenCompositeFieldValueIsNotMap() {
            // given
            EventType eventType = EventType.of("SampleCreated", "sample", "1");
            DomainEventDescriptor descriptor = mockDescriptor(eventType, CompositeSampleEvent.class,
                    SamplePayload.class, CompositeSampleIdentifier.class);
            doReturn(descriptor).when(resolver).resolve(eventType);
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("partA", "not-a-node");
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", CompositeIdValue.class.getName());
            value.put("fields", fields);
            envelope.getAggregateId().setIdentifierType(CompositeSampleIdentifier.class.getName());
            envelope.getAggregateId().setValueType(CompositeIdValue.class.getName());
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, CompositeSampleEvent.class))
                    .as("Non-map composite field entry must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("must be a value node map");
        }

        @Test
        @DisplayName("Should throw SerializationException when payload cannot be converted")
        void deserialize_ShouldThrowSerializationException_WhenPayloadConversionFails() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            ObjectNode badPayload = objectMapper.createObjectNode();
            badPayload.put("name", "widget");
            badPayload.put("quantity", "not-an-int");
            envelope.setPayload(badPayload);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Invalid payload conversion must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Failed to convert payload to type");
        }

        @Test
        @DisplayName("Should throw SerializationException when aggregateId decoding fails")
        void deserialize_ShouldThrowSerializationException_WhenAggregateIdDecodingFails() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setIdentifierType("com.example.UnknownIdentifier");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Failed aggregateId decoding must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Failed to decode aggregateId");
        }

        @Test
        @DisplayName("Should throw SerializationException when required eventId field is missing")
        void deserialize_ShouldThrowSerializationException_WhenEventIdIsMissing() {
            // given
            String incompleteJson = """
                    {
                      "formatVersion": 1,
                      "occurredAt": "2026-01-15T10:30:00Z",
                      "eventType": { "name": "SampleCreated", "category": "sample", "version": "1" },
                      "aggregateId": {
                        "identifierType": "%s",
                        "valueType": "java.lang.String",
                        "value": { "kind": "scalar", "type": "java.lang.String", "value": "agg-1" }
                      },
                      "payload": { "name": "widget", "quantity": 1 }
                    }
                    """.formatted(SampleIdentifier.class.getName());
            byte[] data = incompleteJson.getBytes(StandardCharsets.UTF_8);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Missing mandatory eventId field must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("eventId");
        }

        @Test
        @DisplayName("Should throw SerializationException when JSON body is truncated")
        void deserialize_ShouldThrowSerializationException_WhenJsonIsIncomplete() {
            // given
            byte[] data = "{\"formatVersion\":1,\"eventId\":\"evt-1\"".getBytes(StandardCharsets.UTF_8);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Truncated or incomplete JSON input must be rejected")
                    .isInstanceOf(SerializationException.class);
        }

        @Test
        @DisplayName("Should throw SerializationException when formatVersion is not supported")
        void deserialize_ShouldThrowSerializationException_WhenFormatVersionIsInvalid() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setFormatVersion(99);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("Unsupported envelope formatVersion must be rejected")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("formatVersion");
        }
    }

    @Nested
    @DisplayName("supportedFormat()")
    class SupportedFormat {

        @Test
        @DisplayName("Should return application/json format identifier")
        void supportedFormat_ShouldReturnApplicationJson_WhenCalled() {
            // given

            // when
            String format = deserializer.supportedFormat();

            // then
            then(format)
                    .as("Format identifier returned by supportedFormat() must be application/json")
                    .isEqualTo("application/json");
            then(format)
                    .as("Format identifier must match the FORMAT constant")
                    .isEqualTo(JacksonEventDeserializer.FORMAT);
        }
    }

    @Nested
    @DisplayName("roundTrip")
    class RoundTripTests {

        private final JacksonEventSerializer serializer = new JacksonEventSerializer(objectMapper);

        @Test
        @DisplayName("Should preserve identity and payload when metadata is empty")
        void roundTrip_ShouldPreserveIdentityAndPayload_WhenMetadataIsEmpty() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            SampleEvent original = new SampleEvent(
                    "evt-empty-meta",
                    Instant.parse("2026-03-01T12:00:00Z"),
                    new SampleIdentifier("agg-100"),
                    new SamplePayload("widget", 7),
                    EventType.of("SampleCreated", "sample", "1"),
                    EventMetadata.empty());

            // when
            byte[] bytes = serializer.serialize(original);
            SampleEvent restored = deserializer.deserialize(bytes, SampleEvent.class);

            // then
            then(restored.getEventId())
                    .as("eventId must survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getEventId());
            then(restored.getOccurredAt())
                    .as("occurredAt must survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getOccurredAt());
            then(restored.getEventType())
                    .as("eventType coordinates must survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getEventType());
            then(restored.getAggregateId())
                    .as("aggregateId must survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getAggregateId());
            then(restored.getPayload())
                    .as("payload must survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getPayload());
            then(restored.getMetadata())
                    .as("empty metadata must remain empty after a full round-trip")
                    .isEqualTo(EventMetadata.empty());
        }

        @Test
        @DisplayName("Should preserve full metadata including custom properties")
        void roundTrip_ShouldPreserveFullMetadata_WhenMetadataIsPopulated() {
            // given
            stubResolverFor(SampleEvent.class, SamplePayload.class);
            EventMetadata metadata = EventMetadata.builder()
                    .correlationId("corr-rt")
                    .causationId("cause-rt")
                    .tenant("tenant-rt")
                    .traceId("trace-rt")
                    .spanId("span-rt")
                    .userId("user-rt")
                    .properties("channel", "web")
                    .properties("priority", 2)
                    .build();
            SampleEvent original = new SampleEvent(
                    "evt-full-meta",
                    Instant.parse("2026-03-02T08:15:30Z"),
                    new SampleIdentifier("agg-200"),
                    new SamplePayload("gadget", 4),
                    EventType.of("SampleCreated", "sample", "1"),
                    metadata);

            // when
            byte[] bytes = serializer.serialize(original);
            SampleEvent restored = deserializer.deserialize(bytes, SampleEvent.class);

            // then
            then(restored.getEventId())
                    .as("eventId must survive round-trip with populated metadata")
                    .isEqualTo(original.getEventId());
            then(restored.getMetadata().getCorrelationId())
                    .as("correlationId must survive round-trip")
                    .isEqualTo("corr-rt");
            then(restored.getMetadata().getCausationId())
                    .as("causationId must survive round-trip")
                    .isEqualTo("cause-rt");
            then(restored.getMetadata().getTenant())
                    .as("tenant must survive round-trip")
                    .isEqualTo("tenant-rt");
            then(restored.getMetadata().getTraceId())
                    .as("traceId must survive round-trip")
                    .isEqualTo("trace-rt");
            then(restored.getMetadata().getSpanId())
                    .as("spanId must survive round-trip")
                    .isEqualTo("span-rt");
            then(restored.getMetadata().getUserId())
                    .as("userId must survive round-trip")
                    .isEqualTo("user-rt");
            then(restored.getMetadata().getProperty("channel"))
                    .as("Custom metadata property 'channel' must survive round-trip")
                    .isEqualTo("web");
            then(restored.getMetadata().getProperty("priority"))
                    .as("Custom metadata property 'priority' must survive round-trip")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("Should preserve composite aggregate id through serialize and deserialize")
        void roundTrip_ShouldPreserveCompositeAggregateId_WhenIdentifierValueIsComposite() {
            // given
            EventType eventType = EventType.of("SampleCreated", "sample", "1");
            DomainEventDescriptor descriptor = mockDescriptor(eventType, CompositeSampleEvent.class,
                    SamplePayload.class, CompositeSampleIdentifier.class);
            doReturn(descriptor).when(resolver).resolve(eventType);
            CompositeIdValue compositeValue = new CompositeIdValue("part-a", "part-b");
            CompositeSampleIdentifier aggregateId = new CompositeSampleIdentifier(compositeValue);
            CompositeSampleEvent original = new CompositeSampleEvent(
                    "evt-composite",
                    Instant.parse("2026-04-10T09:00:00Z"),
                    aggregateId,
                    new SamplePayload("composite-item", 1),
                    EventType.of("SampleCreated", "sample", "1"),
                    EventMetadata.empty());

            // when
            byte[] bytes = serializer.serialize(original);
            CompositeSampleEvent restored = deserializer.deserialize(bytes, CompositeSampleEvent.class);

            // then
            then(restored.getAggregateId())
                    .as("Composite aggregate id instance must be restored after round-trip")
                    .isEqualTo(aggregateId);
            then(restored.getAggregateId().value().partA())
                    .as("Composite value partA must survive round-trip")
                    .isEqualTo("part-a");
            then(restored.getAggregateId().value().partB())
                    .as("Composite value partB must survive round-trip")
                    .isEqualTo("part-b");
            then(restored.getEventId())
                    .as("eventId must survive composite-id round-trip")
                    .isEqualTo("evt-composite");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers and test doubles
    // -------------------------------------------------------------------------

    private DomainEventDescriptor mockDescriptor(EventType eventType,
                                                 Class<? extends DomainEvent<?, ?>> eventClass,
                                                 Class<?> payloadType,
                                                 Class<? extends Identifier<?>> identifierType) {
        DomainEventDescriptor descriptor = mock(DomainEventDescriptor.class);
        doReturn(eventType).when(descriptor).eventType();
        doReturn(eventClass).when(descriptor).domainEvent();
        doReturn(payloadType).when(descriptor).payloadType();
        doReturn(identifierType).when(descriptor).identifierType();
        return descriptor;
    }

    private EventEnvelope validEnvelope() {
        EventEnvelope envelope = new EventEnvelope();
        envelope.setFormatVersion(EventEnvelope.CURRENT_FORMAT_VERSION);
        envelope.setEventId("evt-1");
        envelope.setOccurredAt("2026-01-15T10:30:00Z");

        EventEnvelope.EventTypeNode eventType = new EventEnvelope.EventTypeNode();
        eventType.setName("SampleCreated");
        eventType.setCategory("sample");
        eventType.setVersion("1");
        envelope.setEventType(eventType);

        EventEnvelope.AggregateIdNode aggregateId = new EventEnvelope.AggregateIdNode();
        aggregateId.setIdentifierType(SampleIdentifier.class.getName());
        aggregateId.setValueType(String.class.getName());
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("kind", "scalar");
        value.put("type", String.class.getName());
        value.put("value", "agg-1");
        aggregateId.setValue(value);
        envelope.setAggregateId(aggregateId);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("name", "widget");
        payload.put("quantity", 3);
        envelope.setPayload(payload);

        EventEnvelope.MetadataNode metadata = new EventEnvelope.MetadataNode();
        metadata.setCorrelationId("corr-1");
        metadata.setCausationId("cause-1");
        metadata.setTenant("tenant-1");
        metadata.setTraceId("trace-1");
        metadata.setSpanId("span-1");
        metadata.setUserId("user-1");
        metadata.setProperties(Map.of("extra", "extra-value"));
        envelope.setMetadata(metadata);

        return envelope;
    }

    private Map<String, Object> compositeValueNode() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("partA", scalarValueNode(String.class.getName(), "part-a"));
        fields.put("partB", scalarValueNode(String.class.getName(), "part-b"));
        Map<String, Object> compositeValue = new LinkedHashMap<>();
        compositeValue.put("kind", "composite");
        compositeValue.put("type", CompositeIdValue.class.getName());
        compositeValue.put("fields", fields);
        return compositeValue;
    }

    private Map<String, Object> scalarValueNode(String type, String value) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("kind", "scalar");
        node.put("type", type);
        node.put("value", value);
        return node;
    }

    @SneakyThrows
    private byte[] toJsonBytes(EventEnvelope envelope) {
        return objectMapper.writeValueAsBytes(envelope);
    }

    public record SampleIdentifier(String value) implements Identifier<String> {
    }

    public record SamplePayload(String name, int quantity) {
    }

    public static final class SampleEvent extends AbstractDomainEvent<SampleIdentifier, SamplePayload> {

        public SampleEvent(String eventId,
                           Instant occurredAt,
                           SampleIdentifier aggregateId,
                           SamplePayload payload,
                           EventType eventType,
                           EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    public record CompositeIdValue(String partA, String partB) implements Serializable {
    }

    public record CompositeSampleIdentifier(CompositeIdValue value) implements Identifier<CompositeIdValue> {
    }

    public static final class CompositeSampleEvent
            extends AbstractDomainEvent<CompositeSampleIdentifier, SamplePayload> {

        public CompositeSampleEvent(String eventId,
                                    Instant occurredAt,
                                    CompositeSampleIdentifier aggregateId,
                                    SamplePayload payload,
                                    EventType eventType,
                                    EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    public abstract static class NonGenericIntermediate
            extends AbstractDomainEvent<SampleIdentifier, SamplePayload> {

        protected NonGenericIntermediate(String eventId,
                                         Instant occurredAt,
                                         SampleIdentifier aggregateId,
                                         SamplePayload payload,
                                         EventType eventType,
                                         EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    public static final class MultiLevelEvent extends NonGenericIntermediate {

        public MultiLevelEvent(String eventId,
                               Instant occurredAt,
                               SampleIdentifier aggregateId,
                               SamplePayload payload,
                               EventType eventType,
                               EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    public static final class MapPayloadEvent
            extends AbstractDomainEvent<SampleIdentifier, Map<String, String>> {

        public MapPayloadEvent(String eventId,
                               Instant occurredAt,
                               SampleIdentifier aggregateId,
                               Map<String, String> payload,
                               EventType eventType,
                               EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    private static final class NonAbstractEvent implements DomainEvent<SampleIdentifier, SamplePayload> {

        @Override
        public String getEventId() {
            return "x";
        }

        @Override
        public EventType getEventType() {
            return EventType.of("X");
        }

        @Override
        public Instant getOccurredAt() {
            return Instant.EPOCH;
        }

        @Override
        public SampleIdentifier getAggregateId() {
            return new SampleIdentifier("x");
        }

        @Override
        public SamplePayload getPayload() {
            return new SamplePayload("x", 0);
        }
    }

    private void stubResolverFor(Class<? extends DomainEvent<?, ?>> eventClass, Class<?> payloadType) {
        EventType eventType = EventType.of("SampleCreated", "sample", "1");
        DomainEventDescriptor descriptor = mockDescriptor(eventType, eventClass, payloadType, SampleIdentifier.class);
        doReturn(descriptor).when(resolver).resolve(eventType);
    }
}
