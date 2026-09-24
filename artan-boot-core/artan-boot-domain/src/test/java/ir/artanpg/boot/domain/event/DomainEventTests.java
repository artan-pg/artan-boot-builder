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
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link DomainEvent}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("DomainEvent")
class DomainEventTests {

    private static final String EVENT_TYPE_NAME = "TEST_EVENT";
    private static final String OTHER_EVENT_TYPE_NAME = "OTHER_EVENT";
    private static final String EVENT_ID = "event-1";
    private static final String AGGREGATE_ID_VALUE = "aggregate-1";

    private static final String PAYLOAD_VALUE = "test-payload";
    private static final String ERROR_AGGREGATE_ID_NULL = "The aggregateId cannot be null";
    private static final String ERROR_PAYLOAD_NULL = "The source cannot be null";
    private static final long FIXED_MILLIS = 1_700_000_000_000L;
    private static final Instant FIXED_INSTANT = Instant.ofEpochMilli(FIXED_MILLIS);

    @Nested
    @DisplayName("Constructor with aggregateId and payload")
    class ConstructorWithAggregateIdAndPayload {

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when aggregateId is null")
        void constructor_ShouldThrowDomainException_WhenAggregateIdIsNull() {
            // given
            TestIdentifier nullAggregateId = null;
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);

            // when & then
            thenThrownBy(() ->
                    new TestDomainEvent(nullAggregateId, payload))
                    .as("Should throw DomainException when aggregateId is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_AGGREGATE_ID_NULL);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when payload is null")
        void constructor_ShouldThrowDomainException_WhenPayloadIsNull() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload nullPayload = null;

            // when & then
            thenThrownBy(() ->
                    new TestDomainEvent(aggregateId, nullPayload))
                    .as("Should throw DomainException when payload is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PAYLOAD_NULL);
        }

        @Test
        @DisplayName("should generate unique event IDs for different events")
        void constructor_ShouldGenerateUniqueEventIds_WhenMultipleEventsCreated() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);

            // when
            TestDomainEvent event1 = new TestDomainEvent(aggregateId, payload);
            TestDomainEvent event2 = new TestDomainEvent(aggregateId, payload);

            // then
            then(event1.getEventId())
                    .as("Event IDs should be unique")
                    .isNotEqualTo(event2.getEventId());
        }

        @Test
        @DisplayName("should use current time when clock is null")
        void constructor_ShouldUseCurrentTime_WhenClockIsNull() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            Instant beforeCreation = Instant.now().truncatedTo(ChronoUnit.MILLIS);

            // when
            TestDomainEvent event = new TestDomainEvent(aggregateId, payload, null);
            Instant afterCreation = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusMillis(1);

            // then
            then(event.getOccurredAt())
                    .as("Occurred at should be between before and after")
                    .isAfterOrEqualTo(beforeCreation)
                    .isBeforeOrEqualTo(afterCreation);
        }
    }

    @Nested
    @DisplayName("Constructor with Clock")
    class ConstructorWithClock {

        @Test
        @DisplayName("should use provided clock for occurredAt")
        void constructor_ShouldUseProvidedClock_WhenClockIsProvided() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));

            // when
            TestDomainEvent event = new TestDomainEvent(aggregateId, payload, fixedClock);

            // then
            then(event.getOccurredAt())
                    .as("Occurred at should match fixed clock")
                    .isEqualTo(FIXED_INSTANT);
        }
    }

    @Nested
    @DisplayName("getEventId")
    class GetEventId {

        @Test
        @DisplayName("should return non-null event ID")
        void getEventId_ShouldReturnNonNull_WhenCalled() {
            // given
            TestDomainEvent event = createEvent();

            // when
            String eventId = event.getEventId();

            // then
            then(eventId)
                    .as("Event ID should not be null")
                    .isNotNull()
                    .isNotEmpty();
        }

        @Test
        @DisplayName("should return consistent event ID on multiple calls")
        void getEventId_ShouldReturnConsistentId_WhenCalledMultipleTimes() {
            // given
            TestDomainEvent event = createEvent();

            // when
            String eventId1 = event.getEventId();
            String eventId2 = event.getEventId();

            // then
            then(eventId1)
                    .as("Event ID should be consistent")
                    .isEqualTo(eventId2);
        }
    }

    @Nested
    @DisplayName("getOccurredAt")
    class GetOccurredAt {

        @Test
        @DisplayName("should return occurredAt as Instant")
        void getOccurredAt_ShouldReturnInstant_WhenCalled() {
            // given
            Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneId.of("UTC"));
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            TestDomainEvent event =
                    new TestDomainEvent(aggregateId, payload, fixedClock);

            // when
            Instant occurredAt = event.getOccurredAt();

            // then
            then(occurredAt)
                    .as("Occurred at should match fixed clock")
                    .isEqualTo(FIXED_INSTANT);
        }
    }

    @Nested
    @DisplayName("getAggregateId")
    class GetAggregateId {

        @Test
        @DisplayName("should return aggregate identifier")
        void getAggregateId_ShouldReturnAggregateIdentifier_WhenCalled() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            TestDomainEvent event = new TestDomainEvent(aggregateId, payload);

            // when
            TestIdentifier result = event.getAggregateId();

            // then
            then(result)
                    .as("Aggregate ID should match")
                    .isEqualTo(aggregateId);
        }
    }

    @Nested
    @DisplayName("getPayload")
    class GetPayload {

        @Test
        @DisplayName("should return payload")
        void getPayload_ShouldReturnPayload_WhenCalled() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            TestDomainEvent event = new TestDomainEvent(aggregateId, payload);

            // when
            TestPayload result = event.getPayload();

            // then
            then(result)
                    .as("Payload should match")
                    .isEqualTo(payload);
        }
    }

    @Nested
    @DisplayName("equals")
    class Equals {

        @Test
        @DisplayName("should be equal when event IDs are same")
        void equals_ShouldBeEqual_WhenEventIdsAreSame() {
            // given
            TestDomainEvent event1 = createEvent();
            TestDomainEventWithId event2 = new TestDomainEventWithId(
                    event1.getEventId(),
                    new TestIdentifier(AGGREGATE_ID_VALUE),
                    new TestPayload(PAYLOAD_VALUE));

            // when & then
            then(event1)
                    .as("Events with same ID should be equal")
                    .isEqualTo(event2);
        }

        @Test
        @DisplayName("should not be equal when event IDs differ")
        void equals_ShouldNotBeEqual_WhenEventIdsDiffer() {
            // given
            TestDomainEvent event1 = createEvent();
            TestDomainEvent event2 = createEvent();

            // when & then
            then(event1)
                    .as("Events with different IDs should not be equal")
                    .isNotEqualTo(event2);
        }

        @Test
        @DisplayName("should be equal to itself")
        void equals_ShouldBeEqualToItself_WhenCompared() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event)
                    .as("Event should be equal to itself")
                    .isEqualTo(event);
        }

        @Test
        @DisplayName("should not be equal to null")
        void equals_ShouldNotBeEqualToNull_WhenCompared() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event)
                    .as("Event should not be equal to null")
                    .isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void equals_ShouldNotBeEqualToDifferentType_WhenCompared() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event)
                    .as("Event should not be equal to different type")
                    .isNotEqualTo("not an event");
        }
    }

    @Nested
    @DisplayName("hashCode")
    class HashCode {

        @Test
        @DisplayName("should return same hash code for equal events")
        void hashCode_ShouldReturnSameHashCode_WhenEventsAreEqual() {
            // given
            TestDomainEvent event1 = createEvent();
            TestDomainEventWithId event2 = new TestDomainEventWithId(
                    event1.getEventId(),
                    new TestIdentifier(AGGREGATE_ID_VALUE),
                    new TestPayload(PAYLOAD_VALUE));

            // when & then
            then(event1.hashCode())
                    .as("Equal events should have same hash code")
                    .isEqualTo(event2.hashCode());
        }

        @Test
        @DisplayName("should return different hash codes for different events")
        void hashCode_ShouldReturnDifferentHashCodes_WhenEventsDiffer() {
            // given
            TestDomainEvent event1 = createEvent();
            TestDomainEvent event2 = createEvent();

            // when & then
            then(event1.hashCode())
                    .as("Different events should have different hash codes")
                    .isNotEqualTo(event2.hashCode());
        }

        @Test
        @DisplayName("should be consistent on multiple calls")
        void hashCode_ShouldBeConsistent_WhenCalledMultipleTimes() {
            // given
            TestDomainEvent event = createEvent();

            // when
            int hashCode1 = event.hashCode();
            int hashCode2 = event.hashCode();

            // then
            then(hashCode1)
                    .as("Hash code should be consistent")
                    .isEqualTo(hashCode2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should contain class name and fields")
        void toString_ShouldContainClassNameAndFields_WhenCalled() {
            // given
            TestDomainEvent event = createEvent();

            // when
            String result = event.toString();

            // then
            then(result)
                    .as("Should contain class name")
                    .contains("TestDomainEvent")
                    .as("Should contain eventId")
                    .contains("eventId=")
                    .as("Should contain occurredAt")
                    .contains("occurredAt=")
                    .as("Should contain aggregateId")
                    .contains("aggregateId=")
                    .as("Should contain payload")
                    .contains("payload=");
        }
    }

    @Nested
    @DisplayName("Serializable")
    class SerializableTests {

        @Test
        @DisplayName("should be instance of Serializable")
        void serializable_ShouldBeInstanceOfSerializable_WhenCreated() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event)
                    .as("Should be Serializable")
                    .isInstanceOf(java.io.Serializable.class);
        }

        @Test
        @DisplayName("should be instance of DomainEvent")
        void serializable_ShouldBeInstanceOfDomainEvent_WhenCreated() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event)
                    .as("Should be a DomainEvent")
                    .isInstanceOf(DomainEvent.class);
        }
    }

    @Nested
    @DisplayName("isOfType")
    class IsOfType {

        @Test
        @DisplayName("should return true when event type matches")
        void isOfType_ShouldReturnTrue_WhenEventTypeMatches() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);
            TestDomainEventImpl event = new TestDomainEventImpl(EVENT_ID, eventType, AGGREGATE_ID_VALUE);

            // when
            boolean result = event.isOfType(eventType);

            // then
            then(result)
                    .as("Should return true when event type matches")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when event type differs")
        void isOfType_ShouldReturnFalse_WhenEventTypeDiffers() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);
            EventType otherType = EventType.valueOf(OTHER_EVENT_TYPE_NAME);
            TestDomainEventImpl event = new TestDomainEventImpl(EVENT_ID, eventType, AGGREGATE_ID_VALUE);

            // when
            boolean result = event.isOfType(otherType);

            // then
            then(result)
                    .as("Should return false when event type differs")
                    .isFalse();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return false when type is null")
        void isOfType_ShouldReturnFalse_WhenTypeIsNull() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);
            TestDomainEventImpl event = new TestDomainEventImpl(EVENT_ID, eventType, AGGREGATE_ID_VALUE);
            EventType nullType = null;

            // when
            boolean result = event.isOfType(nullType);

            // then
            then(result)
                    .as("Should return false when type is null")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("getMetaData")
    class GetMetaData {

        @Test
        @DisplayName("should return non-null metadata map")
        void getMetaData_ShouldReturnNonNullMap_WhenCalled() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);
            TestDomainEventImpl event = new TestDomainEventImpl(EVENT_ID, eventType, AGGREGATE_ID_VALUE);

            // when
            Map<String, Object> metaData = event.getMetaData();

            // then
            then(metaData)
                    .as("Metadata map should not be null")
                    .isNotNull();
        }

        @Test
        @DisplayName("should return empty metadata map by default")
        void getMetaData_ShouldReturnEmptyMap_WhenCalled() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);
            TestDomainEventImpl event = new TestDomainEventImpl(EVENT_ID, eventType, AGGREGATE_ID_VALUE);

            // when
            Map<String, Object> metaData = event.getMetaData();

            // then
            then(metaData)
                    .as("Metadata map should be empty by default")
                    .isEmpty();
        }

        @Test
        @DisplayName("should return consistent metadata on multiple calls")
        void getMetaData_ShouldReturnConsistentMap_WhenCalledMultipleTimes() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);
            TestDomainEventImpl event = new TestDomainEventImpl(EVENT_ID, eventType, AGGREGATE_ID_VALUE);

            // when
            Map<String, Object> metaData1 = event.getMetaData();
            Map<String, Object> metaData2 = event.getMetaData();

            // then
            then(metaData1)
                    .as("Metadata map should be consistent on multiple calls")
                    .isEqualTo(metaData2);
        }
    }
    // ========================================================================================
    // Helper Methods
    // ========================================================================================

    private TestDomainEvent createEvent() {
        TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
        TestPayload payload = new TestPayload(PAYLOAD_VALUE);
        return new TestDomainEvent(aggregateId, payload);
    }

    // ========================================================================================
    // Test Doubles
    // ========================================================================================

    private record TestIdentifier(String value) implements Identifier<String> {

        @Override
        public @NonNull String value() {
            return this.value;
        }
    }

    private record TestPayload(String data) implements Serializable {
    }

    private record TestDomainEventImpl(String getEventId,
                                       EventType getEventType,
                                       TestIdentifier aggregateId) implements DomainEvent {

        @Serial
        private static final long serialVersionUID = -6093081430975889755L;

        TestDomainEventImpl(String eventId, EventType eventType, String aggregateIdValue) {
            this(eventId, eventType, new TestIdentifier(aggregateIdValue));
        }

        @Override
        public Instant getOccurredAt() {
            return Instant.now();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <I extends Identifier<?>> I getAggregateId() {
            return (I) this.aggregateId;
        }

        @Override
        public <T extends Serializable> T getPayload() {
            return null;
        }
    }

    private static class TestDomainEvent extends AbstractDomainEvent<TestIdentifier, TestPayload> {

        @Serial
        private static final long serialVersionUID = 7337080134618463901L;

        TestDomainEvent(TestIdentifier aggregateId, TestPayload payload) {
            super(aggregateId, payload);
        }

        TestDomainEvent(TestIdentifier aggregateId, TestPayload payload, Clock clock) {
            super(aggregateId, payload, clock);
        }

        @Override
        public EventType getEventType() {
            return null;
        }
    }

    private static class TestDomainEventWithId extends AbstractDomainEvent<TestIdentifier, TestPayload> {

        @Serial
        private static final long serialVersionUID = 4225756878507348687L;

        private final String fixedEventId;

        TestDomainEventWithId(
                String fixedEventId,
                TestIdentifier aggregateId,
                TestPayload payload) {
            super(aggregateId, payload);
            this.fixedEventId = fixedEventId;
        }

        @Override
        public String getEventId() {
            return this.fixedEventId;
        }

        @Override
        public EventType getEventType() {
            return null;
        }
    }
}
