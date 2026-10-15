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

import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventMetadata;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.SerializationException;
import ir.artanpg.boot.domain.model.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link JacksonEventSerializer}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("JacksonEventSerializer")
class JacksonEventSerializerTests {

    private static final String EVENT_ID = "event-1";
    private static final Instant OCCURRED_AT = Instant.parse("2024-01-01T00:00:00Z");
    private static final EventType SAMPLE_TYPE = EventType.of("SampleEvent");
    private static final String AGGREGATE_VALUE = "agg-1";
    private static final String PAYLOAD_VALUE = "payload";

    private JacksonEventSerializer serializer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        serializer = new JacksonEventSerializer(objectMapper);
    }

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Creates serializer instance when mapper is not null")
        void Constructor_ShouldCreateInstance_WhenMapperIsNotNull() {
            // when
            JacksonEventSerializer instance = new JacksonEventSerializer(new ObjectMapper());

            // then
            then(instance)
                    .as("the constructor must produce a usable serializer when a mapper is supplied")
                    .isNotNull();
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        @DisplayName("Throws SerializationException when mapper is null")
        void Constructor_ShouldThrowSerializationException_WhenMapperIsNull() {
            // given
            ObjectMapper nullMapper = null;

            // when & then
            thenThrownBy(() -> new JacksonEventSerializer(nullMapper))
                    .as("a null ObjectMapper must be rejected by the constructor")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The ObjectMapper cannot be null");
        }
    }

    @Nested
    @DisplayName("serialize(DomainEvent)")
    class SerializeTests {

        @Test
        @DisplayName("Serializes a string-identifier event with empty metadata")
        void Serialize_ShouldProduceJson_WhenEventHasScalarIdentifierAndEmptyMetadata() {
            // given
            DomainEvent<Identifier<String>, String> event = baseEvent();
            given(event.getMetadata()).willReturn(EventMetadata.empty());

            // when
            byte[] bytes = serializer.serialize(event);
            String json = new String(bytes, StandardCharsets.UTF_8);

            // then
            then(json)
                    .as("the serialized JSON must include the format version field")
                    .contains("\"formatVersion\":1");
            then(json)
                    .as("the serialized JSON must include the event id")
                    .contains("\"eventId\":\"" + EVENT_ID + "\"");
            then(json)
                    .as("the serialized JSON must include the ISO-8601 occurrence time")
                    .contains("\"occurredAt\":\"" + OCCURRED_AT + "\"");
            then(json)
                    .as("the serialized JSON must include the event type name")
                    .contains("\"name\":\"SampleEvent\"");
            then(json)
                    .as("the serialized JSON must include the aggregate identifier type")
                    .contains("\"identifierType\":\"" + SampleIdentifier.class.getName() + "\"");
            then(json)
                    .as("the serialized JSON must include the aggregate identifier as a scalar value node")
                    .contains("\"kind\":\"scalar\"");
            then(json)
                    .as("the serialized JSON must include the aggregate value")
                    .contains("\"value\":\"" + AGGREGATE_VALUE + "\"");
            then(json)
                    .as("the serialized JSON must include the payload")
                    .contains("\"payload\":\"payload\"");
        }

        @Test
        @DisplayName("Serializes event type with category and version")
        void Serialize_ShouldIncludeCategoryAndVersion_WhenEventTypeHasThem() {
            // given
            DomainEvent<Identifier<String>, String> event = baseEvent();
            given(event.getEventType()).willReturn(EventType.of("OrderPlaced", "sales", "v2"));

            // when
            String json = new String(serializer.serialize(event), StandardCharsets.UTF_8);

            // then
            then(json)
                    .as("the serialized JSON must include the event type category")
                    .contains("\"category\":\"sales\"");
            then(json)
                    .as("the serialized JSON must include the event type version")
                    .contains("\"version\":\"v2\"");
        }

        @Test
        @DisplayName("Serializes composite identifier with nested fields")
        void Serialize_ShouldIncludeNestedFields_WhenIdentifierIsComposite() {
            // given
            DomainEvent<Identifier<CompositeValue>, String> event = compositeEvent();

            // when
            String json = new String(serializer.serialize(event), StandardCharsets.UTF_8);

            // then
            then(json)
                    .as("a composite identifier must be serialized with the composite kind")
                    .contains("\"kind\":\"composite\"");
            then(json)
                    .as("the composite node must include its concrete record type name")
                    .contains("\"type\":\"" + CompositeValue.class.getName() + "\"");
            then(json)
                    .as("the composite node must include the nested fields map")
                    .contains("\"fields\":");
            then(json)
                    .as("the nested name field must be serialized as a scalar")
                    .contains("\"name\":{\"kind\":\"scalar\"");
        }

        @Test
        @DisplayName("Serializes metadata with standard fields when properties are empty")
        void Serialize_ShouldIncludeMetadataFields_WhenPropertiesAreEmpty() {
            // given
            DomainEvent<Identifier<String>, String> event = baseEvent();
            EventMetadata metadata = EventMetadata.builder()
                    .correlationId("corr-1")
                    .causationId("cause-1")
                    .tenant("tenant-a")
                    .traceId("trace-1")
                    .spanId("span-1")
                    .userId("user-1")
                    .build();
            given(event.getMetadata()).willReturn(metadata);

            // when
            String json = new String(serializer.serialize(event), StandardCharsets.UTF_8);

            // then
            then(json)
                    .as("the serialized metadata must include the correlation id")
                    .contains("\"correlationId\":\"corr-1\"");
            then(json)
                    .as("the serialized metadata must include the causation id")
                    .contains("\"causationId\":\"cause-1\"");
            then(json)
                    .as("the serialized metadata must include the tenant")
                    .contains("\"tenant\":\"tenant-a\"");
            then(json)
                    .as("the serialized metadata must include the trace id")
                    .contains("\"traceId\":\"trace-1\"");
            then(json)
                    .as("the serialized metadata must include the span id")
                    .contains("\"spanId\":\"span-1\"");
            then(json)
                    .as("the serialized metadata must include the user id")
                    .contains("\"userId\":\"user-1\"");
        }

        @Test
        @DisplayName("Serializes metadata properties when they are not empty")
        void Serialize_ShouldIncludeMetadataProperties_WhenPropertiesAreNotEmpty() {
            // given
            DomainEvent<Identifier<String>, String> event = baseEvent();
            EventMetadata metadata = EventMetadata.builder()
                    .properties("region", "eu")
                    .properties("priority", 3)
                    .build();
            given(event.getMetadata()).willReturn(metadata);

            // when
            String json = new String(serializer.serialize(event), StandardCharsets.UTF_8);

            // then
            then(json)
                    .as("the serialized metadata must include the properties map")
                    .contains("\"properties\":");
            then(json)
                    .as("the serialized metadata must include the region property")
                    .contains("\"region\":\"eu\"");
            then(json)
                    .as("the serialized metadata must include the priority property")
                    .contains("\"priority\":3");
        }

        @Test
        @DisplayName("Serializes empty metadata node when metadata is null")
        void Serialize_ShouldProduceEmptyMetadataNode_WhenMetadataIsNull() {
            // given
            DomainEvent<Identifier<String>, String> event = baseEvent();
            given(event.getMetadata()).willReturn(null);

            // when
            String json = new String(serializer.serialize(event), StandardCharsets.UTF_8);

            // then
            then(json)
                    .as("a null metadata must still produce a metadata node in the JSON envelope")
                    .contains("\"metadata\":");
            then(json)
                    .as("a null metadata must produce a metadata node with no correlation id")
                    .doesNotContain("\"correlationId\":\"corr");
        }

        @SuppressWarnings("ConstantConditions")
        @Test
        @DisplayName("Throws SerializationException when event is null")
        void Serialize_ShouldThrowSerializationException_WhenEventIsNull() {
            // given
            DomainEvent<?, ?> event = null;

            // when & then
            thenThrownBy(() -> serializer.serialize(event))
                    .as("a null event must be rejected by serialize")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("The event cannot be null");
        }

        @Test
        @DisplayName("Rethrows SerializationException thrown while building the envelope")
        void Serialize_ShouldRethrowSerializationException_WhenEnvelopeBuildingFails() {
            // given
            DomainEvent<?, ?> event = mock(DomainEvent.class);
            given(event.getEventId()).willThrow(new SerializationException("from event"));

            // when & then
            thenThrownBy(() -> serializer.serialize(event))
                    .as("a SerializationException raised during envelope construction must be rethrown verbatim")
                    .isInstanceOf(SerializationException.class)
                    .hasMessage("from event");
        }

        @Test
        @DisplayName("Wraps non-SerializationException into a SerializationException")
        void Serialize_ShouldWrapOtherExceptions_WhenEnvelopeBuildingFailsUnexpectedly() {
            // given
            DomainEvent<?, ?> event = mock(DomainEvent.class);
            given(event.getEventId()).willThrow(new IllegalStateException("boom"));

            // when & then
            thenThrownBy(() -> serializer.serialize(event))
                    .as("an unexpected exception during envelope construction must be wrapped in a" +
                            " SerializationException")
                    .isInstanceOf(SerializationException.class)
                    .hasMessageContaining("Failed to serialize event of type")
                    .hasMessageContaining("boom");
        }
    }

    @Nested
    @DisplayName("supportedFormat()")
    class SupportedFormatTests {

        @Test
        @DisplayName("Returns the canonical JSON format identifier")
        void SupportedFormat_ShouldReturnFormatConstant_WhenCalled() {
            // when
            String format = serializer.supportedFormat();

            // then
            then(format)
                    .as("supportedFormat must return the JSON format identifier exposed as a constant")
                    .isEqualTo(JacksonEventSerializer.FORMAT);
        }
    }

    // Helpers and test doubles below all nested test classes

    @SuppressWarnings("unchecked")
    private static DomainEvent<Identifier<String>, String> baseEvent() {
        DomainEvent<Identifier<String>, String> event = mock(DomainEvent.class);
        given(event.getEventId()).willReturn(EVENT_ID);
        given(event.getOccurredAt()).willReturn(OCCURRED_AT);
        given(event.getEventType()).willReturn(SAMPLE_TYPE);
        given(event.getAggregateId()).willReturn(new SampleIdentifier(AGGREGATE_VALUE));
        given(event.getPayload()).willReturn(PAYLOAD_VALUE);
        given(event.getMetadata()).willReturn(EventMetadata.empty());
        return event;
    }

    @SuppressWarnings("unchecked")
    private static DomainEvent<Identifier<CompositeValue>, String> compositeEvent() {
        DomainEvent<Identifier<CompositeValue>, String> event = mock(DomainEvent.class);
        given(event.getEventId()).willReturn(EVENT_ID);
        given(event.getOccurredAt()).willReturn(OCCURRED_AT);
        given(event.getEventType()).willReturn(SAMPLE_TYPE);
        given(event.getAggregateId()).willReturn(new CompositeIdentifier(new CompositeValue("alice", 3)));
        given(event.getPayload()).willReturn(PAYLOAD_VALUE);
        given(event.getMetadata()).willReturn(EventMetadata.empty());
        return event;
    }

    public record SampleIdentifier(String value) implements Identifier<String> {
    }

    public record CompositeValue(String name, Integer count) implements Serializable {
    }

    public record CompositeIdentifier(CompositeValue value) implements Identifier<CompositeValue> {
    }
}
