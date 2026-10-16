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

import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventMetadata;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.event.SerializationException;
import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.infrastructure.adapter.driven.event.serializer.JacksonEventSerializer;
import ir.artanpg.boot.infrastructure.adapter.driven.event.support.EventEnvelope;
import lombok.SneakyThrows;
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

/**
 * Unit tests for {@link JacksonEventDeserializer}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("JacksonEventDeserializer")
class JacksonEventDeserializerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JacksonEventDeserializer deserializer = new JacksonEventDeserializer(objectMapper);

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Creates deserializer with given ObjectMapper")
        void Constructor_ShouldCreateInstance_WhenObjectMapperProvided() {
            // given
            ObjectMapper mapper = new ObjectMapper();

            // when
            JacksonEventDeserializer result = new JacksonEventDeserializer(mapper);

            // then
            then(result)
                    .as("deserializer instance created from a non-null ObjectMapper")
                    .isNotNull();
            then(result.supportedFormat())
                    .as("supportedFormat() returned by the newly created deserializer")
                    .isEqualTo(JacksonEventDeserializer.FORMAT);
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws SerializationException when ObjectMapper is null")
        void Constructor_ShouldThrowSerializationException_WhenObjectMapperIsNull() {
            // given

            // when & then
            thenThrownBy(() -> new JacksonEventDeserializer(null))
                    .as("exception thrown when constructing the deserializer with a null ObjectMapper")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The ObjectMapper cannot be null");
        }
    }

    @Nested
    @DisplayName("deserialize(byte[], Class)")
    class Deserialize {

        @Test
        @DisplayName("Deserializes a valid envelope into the target event type")
        void deserialize_ShouldReturnEvent_WhenEnvelopeIsValid() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when
            SampleEvent event = deserializer.deserialize(data, SampleEvent.class);

            // then
            then(event)
                    .as("deserialized event instance returned from a valid JSON envelope")
                    .isNotNull();
            then(event.getEventId())
                    .as("eventId restored from the envelope eventId field")
                    .isEqualTo("evt-1");
            then(event.getOccurredAt())
                    .as("occurredAt parsed back to an Instant from the ISO-8601 envelope value")
                    .isEqualTo(Instant.parse("2026-01-15T10:30:00Z"));
            then(event.getEventType())
                    .as("eventType reconstructed from the envelope eventType node")
                    .isEqualTo(EventType.of("SampleCreated", "sample", "1"));
            then(event.getAggregateId().value())
                    .as("aggregate identifier decoded from the envelope aggregateId scalar value")
                    .isEqualTo("agg-1");
            then(event.getPayload().name())
                    .as("payload.name converted into the target payload record")
                    .isEqualTo("widget");
            then(event.getPayload().quantity())
                    .as("payload.quantity converted into the target payload record")
                    .isEqualTo(3);
            then(event.getMetadata().getCorrelationId())
                    .as("metadata.correlationId mapped into EventMetadata")
                    .isEqualTo("corr-1");
            then(event.getMetadata().getCausationId())
                    .as("metadata.causationId mapped into EventMetadata")
                    .isEqualTo("cause-1");
            then(event.getMetadata().getTenant())
                    .as("metadata.tenant mapped into EventMetadata")
                    .isEqualTo("tenant-1");
            then(event.getMetadata().getTraceId())
                    .as("metadata.traceId mapped into EventMetadata")
                    .isEqualTo("trace-1");
            then(event.getMetadata().getSpanId())
                    .as("metadata.spanId mapped into EventMetadata")
                    .isEqualTo("span-1");
            then(event.getMetadata().getUserId())
                    .as("metadata.userId mapped into EventMetadata")
                    .isEqualTo("user-1");
            then(event.getMetadata().getProperty("extra"))
                    .as("custom metadata property 'extra' preserved on EventMetadata")
                    .isEqualTo("extra-value");
        }

        @Test
        @DisplayName("Deserializes envelope with null metadata as empty metadata")
        void deserialize_ShouldUseEmptyMetadata_WhenMetadataNodeIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setMetadata(null);
            byte[] data = toJsonBytes(envelope);

            // when
            SampleEvent event = deserializer.deserialize(data, SampleEvent.class);

            // then
            then(event.getMetadata())
                    .as("EventMetadata falls back to EventMetadata.empty() when the envelope metadata node is absent")
                    .isEqualTo(EventMetadata.empty());
        }

        @Test
        @DisplayName("Deserializes envelope with empty metadata properties")
        void deserialize_ShouldIgnoreEmptyProperties_WhenPropertiesMapIsEmpty() {
            // given
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
                    .as("correlationId preserved even when the properties map is empty")
                    .isEqualTo("corr-only");
            then(event.getMetadata().getProperties())
                    .as("EventMetadata properties remain empty when the envelope supplied an empty map")
                    .isEmpty();
        }

        @Test
        @DisplayName("Deserializes envelope when metadata properties is null")
        void deserialize_ShouldIgnoreNullProperties_WhenPropertiesMapIsNull() {
            // given
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
                    .as("correlationId preserved even when the properties map is null")
                    .isEqualTo("corr-null-props");
            then(event.getMetadata().getProperties())
                    .as("EventMetadata properties remain empty when the envelope supplied a null map")
                    .isEmpty();
        }

        @Test
        @DisplayName("Deserializes aggregate id with composite value node")
        void deserialize_ShouldDecodeCompositeAggregateId_WhenValueIsComposite() {
            // given
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
                    .as("composite aggregate id 'partA' reconstructed from its nested scalar node")
                    .isEqualTo("part-a");
            then(event.getAggregateId().value().partB())
                    .as("composite aggregate id 'partB' reconstructed from its nested scalar node")
                    .isEqualTo("part-b");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws SerializationException when data is null")
        void deserialize_ShouldThrowSerializationException_WhenDataIsNull() {
            // given

            // when & then
            thenThrownBy(() -> deserializer.deserialize(null, SampleEvent.class))
                    .as("exception thrown when the serialized data array is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The serialized data cannot be null or empty");
        }

        @Test
        @DisplayName("Throws SerializationException when data is empty")
        void deserialize_ShouldThrowSerializationException_WhenDataIsEmpty() {
            // given
            byte[] data = new byte[0];

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when the serialized data array is empty")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The serialized data cannot be null or empty");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws SerializationException when target type is null")
        void deserialize_ShouldThrowSerializationException_WhenTypeIsNull() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, null))
                    .as("exception thrown when the target event type is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The target event type cannot be null");
        }

        @Test
        @DisplayName("Throws SerializationException when type does not extend AbstractDomainEvent")
        void deserialize_ShouldThrowSerializationException_WhenTypeDoesNotExtendAbstractDomainEvent() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, NonAbstractEvent.class))
                    .as("exception thrown when the target type does not extend AbstractDomainEvent")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("The target event type must extend AbstractDomainEvent");
        }

        @Test
        @DisplayName("Throws SerializationException when AbstractDomainEvent generic type cannot be resolved")
        void deserialize_ShouldThrowSerializationException_WhenAbstractDomainEventGenericTypeCannotBeResolved() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, RawEvent.class))
                    .as("exception thrown when AbstractDomainEvent generic type cannot be resolved from a raw subclass")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Cannot resolve AbstractDomainEvent generic type from event class");
        }

        @Test
        @DisplayName("Deserializes event when AbstractDomainEvent is reached via non-generic intermediate")
        void deserialize_ShouldReturnEvent_WhenAbstractDomainEventFoundViaIntermediateClass() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when
            MultiLevelEvent event = deserializer.deserialize(data, MultiLevelEvent.class);

            // then
            then(event)
                    .as("deserialized event resolved via a non-generic intermediate superclass")
                    .isNotNull();
            then(event.getEventId())
                    .as("eventId restored when AbstractDomainEvent is reached through an intermediate class")
                    .isEqualTo("evt-1");
            then(event.getAggregateId().value())
                    .as("aggregate id restored when AbstractDomainEvent is reached through an intermediate class")
                    .isEqualTo("agg-1");
            then(event.getPayload().name())
                    .as("payload restored when AbstractDomainEvent is reached through an intermediate class")
                    .isEqualTo("widget");
        }

        @Test
        @DisplayName("Deserializes event when payload type is a parameterized type")
        void deserialize_ShouldReturnEvent_WhenPayloadTypeIsParameterizedType() {
            // given
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
                    .as("deserialized event whose payload type is a ParameterizedType (Map<String, String>)")
                    .isNotNull();
            then(event.getPayload())
                    .as("payload map converted from JSON object to Map<String, String>")
                    .containsEntry("key1", "value1");
            then(event.getPayload())
                    .as("payload map contains every expected JSON entry")
                    .containsEntry("key2", "value2");
        }

        @Test
        @DisplayName("Throws SerializationException when payload type argument is unsupported")
        void deserialize_ShouldThrowSerializationException_WhenPayloadTypeArgumentIsUnsupported() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, OpenPayloadEvent.class))
                    .as("exception thrown when the payload type argument is a TypeVariable rather than a concrete" +
                            " class")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Unsupported payload type argument");
        }

        @Test
        @DisplayName("Throws SerializationException when payload type cannot be resolved")
        void deserialize_ShouldThrowSerializationException_WhenPayloadTypeCannotBeResolved() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SingleArgEvent.class))
                    .as("exception thrown when only one type argument is available to resolve the payload type")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Cannot resolve payload type from event class");
        }

        @Test
        @DisplayName("Throws SerializationException when resolved id type does not implement Identifier")
        void deserialize_ShouldThrowSerializationException_WhenResolvedIdTypeDoesNotImplementIdentifier() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SwappedTypeArgsEvent.class))
                    .as("exception thrown when the resolved id type argument does not implement Identifier")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Resolved aggregate id type does not implement Identifier");
        }

        @Test
        @DisplayName("Throws SerializationException when JSON is malformed")
        void deserialize_ShouldThrowSerializationException_WhenJsonIsMalformed() {
            // given
            byte[] data = "{not-json".getBytes(StandardCharsets.UTF_8);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when the input bytes are not a valid JSON document")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Failed to deserialize event to type");
        }

        @Test
        @DisplayName("Throws SerializationException when envelope is null")
        void deserialize_ShouldThrowSerializationException_WhenEnvelopeIsNull() {
            // given
            byte[] data = "null".getBytes(StandardCharsets.UTF_8);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when the JSON document is a top-level null literal")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The event envelope cannot be null");
        }

        @Test
        @DisplayName("Throws SerializationException when formatVersion is unsupported")
        void deserialize_ShouldThrowSerializationException_WhenFormatVersionUnsupported() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setFormatVersion(99);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when formatVersion does not match CURRENT_FORMAT_VERSION")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Unsupported event envelope formatVersion");
        }

        @Test
        @DisplayName("Throws SerializationException when eventId is null")
        void deserialize_ShouldThrowSerializationException_WhenEventIdIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setEventId(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope eventId is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when eventId is blank")
        void deserialize_ShouldThrowSerializationException_WhenEventIdIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setEventId("   ");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope eventId is blank (whitespace only)")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when eventId is empty string")
        void deserialize_ShouldThrowSerializationException_WhenEventIdIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setEventId("");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope eventId is an empty string")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when occurredAt is null")
        void deserialize_ShouldThrowSerializationException_WhenOccurredAtIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setOccurredAt(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope occurredAt is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope occurredAt cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when occurredAt is blank")
        void deserialize_ShouldThrowSerializationException_WhenOccurredAtIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setOccurredAt("   ");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope occurredAt is blank (whitespace only)")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope occurredAt cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when occurredAt is empty string")
        void deserialize_ShouldThrowSerializationException_WhenOccurredAtIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setOccurredAt("");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope occurredAt is an empty string")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope occurredAt cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when eventType is null")
        void deserialize_ShouldThrowSerializationException_WhenEventTypeIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setEventType(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope eventType node is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventType.name cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when eventType.name is null")
        void deserialize_ShouldThrowSerializationException_WhenEventTypeNameIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            EventEnvelope.EventTypeNode eventType = new EventEnvelope.EventTypeNode();
            eventType.setName(null);
            envelope.setEventType(eventType);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope eventType.name is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventType.name cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when eventType.name is blank")
        void deserialize_ShouldThrowSerializationException_WhenEventTypeNameIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            EventEnvelope.EventTypeNode eventType = new EventEnvelope.EventTypeNode();
            eventType.setName("   ");
            envelope.setEventType(eventType);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope eventType.name is blank (whitespace only)")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventType.name cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when eventType.name is empty string")
        void deserialize_ShouldThrowSerializationException_WhenEventTypeNameIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            EventEnvelope.EventTypeNode eventType = new EventEnvelope.EventTypeNode();
            eventType.setName("");
            envelope.setEventType(eventType);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope eventType.name is an empty string")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope eventType.name cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId is null")
        void deserialize_ShouldThrowSerializationException_WhenAggregateIdIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setAggregateId(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope aggregateId node is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope aggregateId cannot be null");
        }

        @Test
        @DisplayName("Throws SerializationException when validateEnvelope sees null payload via reflection")
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
                    .as("exception thrown when validateEnvelope is invoked with a null payload field")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope payload cannot be null");
        }

        @Test
        @DisplayName("Throws SerializationException when payload is JSON null node")
        void deserialize_ShouldThrowSerializationException_WhenPayloadIsJsonNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setPayload(NullNode.getInstance());
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when envelope payload is an in-memory NullNode instance")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope payload cannot be null");
        }

        @Test
        @DisplayName("Throws SerializationException when payload is explicit JSON null in wire data")
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
                    .as("exception thrown when envelope payload is an explicit JSON null literal in the wire data")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The envelope payload cannot be null");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.identifierType is null")
        void deserialize_ShouldThrowSerializationException_WhenIdentifierTypeIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setIdentifierType(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.identifierType is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.identifierType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.identifierType is blank")
        void deserialize_ShouldThrowSerializationException_WhenIdentifierTypeIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setIdentifierType("   ");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.identifierType is blank (whitespace only)")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.identifierType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.identifierType is empty")
        void deserialize_ShouldThrowSerializationException_WhenIdentifierTypeIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setIdentifierType("");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.identifierType is an empty string")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.identifierType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.valueType is null")
        void deserialize_ShouldThrowSerializationException_WhenValueTypeIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValueType(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.valueType is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.valueType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.valueType is blank")
        void deserialize_ShouldThrowSerializationException_WhenValueTypeIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValueType("   ");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.valueType is blank (whitespace only)")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.valueType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.valueType is empty")
        void deserialize_ShouldThrowSerializationException_WhenValueTypeIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValueType("");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.valueType is an empty string")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.valueType cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.value is null")
        void deserialize_ShouldThrowSerializationException_WhenAggregateIdValueIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValue(null);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.value node is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.value cannot be null or empty");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.value is empty")
        void deserialize_ShouldThrowSerializationException_WhenAggregateIdValueIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setValue(Map.of());
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.value node is an empty map")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.value cannot be null or empty");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.value.kind is missing")
        void deserialize_ShouldThrowSerializationException_WhenValueKindIsMissing() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", String.class.getName());
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.value.kind key is missing from the value node")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The aggregateId.value.kind must be a string");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId.value.kind is unknown")
        void deserialize_ShouldThrowSerializationException_WhenValueKindIsUnknown() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "unknown");
            value.put("type", String.class.getName());
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when aggregateId.value.kind carries an unrecognized discriminator")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Unknown aggregateId.value.kind");
        }

        @Test
        @DisplayName("Throws SerializationException when scalar type is blank")
        void deserialize_ShouldThrowSerializationException_WhenScalarTypeIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", "   ");
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when scalar value node 'type' is blank (whitespace only)")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when scalar type is empty")
        void deserialize_ShouldThrowSerializationException_WhenScalarTypeIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", "");
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when scalar value node 'type' is an empty string")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when scalar type is null")
        void deserialize_ShouldThrowSerializationException_WhenScalarTypeIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", null);
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when scalar value node 'type' is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when scalar type is not a string")
        void deserialize_ShouldThrowSerializationException_WhenScalarTypeIsNotString() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", 123);
            value.put("value", "agg-1");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when scalar value node 'type' is a non-string JSON value")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when scalar value is not a string")
        void deserialize_ShouldThrowSerializationException_WhenScalarValueIsNotString() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "scalar");
            value.put("type", String.class.getName());
            value.put("value", 42);
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when scalar value node 'value' is not a string")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The scalar value must be a string");
        }

        @Test
        @DisplayName("Throws SerializationException when composite type is blank")
        void deserialize_ShouldThrowSerializationException_WhenCompositeTypeIsBlank() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", "   ");
            value.put("fields", Map.of());
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when composite value node 'type' is blank (whitespace only)")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when composite type is empty")
        void deserialize_ShouldThrowSerializationException_WhenCompositeTypeIsEmpty() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", "");
            value.put("fields", Map.of());
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when composite value node 'type' is an empty string")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when composite type is null")
        void deserialize_ShouldThrowSerializationException_WhenCompositeTypeIsNull() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", null);
            value.put("fields", Map.of());
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when composite value node 'type' is null")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when composite type is not a string")
        void deserialize_ShouldThrowSerializationException_WhenCompositeTypeIsNotString() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", 123);
            value.put("fields", Map.of());
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when composite value node 'type' is a non-string JSON value")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite type cannot be null or blank");
        }

        @Test
        @DisplayName("Throws SerializationException when composite fields is not a map")
        void deserialize_ShouldThrowSerializationException_WhenCompositeFieldsIsNotMap() {
            // given
            EventEnvelope envelope = validEnvelope();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("kind", "composite");
            value.put("type", CompositeIdValue.class.getName());
            value.put("fields", "not-a-map");
            envelope.getAggregateId().setValue(value);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when composite value node 'fields' is not a JSON object")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The composite fields must be a map");
        }

        @Test
        @DisplayName("Throws SerializationException when composite field value is not a map")
        void deserialize_ShouldThrowSerializationException_WhenCompositeFieldValueIsNotMap() {
            // given
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
                    .as("exception thrown when a composite field entry is not a nested value node map")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("must be a value node map");
        }

        @Test
        @DisplayName("Throws SerializationException when payload cannot be converted")
        void deserialize_ShouldThrowSerializationException_WhenPayloadConversionFails() {
            // given
            EventEnvelope envelope = validEnvelope();
            ObjectNode badPayload = objectMapper.createObjectNode();
            badPayload.put("name", "widget");
            badPayload.put("quantity", "not-an-int");
            envelope.setPayload(badPayload);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when payload conversion fails because 'quantity' is not an int")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Failed to convert payload to type");
        }

        @Test
        @DisplayName("Throws SerializationException when aggregateId decoding fails")
        void deserialize_ShouldThrowSerializationException_WhenAggregateIdDecodingFails() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.getAggregateId().setIdentifierType("com.example.UnknownIdentifier");
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("exception thrown when IdentifierCodec cannot resolve the declared identifier type")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Failed to decode aggregateId");
        }

        @Test
        @DisplayName("Throws SerializationException when no parameterized AbstractDomainEvent found")
        void deserialize_ShouldThrowSerializationException_WhenNoParameterizedAbstractDomainEventFound() {
            // given
            byte[] data = toJsonBytes(validEnvelope());

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, RawEvent.class))
                    .as("exception thrown when no parameterized AbstractDomainEvent found in hierarchy")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Cannot resolve AbstractDomainEvent generic type from event class");
        }

        @Test
        @DisplayName("Throws SerializationException when required eventId field is missing")
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
                    .as("deserialize must reject an envelope that omits the mandatory eventId field")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("eventId");
        }

        @Test
        @DisplayName("Throws SerializationException when JSON body is truncated")
        void deserialize_ShouldThrowSerializationException_WhenJsonIsIncomplete() {
            // given
            byte[] data = "{\"formatVersion\":1,\"eventId\":\"evt-1\"".getBytes(StandardCharsets.UTF_8);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("deserialize must reject truncated or incomplete JSON input")
                    .isInstanceOf(SerializationException.class);
        }

        @Test
        @DisplayName("Throws SerializationException when formatVersion is not supported")
        void deserialize_ShouldThrowSerializationException_WhenFormatVersionIsInvalid() {
            // given
            EventEnvelope envelope = validEnvelope();
            envelope.setFormatVersion(99);
            byte[] data = toJsonBytes(envelope);

            // when & then
            thenThrownBy(() -> deserializer.deserialize(data, SampleEvent.class))
                    .as("deserialize must reject an unsupported envelope formatVersion")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("formatVersion");
        }
    }

    @Nested
    @DisplayName("supportedFormat()")
    class SupportedFormat {

        @Test
        @DisplayName("Returns application/json format identifier")
        void SupportedFormat_ShouldReturnApplicationJson_WhenCalled() {
            // given

            // when
            String format = deserializer.supportedFormat();

            // then
            then(format)
                    .as("format identifier returned by supportedFormat()")
                    .isEqualTo("application/json");
            then(format)
                    .as("format identifier matches the JacksonEventDeserializer.FORMAT constant")
                    .isEqualTo(JacksonEventDeserializer.FORMAT);
        }
    }

    @Nested
    @DisplayName("roundTrip")
    class RoundTripTests {

        private final JacksonEventSerializer serializer = new JacksonEventSerializer(objectMapper);

        @Test
        @DisplayName("Preserves identity and payload when metadata is empty")
        void roundTrip_ShouldPreserveIdentityAndPayload_WhenMetadataIsEmpty() {
            // given
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
                    .as("eventId should survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getEventId());
            then(restored.getOccurredAt())
                    .as("occurredAt should survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getOccurredAt());
            then(restored.getEventType())
                    .as("eventType coordinates should survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getEventType());
            then(restored.getAggregateId())
                    .as("aggregateId should survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getAggregateId());
            then(restored.getPayload())
                    .as("payload should survive a full serialize/deserialize round-trip")
                    .isEqualTo(original.getPayload());
            then(restored.getMetadata())
                    .as("empty metadata should remain empty after a full round-trip")
                    .isEqualTo(EventMetadata.empty());
        }

        @Test
        @DisplayName("Preserves full metadata including custom properties")
        void roundTrip_ShouldPreserveFullMetadata_WhenMetadataIsPopulated() {
            // given
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
                    .as("eventId should survive round-trip with populated metadata")
                    .isEqualTo(original.getEventId());
            then(restored.getMetadata().getCorrelationId())
                    .as("correlationId should survive round-trip")
                    .isEqualTo("corr-rt");
            then(restored.getMetadata().getCausationId())
                    .as("causationId should survive round-trip")
                    .isEqualTo("cause-rt");
            then(restored.getMetadata().getTenant())
                    .as("tenant should survive round-trip")
                    .isEqualTo("tenant-rt");
            then(restored.getMetadata().getTraceId())
                    .as("traceId should survive round-trip")
                    .isEqualTo("trace-rt");
            then(restored.getMetadata().getSpanId())
                    .as("spanId should survive round-trip")
                    .isEqualTo("span-rt");
            then(restored.getMetadata().getUserId())
                    .as("userId should survive round-trip")
                    .isEqualTo("user-rt");
            then(restored.getMetadata().getProperty("channel"))
                    .as("custom metadata property 'channel' should survive round-trip")
                    .isEqualTo("web");
            then(restored.getMetadata().getProperty("priority"))
                    .as("custom metadata property 'priority' should survive round-trip")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("Preserves composite aggregate id through serialize and deserialize")
        void roundTrip_ShouldPreserveCompositeAggregateId_WhenIdentifierValueIsComposite() {
            // given
            CompositeIdValue compositeValue = new CompositeIdValue("part-a", "part-b");
            CompositeSampleIdentifier aggregateId = new CompositeSampleIdentifier(compositeValue);
            CompositeSampleEvent original = new CompositeSampleEvent(
                    "evt-composite",
                    Instant.parse("2026-04-10T09:00:00Z"),
                    aggregateId,
                    new SamplePayload("composite-item", 1),
                    EventType.of("CompositeCreated", "sample", "1"),
                    EventMetadata.empty());

            // when
            byte[] bytes = serializer.serialize(original);
            CompositeSampleEvent restored = deserializer.deserialize(bytes, CompositeSampleEvent.class);

            // then
            then(restored.getAggregateId())
                    .as("composite aggregate id instance should be restored after round-trip")
                    .isEqualTo(aggregateId);
            then(restored.getAggregateId().value().partA())
                    .as("composite value partA should survive round-trip")
                    .isEqualTo("part-a");
            then(restored.getAggregateId().value().partB())
                    .as("composite value partB should survive round-trip")
                    .isEqualTo("part-b");
            then(restored.getEventId())
                    .as("eventId should survive composite-id round-trip")
                    .isEqualTo("evt-composite");
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods and test doubles (after all Nested classes)
    // -------------------------------------------------------------------------

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

    /**
     * Builds the wire-form composite value node used to encode a
     * {@link CompositeIdValue} inside an {@link EventEnvelope.AggregateIdNode}.
     *
     * @return the composite value node as a nested map
     */
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

    /**
     * Builds the wire-form scalar value node.
     *
     * @param type  the scalar type name
     * @param value the scalar value
     * @return the scalar value node as a map
     */
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

    /**
     * Simple string-based identifier for tests.
     */
    public record SampleIdentifier(String value) implements Identifier<String> {
    }

    /**
     * Simple payload for tests.
     */
    public record SamplePayload(String name, int quantity) {
    }

    /**
     * Concrete event with reconstitution constructor for deserialization tests.
     */
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

    /**
     * Composite identifier value (record) for composite aggregate-id tests.
     */
    public record CompositeIdValue(String partA, String partB) implements Serializable {
    }

    /**
     * Identifier wrapping a composite value.
     */
    public record CompositeSampleIdentifier(CompositeIdValue value) implements Identifier<CompositeIdValue> {
    }

    /**
     * Event using composite aggregate identifier.
     */
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

    /**
     * Intermediate type with a single type argument (the identifier) so that
     * {@code getGenericSuperclass()} yields a ParameterizedType with fewer
     * than two arguments. Identifier resolution still succeeds (args[0]), but
     * payload resolution fails the {@code args.length < 2} guard.
     *
     * @param <I> aggregate identifier type
     */
    public abstract static class IdentifierOnlyBase<I extends Identifier<?>>
            extends AbstractDomainEvent<I, SamplePayload> {

        protected IdentifierOnlyBase(String eventId,
                                     Instant occurredAt,
                                     I aggregateId,
                                     SamplePayload payload,
                                     EventType eventType,
                                     EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    /**
     * Concrete event extending {@link IdentifierOnlyBase} to trigger
     * insufficient type-argument count during payload resolution.
     */
    public static final class SingleArgEvent extends IdentifierOnlyBase<SampleIdentifier> {

        public SingleArgEvent(String eventId,
                              Instant occurredAt,
                              SampleIdentifier aggregateId,
                              SamplePayload payload,
                              EventType eventType,
                              EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    /**
     * Event extending {@link AbstractDomainEvent} as a raw type so that
     * {@code getGenericSuperclass()} is not a {@link java.lang.reflect.ParameterizedType},
     * exercising the failure path of generic-type resolution.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final class RawEvent extends AbstractDomainEvent {

        public RawEvent(String eventId,
                        Instant occurredAt,
                        Identifier aggregateId,
                        Object payload,
                        EventType eventType,
                        EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    /**
     * Non-generic intermediate so the hierarchy walk must skip a non-parameterized
     * superclass before finding {@code AbstractDomainEvent<I, P>}.
     */
    public abstract static class NonGenericIntermediate extends AbstractDomainEvent<SampleIdentifier, SamplePayload> {

        protected NonGenericIntermediate(String eventId,
                                         Instant occurredAt,
                                         SampleIdentifier aggregateId,
                                         SamplePayload payload,
                                         EventType eventType,
                                         EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    /**
     * Concrete event under {@link NonGenericIntermediate} to exercise multiple
     * iterations of the generic-type resolution loop.
     */
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

    /**
     * Event whose payload type is {@code Map<String, String>} so that
     * {@code typeToClass} receives a {@link java.lang.reflect.ParameterizedType}.
     */
    public static final class MapPayloadEvent extends AbstractDomainEvent<SampleIdentifier, Map<String, String>> {

        public MapPayloadEvent(String eventId,
                               Instant occurredAt,
                               SampleIdentifier aggregateId,
                               Map<String, String> payload,
                               EventType eventType,
                               EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    /**
     * Open generic event so that the payload type argument remains a
     * {@link java.lang.reflect.TypeVariable}, exercising the unsupported
     * branch of {@code typeToClass}.
     *
     * @param <P> unresolved payload type
     */
    public static final class OpenPayloadEvent<P> extends AbstractDomainEvent<SampleIdentifier, P> {

        public OpenPayloadEvent(String eventId,
                                Instant occurredAt,
                                SampleIdentifier aggregateId,
                                P payload,
                                EventType eventType,
                                EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    /**
     * DomainEvent that does not extend AbstractDomainEvent (negative type test).
     */
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

    /**
     * Intermediate with swapped type-parameter order so that
     * {@code args[0]} of the resolved parameterized type is the payload
     * type (not an {@link Identifier}), exercising the identifier validation guard.
     *
     * @param <P> payload type (appears as args[0])
     * @param <I> identifier type (appears as args[1])
     */
    public abstract static class SwappedTypeArgsBase<P, I extends Identifier<?>> extends AbstractDomainEvent<I, P> {

        protected SwappedTypeArgsBase(String eventId,
                                      Instant occurredAt,
                                      I aggregateId,
                                      P payload,
                                      EventType eventType,
                                      EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }

    /**
     * Concrete event under {@link SwappedTypeArgsBase} so identifier resolution
     * sees {@link SamplePayload} as args[0].
     */
    public static final class SwappedTypeArgsEvent extends SwappedTypeArgsBase<SamplePayload, SampleIdentifier> {

        public SwappedTypeArgsEvent(String eventId,
                                    Instant occurredAt,
                                    SampleIdentifier aggregateId,
                                    SamplePayload payload,
                                    EventType eventType,
                                    EventMetadata metadata) {
            super(eventId, occurredAt, aggregateId, payload, eventType, metadata);
        }
    }
}
