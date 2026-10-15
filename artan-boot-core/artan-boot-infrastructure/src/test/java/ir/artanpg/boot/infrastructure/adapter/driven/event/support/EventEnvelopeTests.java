package ir.artanpg.boot.infrastructure.adapter.driven.event.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link EventEnvelope}.
 *
 * @author Mohammad Yazdian
 */
@SuppressWarnings("ALL")
@DisplayName("EventEnvelope")
class EventEnvelopeTests {

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Initializes format version to CURRENT_FORMAT_VERSION")
        void Constructor_ShouldInitializeFormatVersion_WhenCreated() {
            // when
            EventEnvelope envelope = new EventEnvelope();

            // then
            then(envelope.getFormatVersion())
                    .as("a fresh envelope must default to the current format version")
                    .isEqualTo(EventEnvelope.CURRENT_FORMAT_VERSION);
        }

        @Test
        @DisplayName("Leaves other fields null after construction")
        void Constructor_ShouldLeaveFieldsNull_WhenCreated() {
            // when
            EventEnvelope envelope = new EventEnvelope();

            // then
            then(envelope.getEventId()).as("eventId must be null on a fresh envelope").isNull();
            then(envelope.getOccurredAt()).as("occurredAt must be null on a fresh envelope").isNull();
            then(envelope.getEventType()).as("eventType must be null on a fresh envelope").isNull();
            then(envelope.getAggregateId()).as("aggregateId must be null on a fresh envelope").isNull();
            then(envelope.getPayload()).as("payload must be null on a fresh envelope").isNull();
            then(envelope.getMetadata()).as("metadata must be null on a fresh envelope").isNull();
        }
    }

    @Nested
    @DisplayName("formatVersion")
    class FormatVersionTests {

        @Test
        @DisplayName("Returns the value set through setFormatVersion")
        void GetFormatVersion_ShouldReturnSetValue_WhenValueWasAssigned() {
            // given
            EventEnvelope envelope = new EventEnvelope();

            // when
            envelope.setFormatVersion(42);

            // then
            then(envelope.getFormatVersion())
                    .as("getFormatVersion must return the value last set")
                    .isEqualTo(42);
        }

        @Test
        @DisplayName("Accepts zero as a valid format version")
        void SetFormatVersion_ShouldAcceptZero_WhenZeroProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();

            // when
            envelope.setFormatVersion(0);

            // then
            then(envelope.getFormatVersion())
                    .as("zero must be accepted as a valid format version")
                    .isZero();
        }

        @Test
        @DisplayName("Accepts negative format version without validation")
        void SetFormatVersion_ShouldAcceptNegative_WhenNegativeProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();

            // when
            envelope.setFormatVersion(-1);

            // then
            then(envelope.getFormatVersion())
                    .as("the DTO must not perform any validation on the format version")
                    .isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("eventId")
    class EventIdTests {

        @Test
        @DisplayName("Returns the event id set through the setter")
        void GetEventId_ShouldReturnSetValue_WhenValueWasAssigned() {
            // given
            EventEnvelope envelope = new EventEnvelope();

            // when
            envelope.setEventId("event-1");

            // then
            then(envelope.getEventId())
                    .as("getEventId must return the value last set")
                    .isEqualTo("event-1");
        }

        @Test
        @DisplayName("Accepts null without validation")
        void SetEventId_ShouldAcceptNull_WhenNullProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            envelope.setEventId("event-1");

            // when
            envelope.setEventId(null);

            // then
            then(envelope.getEventId())
                    .as("the DTO must store null event id verbatim")
                    .isNull();
        }

        @Test
        @DisplayName("Accepts empty string without validation")
        void SetEventId_ShouldAcceptEmptyString_WhenEmptyStringProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();

            // when
            envelope.setEventId("");

            // then
            then(envelope.getEventId())
                    .as("the DTO must store an empty event id verbatim")
                    .isEmpty();
        }

        @Test
        @DisplayName("Accepts blank string without validation")
        void SetEventId_ShouldAcceptBlankString_WhenBlankStringProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();

            // when
            envelope.setEventId("   ");

            // then
            then(envelope.getEventId())
                    .as("the DTO must store a blank event id verbatim")
                    .isEqualTo("   ");
        }
    }

    @Nested
    @DisplayName("occurredAt")
    class OccurredAtTests {

        @Test
        @DisplayName("Returns the timestamp set through the setter")
        void GetOccurredAt_ShouldReturnSetValue_WhenValueWasAssigned() {
            // given
            EventEnvelope envelope = new EventEnvelope();

            // when
            envelope.setOccurredAt("2024-01-01T00:00:00Z");

            // then
            then(envelope.getOccurredAt())
                    .as("getOccurredAt must return the value last set")
                    .isEqualTo("2024-01-01T00:00:00Z");
        }

        @Test
        @DisplayName("Accepts null without validation")
        void SetOccurredAt_ShouldAcceptNull_WhenNullProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            envelope.setOccurredAt("2024-01-01T00:00:00Z");

            // when
            envelope.setOccurredAt(null);

            // then
            then(envelope.getOccurredAt())
                    .as("the DTO must store null occurredAt verbatim")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("eventType")
    class EventTypeTests {

        @Test
        @DisplayName("Returns the event type node set through the setter")
        void GetEventType_ShouldReturnSetValue_WhenValueWasAssigned() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            EventEnvelope.EventTypeNode node = new EventEnvelope.EventTypeNode();

            // when
            envelope.setEventType(node);

            // then
            then(envelope.getEventType())
                    .as("getEventType must return the exact instance last set")
                    .isSameAs(node);
        }

        @Test
        @DisplayName("Accepts null without validation")
        void SetEventType_ShouldAcceptNull_WhenNullProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            envelope.setEventType(new EventEnvelope.EventTypeNode());

            // when
            envelope.setEventType(null);

            // then
            then(envelope.getEventType())
                    .as("the DTO must store null eventType verbatim")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("aggregateId")
    class AggregateIdTests {

        @Test
        @DisplayName("Returns the aggregate id node set through the setter")
        void GetAggregateId_ShouldReturnSetValue_WhenValueWasAssigned() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            EventEnvelope.AggregateIdNode node = new EventEnvelope.AggregateIdNode();

            // when
            envelope.setAggregateId(node);

            // then
            then(envelope.getAggregateId())
                    .as("getAggregateId must return the exact instance last set")
                    .isSameAs(node);
        }

        @Test
        @DisplayName("Accepts null without validation")
        void SetAggregateId_ShouldAcceptNull_WhenNullProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            envelope.setAggregateId(new EventEnvelope.AggregateIdNode());

            // when
            envelope.setAggregateId(null);

            // then
            then(envelope.getAggregateId())
                    .as("the DTO must store null aggregateId verbatim")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("payload")
    class PayloadTests {

        @Test
        @DisplayName("Returns the JSON payload set through the setter")
        void GetPayload_ShouldReturnSetValue_WhenValueWasAssigned() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            JsonNode payload = JsonNodeFactory.instance.textNode("some-payload");

            // when
            envelope.setPayload(payload);

            // then
            then(envelope.getPayload())
                    .as("getPayload must return the exact JSON node last set")
                    .isSameAs(payload);
        }

        @Test
        @DisplayName("Accepts null without validation")
        void SetPayload_ShouldAcceptNull_WhenNullProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            envelope.setPayload(JsonNodeFactory.instance.textNode("x"));

            // when
            envelope.setPayload(null);

            // then
            then(envelope.getPayload())
                    .as("the DTO must store null payload verbatim")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("metadata")
    class MetadataTests {

        @Test
        @DisplayName("Returns the metadata node set through the setter")
        void GetMetadata_ShouldReturnSetValue_WhenValueWasAssigned() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();

            // when
            envelope.setMetadata(node);

            // then
            then(envelope.getMetadata())
                    .as("getMetadata must return the exact instance last set")
                    .isSameAs(node);
        }

        @Test
        @DisplayName("Accepts null without validation")
        void SetMetadata_ShouldAcceptNull_WhenNullProvided() {
            // given
            EventEnvelope envelope = new EventEnvelope();
            envelope.setMetadata(new EventEnvelope.MetadataNode());

            // when
            envelope.setMetadata(null);

            // then
            then(envelope.getMetadata())
                    .as("the DTO must store null metadata verbatim")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("EventTypeNode")
    class EventTypeNodeTests {

        @Nested
        @DisplayName("constructor")
        class ConstructorTests {

            @Test
            @DisplayName("Leaves name, category and version null after construction")
            void Constructor_ShouldLeaveFieldsNull_WhenCreated() {
                // when
                EventEnvelope.EventTypeNode node = new EventEnvelope.EventTypeNode();

                // then
                then(node.getName()).as("name must be null on a fresh node").isNull();
                then(node.getCategory()).as("category must be null on a fresh node").isNull();
                then(node.getVersion()).as("version must be null on a fresh node").isNull();
            }
        }

        @Nested
        @DisplayName("name")
        class NameTests {

            @Test
            @DisplayName("Returns the name set through the setter")
            void GetName_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.EventTypeNode node = new EventEnvelope.EventTypeNode();

                // when
                node.setName("OrderPlaced");

                // then
                then(node.getName())
                        .as("getName must return the value last set")
                        .isEqualTo("OrderPlaced");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetName_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.EventTypeNode node = new EventEnvelope.EventTypeNode();
                node.setName("OrderPlaced");

                // when
                node.setName(null);

                // then
                then(node.getName())
                        .as("the node must store a null name verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("category")
        class CategoryTests {

            @Test
            @DisplayName("Returns the category set through the setter")
            void GetCategory_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.EventTypeNode node = new EventEnvelope.EventTypeNode();

                // when
                node.setCategory("sales");

                // then
                then(node.getCategory())
                        .as("getCategory must return the value last set")
                        .isEqualTo("sales");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetCategory_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.EventTypeNode node = new EventEnvelope.EventTypeNode();
                node.setCategory("sales");

                // when
                node.setCategory(null);

                // then
                then(node.getCategory())
                        .as("the node must store a null category verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("version")
        class VersionTests {

            @Test
            @DisplayName("Returns the version set through the setter")
            void GetVersion_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.EventTypeNode node = new EventEnvelope.EventTypeNode();

                // when
                node.setVersion("v2");

                // then
                then(node.getVersion())
                        .as("getVersion must return the value last set")
                        .isEqualTo("v2");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetVersion_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.EventTypeNode node = new EventEnvelope.EventTypeNode();
                node.setVersion("v2");

                // when
                node.setVersion(null);

                // then
                then(node.getVersion())
                        .as("the node must store a null version verbatim")
                        .isNull();
            }
        }
    }

    @Nested
    @DisplayName("AggregateIdNode")
    class AggregateIdNodeTests {

        @Nested
        @DisplayName("constructor")
        class ConstructorTests {

            @Test
            @DisplayName("Leaves identifierType, valueType and value null after construction")
            void Constructor_ShouldLeaveFieldsNull_WhenCreated() {
                // when
                EventEnvelope.AggregateIdNode node = new EventEnvelope.AggregateIdNode();

                // then
                then(node.getIdentifierType()).as("identifierType must be null on a fresh node").isNull();
                then(node.getValueType()).as("valueType must be null on a fresh node").isNull();
                then(node.getValue()).as("value must be null on a fresh node").isNull();
            }
        }

        @Nested
        @DisplayName("identifierType")
        class IdentifierTypeTests {

            @Test
            @DisplayName("Returns the identifier type set through the setter")
            void GetIdentifierType_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.AggregateIdNode node = new EventEnvelope.AggregateIdNode();

                // when
                node.setIdentifierType("com.example.OrderId");

                // then
                then(node.getIdentifierType())
                        .as("getIdentifierType must return the value last set")
                        .isEqualTo("com.example.OrderId");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetIdentifierType_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.AggregateIdNode node = new EventEnvelope.AggregateIdNode();
                node.setIdentifierType("com.example.OrderId");

                // when
                node.setIdentifierType(null);

                // then
                then(node.getIdentifierType())
                        .as("the node must store a null identifier type verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("valueType")
        class ValueTypeTests {

            @Test
            @DisplayName("Returns the value type set through the setter")
            void GetValueType_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.AggregateIdNode node = new EventEnvelope.AggregateIdNode();

                // when
                node.setValueType(String.class.getName());

                // then
                then(node.getValueType())
                        .as("getValueType must return the value last set")
                        .isEqualTo(String.class.getName());
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetValueType_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.AggregateIdNode node = new EventEnvelope.AggregateIdNode();
                node.setValueType(String.class.getName());

                // when
                node.setValueType(null);

                // then
                then(node.getValueType())
                        .as("the node must store a null value type verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("value")
        class ValueTests {

            @Test
            @DisplayName("Returns the value map set through the setter")
            void GetValue_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.AggregateIdNode node = new EventEnvelope.AggregateIdNode();
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("type", "java.lang.String");
                value.put("value", "order-1");

                // when
                node.setValue(value);

                // then
                then(node.getValue())
                        .as("getValue must return the exact map instance last set")
                        .isSameAs(value);
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetValue_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.AggregateIdNode node = new EventEnvelope.AggregateIdNode();
                node.setValue(Map.of("k", "v"));

                // when
                node.setValue(null);

                // then
                then(node.getValue())
                        .as("the node must store a null value map verbatim")
                        .isNull();
            }
        }
    }

    @Nested
    @DisplayName("MetadataNode")
    class MetadataNodeTests {

        @Nested
        @DisplayName("constructor")
        class ConstructorTests {

            @Test
            @DisplayName("Leaves all fields null after construction")
            void Constructor_ShouldLeaveFieldsNull_WhenCreated() {
                // when
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();

                // then
                then(node.getCorrelationId()).as("correlationId must be null").isNull();
                then(node.getCausationId()).as("causationId must be null").isNull();
                then(node.getTenant()).as("tenant must be null").isNull();
                then(node.getTraceId()).as("traceId must be null").isNull();
                then(node.getSpanId()).as("spanId must be null").isNull();
                then(node.getUserId()).as("userId must be null").isNull();
                then(node.getProperties()).as("properties must be null").isNull();
            }
        }

        @Nested
        @DisplayName("correlationId")
        class CorrelationIdTests {

            @Test
            @DisplayName("Returns the correlation id set through the setter")
            void GetCorrelationId_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();

                // when
                node.setCorrelationId("corr-1");

                // then
                then(node.getCorrelationId())
                        .as("getCorrelationId must return the value last set")
                        .isEqualTo("corr-1");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetCorrelationId_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();
                node.setCorrelationId("corr-1");

                // when
                node.setCorrelationId(null);

                // then
                then(node.getCorrelationId())
                        .as("the node must store a null correlation id verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("causationId")
        class CausationIdTests {

            @Test
            @DisplayName("Returns the causation id set through the setter")
            void GetCausationId_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();

                // when
                node.setCausationId("cause-1");

                // then
                then(node.getCausationId())
                        .as("getCausationId must return the value last set")
                        .isEqualTo("cause-1");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetCausationId_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();
                node.setCausationId("cause-1");

                // when
                node.setCausationId(null);

                // then
                then(node.getCausationId())
                        .as("the node must store a null causation id verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("tenant")
        class TenantTests {

            @Test
            @DisplayName("Returns the tenant set through the setter")
            void GetTenant_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();

                // when
                node.setTenant("tenant-a");

                // then
                then(node.getTenant())
                        .as("getTenant must return the value last set")
                        .isEqualTo("tenant-a");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetTenant_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();
                node.setTenant("tenant-a");

                // when
                node.setTenant(null);

                // then
                then(node.getTenant())
                        .as("the node must store a null tenant verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("traceId")
        class TraceIdTests {

            @Test
            @DisplayName("Returns the trace id set through the setter")
            void GetTraceId_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();

                // when
                node.setTraceId("trace-1");

                // then
                then(node.getTraceId())
                        .as("getTraceId must return the value last set")
                        .isEqualTo("trace-1");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetTraceId_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();
                node.setTraceId("trace-1");

                // when
                node.setTraceId(null);

                // then
                then(node.getTraceId())
                        .as("the node must store a null trace id verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("spanId")
        class SpanIdTests {

            @Test
            @DisplayName("Returns the span id set through the setter")
            void GetSpanId_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();

                // when
                node.setSpanId("span-1");

                // then
                then(node.getSpanId())
                        .as("getSpanId must return the value last set")
                        .isEqualTo("span-1");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetSpanId_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();
                node.setSpanId("span-1");

                // when
                node.setSpanId(null);

                // then
                then(node.getSpanId())
                        .as("the node must store a null span id verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("userId")
        class UserIdTests {

            @Test
            @DisplayName("Returns the user id set through the setter")
            void GetUserId_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();

                // when
                node.setUserId("user-1");

                // then
                then(node.getUserId())
                        .as("getUserId must return the value last set")
                        .isEqualTo("user-1");
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetUserId_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();
                node.setUserId("user-1");

                // when
                node.setUserId(null);

                // then
                then(node.getUserId())
                        .as("the node must store a null user id verbatim")
                        .isNull();
            }
        }

        @Nested
        @DisplayName("properties")
        class PropertiesTests {

            @Test
            @DisplayName("Returns the properties map set through the setter")
            void GetProperties_ShouldReturnSetValue_WhenValueWasAssigned() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();
                Map<String, Object> properties = new LinkedHashMap<>();
                properties.put("region", "eu");
                properties.put("priority", 3);

                // when
                node.setProperties(properties);

                // then
                then(node.getProperties())
                        .as("getProperties must return the exact map instance last set")
                        .isSameAs(properties);
            }

            @Test
            @DisplayName("Accepts null without validation")
            void SetProperties_ShouldAcceptNull_WhenNullProvided() {
                // given
                EventEnvelope.MetadataNode node = new EventEnvelope.MetadataNode();
                node.setProperties(Map.of("k", "v"));

                // when
                node.setProperties(null);

                // then
                then(node.getProperties())
                        .as("the node must store a null properties map verbatim")
                        .isNull();
            }
        }
    }
}
