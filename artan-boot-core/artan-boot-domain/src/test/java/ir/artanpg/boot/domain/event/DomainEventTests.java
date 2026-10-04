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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link DomainEvent} and {@link AbstractDomainEvent}.
 *
 * <p>Covers the current contract: class-name-derived {@code getEventType()},
 * injectable {@link IdGenerator}, mutable metadata support, payload redaction
 * in {@code toString()} and full-attribute equality semantics.
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
    private static final String ERROR_PAYLOAD_NULL = "The payload cannot be null";
    private static final Instant FIXED_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final long FIXED_MILLIS = 1_700_000_000_000L;
    private static final Instant FIXED_INSTANT = Instant.ofEpochMilli(FIXED_MILLIS);

    // ========================================================================================
    // Constructors
    // ========================================================================================

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
            thenThrownBy(() -> new TestDomainEvent(nullAggregateId, payload))
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
            thenThrownBy(() -> new TestDomainEvent(aggregateId, nullPayload))
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
    @DisplayName("Constructor with IdGenerator")
    class ConstructorWithIdGenerator {

        @Test
        @DisplayName("should use injected id generator for eventId")
        void constructor_ShouldUseInjectedIdGenerator_WhenGeneratorIsProvided() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            IdGenerator sequential = IdGenerator.sequential();

            // when
            TestDomainEventWithGenerator event1 = new TestDomainEventWithGenerator(aggregateId, payload, sequential);
            TestDomainEventWithGenerator event2 = new TestDomainEventWithGenerator(aggregateId, payload, sequential);

            // then
            then(event1.getEventId()).as("First generated ID should be deterministic").isEqualTo("0");
            then(event2.getEventId()).as("Second generated ID should increment").isEqualTo("1");
        }

        @Test
        @DisplayName("should fall back to UUID generator when injected generator is null")
        void constructor_ShouldFallBackToUuid_WhenGeneratorIsNull() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);

            // when
            TestDomainEventWithGenerator event =
                    new TestDomainEventWithGenerator(aggregateId, payload, (Clock) null, null);

            // then — a UUID string is 36 chars with dashes
            then(event.getEventId())
                    .as("Null generator should fall back to UUID strategy")
                    .matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
        }

        @Test
        @DisplayName("should throw NullPointerException when generator returns null")
        void constructor_ShouldThrowNpe_WhenGeneratorReturnsNull() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            IdGenerator nullGenerator = () -> null;

            // when & then
            thenThrownBy(() -> new TestDomainEventWithGenerator(aggregateId, payload, nullGenerator))
                    .as("Null generated ID must be rejected")
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("should throw DomainException when generator returns blank ID")
        void constructor_ShouldThrowDomainException_WhenGeneratorReturnsBlank() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            IdGenerator blankGenerator = () -> "   ";

            // when & then
            thenThrownBy(() -> new TestDomainEventWithGenerator(aggregateId, payload, blankGenerator))
                    .as("Blank generated ID must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The generated eventId cannot be blank");
        }
    }

    // ========================================================================================
    // Simple getters
    // ========================================================================================

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
    @DisplayName("getEventType")
    class GetEventType {

        @Test
        @DisplayName("should derive event type from concrete class simple name")
        void getEventType_ShouldDeriveFromClassName_WhenNotOverridden() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            OrderShippedEvent event = new OrderShippedEvent(aggregateId, payload);

            // when
            EventType type = event.getEventType();

            // then
            then(type.getName())
                    .as("Default event type should be the class simple name")
                    .isEqualTo("OrderShippedEvent");
        }

        @Test
        @DisplayName("should return cached identical EventType instance")
        void getEventType_ShouldReturnCachedInstance_WhenCalledTwice() {
            // given
            OrderShippedEvent event = new OrderShippedEvent(
                    new TestIdentifier(AGGREGATE_ID_VALUE), new TestPayload(PAYLOAD_VALUE));

            // when
            EventType type1 = event.getEventType();
            EventType type2 = event.getEventType();

            // then
            then(type1).as("EventType.valueOf caches by name").isSameAs(type2);
        }

        @Test
        @DisplayName("should allow subclass to override event type explicitly")
        void getEventType_ShouldUseOverride_WhenSubclassProvidesStableName() {
            // given
            StableTypeEvent event = new StableTypeEvent(
                    new TestIdentifier(AGGREGATE_ID_VALUE), new TestPayload(PAYLOAD_VALUE));

            // when & then
            then(event.getEventType().getName())
                    .as("Explicit override wins over class-name derivation")
                    .isEqualTo("order.shipped.v2");
        }

        @Test
        @DisplayName("should make isOfType work with derived event type")
        void isOfType_ShouldWorkWithDerivedType_WhenCalled() {
            // given
            OrderShippedEvent event = new OrderShippedEvent(
                    new TestIdentifier(AGGREGATE_ID_VALUE), new TestPayload(PAYLOAD_VALUE));

            // when & then
            then(event.isOfType(EventType.valueOf("OrderShippedEvent")))
                    .as("Event should report itself as its derived type")
                    .isTrue();
            then(event.isOfType(EventType.valueOf("SomeOtherEvent")))
                    .as("Event should not report unrelated types")
                    .isFalse();
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
            TestDomainEvent event = new TestDomainEvent(aggregateId, payload, fixedClock);

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

        @Test
        @DisplayName("should accept non-Serializable payload type")
        void getPayload_ShouldAcceptNonSerializablePayload_WhenProvided() {
            // given — no Serializable bound on the payload generic anymore
            NonSerializablePayload payload = new NonSerializablePayload("secret");

            // when
            EventWithNonSerializablePayload event = new EventWithNonSerializablePayload(
                    new TestIdentifier(AGGREGATE_ID_VALUE), payload);

            // then
            then(event.getPayload())
                    .as("Payload without Serializable bound must be supported")
                    .isEqualTo(payload);
        }
    }

    // ========================================================================================
    // Metadata
    // ========================================================================================

    @Nested
    @DisplayName("Metadata")
    class MetadataSupport {

        @Test
        @DisplayName("should return empty unmodifiable map by default")
        void getMetadata_ShouldReturnEmptyMap_WhenNothingSet() {
            // given
            TestDomainEvent event = createEvent();

            // when
            Map<String, Object> metadata = event.getMetadata();

            // then
            then(metadata).as("Default metadata should be empty").isEmpty();
            thenThrownBy(() -> metadata.put("k", "v"))
                    .as("Returned snapshot must be unmodifiable")
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should store and return a metadata value")
        void setMetadataValue_ShouldStoreValue_WhenKeyIsValid() {
            // given
            TestDomainEvent event = createEvent();

            // when
            event.setMetadataValue("correlationId", "corr-42");

            // then
            then(event.getMetadata())
                    .as("Value should be present in snapshot")
                    .containsEntry("correlationId", "corr-42");
            then(event.getMetadataValue("correlationId"))
                    .as("Convenience accessor should find the value")
                    .isEqualTo("corr-42");
            then(event.hasMetadata("correlationId"))
                    .as("hasMetadata should report presence")
                    .isTrue();
        }

        @Test
        @DisplayName("should return previous value on overwrite")
        void setMetadataValue_ShouldReturnPrevious_WhenKeyAlreadyPresent() {
            // given
            TestDomainEvent event = createEvent();
            event.setMetadataValue("tenant", "acme");

            // when
            Object previous = event.setMetadataValue("tenant", "globex");

            // then
            then(previous).as("Previous value should be returned").isEqualTo("acme");
            then(event.getMetadataValue("tenant")).isEqualTo("globex");
        }

        @Test
        @DisplayName("should remove entry when value is null")
        void setMetadataValue_ShouldRemoveEntry_WhenValueIsNull() {
            // given
            TestDomainEvent event = createEvent();
            event.setMetadataValue("temp", "value");

            // when
            Object removed = event.setMetadataValue("temp", null);

            // then
            then(removed).as("Null value acts as removal and returns old value").isEqualTo("value");
            then(event.hasMetadata("temp")).as("Key should be gone").isFalse();
        }

        @Test
        @DisplayName("should reject null or blank keys")
        void setMetadataValue_ShouldThrow_WhenKeyIsInvalid() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            thenThrownBy(() -> event.setMetadataValue(null, "v"))
                    .as("Null key must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The metadata key cannot be null or blank");
            thenThrownBy(() -> event.setMetadataValue("  ", "v"))
                    .as("Blank key must be rejected")
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("should copy all entries via putAllMetadata")
        void putAllMetadata_ShouldCopyEntries_WhenMapProvided() {
            // given
            TestDomainEvent event = createEvent();

            // when
            event.putAllMetadata(Map.of("a", 1, "b", "two"));

            // then
            then(event.getMetadata())
                    .as("All entries should be copied")
                    .containsOnlyKeys("a", "b")
                    .containsEntry("a", 1)
                    .containsEntry("b", "two");
        }

        @Test
        @DisplayName("should ignore null or empty map in putAllMetadata")
        void putAllMetadata_ShouldBeNoOp_WhenMapNullOrEmpty() {
            // given
            TestDomainEvent event = createEvent();

            // when
            event.putAllMetadata(null);
            event.putAllMetadata(Map.of());

            // then
            then(event.getMetadata()).as("Nothing should change").isEmpty();
        }

        @Test
        @DisplayName("should remove single entry via removeMetadata")
        void removeMetadata_ShouldRemoveEntry_WhenKeyPresent() {
            // given
            TestDomainEvent event = createEvent();
            event.setMetadataValue("x", 1);

            // when
            Object removed = event.removeMetadata("x");

            // then
            then(removed).as("Removed value should be returned").isEqualTo(1);
            then(event.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("should be no-op for null/absent key in removeMetadata")
        void removeMetadata_ShouldReturnNull_WhenKeyNullOrAbsent() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event.removeMetadata(null)).as("Null key is a safe no-op").isNull();
            then(event.removeMetadata("missing")).as("Absent key returns null").isNull();
        }

        @Test
        @DisplayName("should drop all entries via clearMetadata")
        void clearMetadata_ShouldRemoveEverything_WhenCalled() {
            // given
            TestDomainEvent event = createEvent();
            event.putAllMetadata(Map.of("a", 1, "b", 2));

            // when
            event.clearMetadata();

            // then
            then(event.getMetadata()).as("Metadata should be empty after clear").isEmpty();
        }

        @Test
        @DisplayName("should return independent snapshots on repeated calls")
        void getMetadata_ShouldReturnIndependentSnapshots_WhenCalledMultipleTimes() {
            // given
            TestDomainEvent event = createEvent();
            event.setMetadataValue("k", "v");

            // when
            Map<String, Object> first = event.getMetadata();
            event.setMetadataValue("k2", "v2");
            Map<String, Object> second = event.getMetadata();

            // then
            then(first).as("Earlier snapshot must not see later writes").containsOnlyKeys("k");
            then(second).as("Later snapshot reflects current state").containsOnlyKeys("k", "k2");
        }

        @Test
        @DisplayName("interface defaults should handle null key safely")
        void interfaceDefaults_ShouldHandleNullKey_WhenQueried() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);
            TestDomainEventImpl event = new TestDomainEventImpl(EVENT_ID, eventType, AGGREGATE_ID_VALUE);

            // when & then
            then(event.getMetadataValue(null)).as("Null key lookup returns null").isNull();
            then(event.hasMetadata(null)).as("Null key presence is false").isFalse();
            then(event.getMetadata()).as("Default metadata is empty").isEmpty();
        }

        @Test
        @DisplayName("metadata mutation should be thread-safe")
        void setMetadataValue_ShouldBeThreadSafe_WhenWrittenConcurrently() throws InterruptedException {
            // given
            TestDomainEvent event = createEvent();
            int threads = 8;
            int perThread = 50;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);

            // when
            for (int t = 0; t < threads; t++) {
                final int tid = t;
                pool.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            event.setMetadataValue("key-" + tid + "-" + i, i);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            then(done.await(10, TimeUnit.SECONDS)).as("Writers should finish").isTrue();
            pool.shutdownNow();

            // then
            then(event.getMetadata())
                    .as("All concurrent writes should be visible without corruption")
                    .hasSize(threads * perThread);
        }

        @Test
        @DisplayName("metadata changes must not affect equals/hashCode")
        void equalsAndHashCode_ShouldIgnoreMetadata_WhenMetadataChanges() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            // A fixed clock is required: equals/hashCode include occurredAt,
            // so two events created with the system clock would differ by milliseconds.
            Clock fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));
            TestDomainEventWithGenerator event1 =
                    new TestDomainEventWithGenerator(aggregateId, payload, fixedClock, () -> "same-id");
            TestDomainEventWithGenerator event2 =
                    new TestDomainEventWithGenerator(aggregateId, payload, fixedClock, () -> "same-id");

            // when
            event1.setMetadataValue("correlationId", "one");
            event2.setMetadataValue("correlationId", "two");

            // then
            then(event1).as("Metadata is not part of identity").isEqualTo(event2);
            then(event1.hashCode()).isEqualTo(event2.hashCode());
        }
    }

    // ========================================================================================
    // equals / hashCode
    // ========================================================================================

    @Nested
    @DisplayName("equals")
    class Equals {

        @Test
        @DisplayName("should be equal when all core attributes are same")
        void equals_ShouldBeEqual_WhenCoreAttributesAreSame() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            TestDomainEventWithId event1 = new TestDomainEventWithId(EVENT_ID, aggregateId, payload, FIXED_MILLIS);
            TestDomainEventWithId event2 = new TestDomainEventWithId(EVENT_ID, aggregateId, payload, FIXED_MILLIS);

            // when & then
            then(event1)
                    .as("Events with same core attributes should be equal")
                    .isEqualTo(event2);
        }

        @Test
        @DisplayName("should not be equal when event IDs differ")
        void equals_ShouldNotBeEqual_WhenEventIdsDiffer() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            TestDomainEventWithId event1 = new TestDomainEventWithId("id-1", aggregateId, payload, FIXED_MILLIS);
            TestDomainEventWithId event2 = new TestDomainEventWithId("id-2", aggregateId, payload, FIXED_MILLIS);

            // when & then
            then(event1)
                    .as("Events with different IDs should not be equal")
                    .isNotEqualTo(event2);
        }

        @Test
        @DisplayName("should not be equal when payloads differ")
        void equals_ShouldNotBeEqual_WhenPayloadsDiffer() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestDomainEventWithId event1 =
                    new TestDomainEventWithId(EVENT_ID, aggregateId, new TestPayload("a"), FIXED_MILLIS);
            TestDomainEventWithId event2 =
                    new TestDomainEventWithId(EVENT_ID, aggregateId, new TestPayload("b"), FIXED_MILLIS);

            // when & then
            then(event1).as("Different payloads mean different events").isNotEqualTo(event2);
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

        @Test
        @DisplayName("should be symmetric between two instances")
        void equals_ShouldBeSymmetric_WhenComparedBothWays() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            TestDomainEventWithId event1 = new TestDomainEventWithId(EVENT_ID, aggregateId, payload, FIXED_MILLIS);
            TestDomainEventWithId event2 = new TestDomainEventWithId(EVENT_ID, aggregateId, payload, FIXED_MILLIS);

            // when & then
            then(event1.equals(event2) == event2.equals(event1))
                    .as("Equality must be symmetric")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("hashCode")
    class HashCode {

        @Test
        @DisplayName("should return same hash code for equal events")
        void hashCode_ShouldReturnSameHashCode_WhenEventsAreEqual() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            TestDomainEventWithId event1 = new TestDomainEventWithId(EVENT_ID, aggregateId, payload, FIXED_MILLIS);
            TestDomainEventWithId event2 = new TestDomainEventWithId(EVENT_ID, aggregateId, payload, FIXED_MILLIS);

            // when & then
            then(event1.hashCode())
                    .as("Equal events should have same hash code")
                    .isEqualTo(event2.hashCode());
        }

        @Test
        @DisplayName("should behave correctly inside a HashSet")
        void hashCode_ShouldDeduplicateEqualEvents_WhenUsedInHashSet() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestPayload payload = new TestPayload(PAYLOAD_VALUE);
            TestDomainEventWithId event1 = new TestDomainEventWithId(EVENT_ID, aggregateId, payload, FIXED_MILLIS);
            TestDomainEventWithId event2 = new TestDomainEventWithId(EVENT_ID, aggregateId, payload, FIXED_MILLIS);

            // when
            var set = new java.util.HashSet<>(List.of(event1, event2));

            // then
            then(set).as("Equal events collapse into one entry").hasSize(1);
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
    @DisplayName("isSameEventAs")
    class IsSameEventAs {

        @Test
        @DisplayName("should return true for events sharing the same eventId")
        void isSameEventAs_ShouldReturnTrue_WhenEventIdsMatch() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event.isSameEventAs(createEventWithSameId(event.getEventId())))
                    .as("Same eventId means same occurrence")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false for events with different eventIds")
        void isSameEventAs_ShouldReturnFalse_WhenEventIdsDiffer() {
            // given
            TestDomainEvent event1 = createEvent();
            TestDomainEvent event2 = createEvent();

            // when & then
            then(event1.isSameEventAs(event2))
                    .as("Different eventIds are different occurrences")
                    .isFalse();
        }

        @Test
        @DisplayName("should return false for null")
        void isSameEventAs_ShouldReturnFalse_WhenOtherIsNull() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event.isSameEventAs(null)).as("Null must be handled gracefully").isFalse();
        }

        @Test
        @DisplayName("should deduplicate by eventId even when payloads differ")
        void isSameEventAs_ShouldIgnorePayloadDifference_WhenIdsMatch() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
            TestDomainEventWithId event1 =
                    new TestDomainEventWithId(EVENT_ID, aggregateId, new TestPayload("a"), FIXED_MILLIS);
            TestDomainEventWithId event2 =
                    new TestDomainEventWithId(EVENT_ID, aggregateId, new TestPayload("b"), FIXED_MILLIS);

            // when & then
            then(event1.isSameEventAs(event2))
                    .as("Dedup uses eventId only")
                    .isTrue();
            then(event1).as("equals still considers payload").isNotEqualTo(event2);
        }
    }

    // ========================================================================================
    // toString / redaction
    // ========================================================================================

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

        @Test
        @DisplayName("should render raw payload when policy is NONE")
        void toString_ShouldRenderPayload_WhenPolicyIsNone() {
            // given
            TestDomainEvent event = createEvent();

            // when
            String result = event.toString();

            // then
            then(result)
                    .as("Default policy renders the payload content")
                    .contains(PAYLOAD_VALUE);
        }

        @Test
        @DisplayName("should mask payload when policy is MASKED")
        void toString_ShouldMaskPayload_WhenPolicyIsMasked() {
            // given
            SensitiveEvent event = new SensitiveEvent(
                    new TestIdentifier(AGGREGATE_ID_VALUE), new TestPayload("credit-card-1234"));

            // when
            String result = event.toString();

            // then
            then(result)
                    .as("Sensitive payload must not leak into logs")
                    .doesNotContain("credit-card-1234")
                    .contains(PayloadRedactionPolicy.REDACTED_PLACEHOLDER);
        }

        @Test
        @DisplayName("should apply custom redaction policy")
        void toString_ShouldApplyCustomPolicy_WhenOverridden() {
            // given
            CustomRedactedEvent event = new CustomRedactedEvent(
                    new TestIdentifier(AGGREGATE_ID_VALUE), new TestPayload("abcdefgh"));

            // when
            String result = event.toString();

            // then
            then(result)
                    .as("Custom policy shows only the last four characters")
                    .contains("payload=****efgh")
                    .doesNotContain("abcdefgh");
        }
    }

    // ========================================================================================
    // Interface contracts
    // ========================================================================================

    @Nested
    @DisplayName("DomainEvent interface")
    class InterfaceContracts {

        @Test
        @DisplayName("should be instance of DomainEvent")
        void event_ShouldBeInstanceOfDomainEvent_WhenCreated() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event)
                    .as("Should be a DomainEvent")
                    .isInstanceOf(DomainEvent.class);
        }

        @Test
        @DisplayName("should remain ValueObject-compatible (Serializable via marker)")
        void event_ShouldBeSerializable_WhenRequiredByValueObject() {
            // given
            TestDomainEvent event = createEvent();

            // when & then
            then(event)
                    .as("ValueObject extends Serializable, so events remain serializable objects")
                    .isInstanceOf(Serializable.class);
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

    // ========================================================================================
    // Helper Methods
    // ========================================================================================

    private TestDomainEvent createEvent() {
        TestIdentifier aggregateId = new TestIdentifier(AGGREGATE_ID_VALUE);
        TestPayload payload = new TestPayload(PAYLOAD_VALUE);
        return new TestDomainEvent(aggregateId, payload);
    }

    private TestDomainEventWithId createEventWithSameId(String eventId) {
        return new TestDomainEventWithId(
                eventId, new TestIdentifier(AGGREGATE_ID_VALUE), new TestPayload(PAYLOAD_VALUE), FIXED_MILLIS);
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

    /** Payload type deliberately NOT implementing Serializable. */
    private record NonSerializablePayload(String secret) {
    }

    private record TestDomainEventImpl(String getEventId,
                                       EventType getEventType,
                                       TestIdentifier aggregateId) implements DomainEvent<TestIdentifier, TestPayload> {

        TestDomainEventImpl(String eventId, EventType eventType, String aggregateIdValue) {
            this(eventId, eventType, new TestIdentifier(aggregateIdValue));
        }

        @Override
        public Instant getOccurredAt() {
            return Instant.now();
        }

        @Override
        public TestIdentifier getAggregateId() {
            return this.aggregateId;
        }

        @Override
        public TestPayload getPayload() {
            return null;
        }
    }

    /** Relies on the inherited class-name-derived getEventType(). */
    private static class TestDomainEvent extends AbstractDomainEvent<TestIdentifier, TestPayload> {

        TestDomainEvent(TestIdentifier aggregateId, TestPayload payload) {
            super(aggregateId, payload);
        }

        TestDomainEvent(TestIdentifier aggregateId, TestPayload payload, Clock clock) {
            super(aggregateId, payload, clock);
        }
    }

    /** Exercises the full constructor with an injected IdGenerator. */
    private static class TestDomainEventWithGenerator extends AbstractDomainEvent<TestIdentifier, TestPayload> {

        TestDomainEventWithGenerator(TestIdentifier aggregateId, TestPayload payload, IdGenerator idGenerator) {
            super(aggregateId, payload, null, idGenerator);
        }

        TestDomainEventWithGenerator(TestIdentifier aggregateId, TestPayload payload,
                                     Clock clock, IdGenerator idGenerator) {
            super(aggregateId, payload, clock, idGenerator);
        }
    }

    /** Fixed-attribute event used for deterministic equality tests. */
    private static class TestDomainEventWithId extends AbstractDomainEvent<TestIdentifier, TestPayload> {

        private final String fixedEventId;
        private final long fixedOccurredAtMillis;

        TestDomainEventWithId(String fixedEventId,
                              TestIdentifier aggregateId,
                              TestPayload payload,
                              long occurredAtMillis) {
            super(aggregateId, payload,
                    Clock.fixed(Instant.ofEpochMilli(occurredAtMillis), ZoneId.of("UTC")),
                    () -> fixedEventId);
            this.fixedEventId = fixedEventId;
            this.fixedOccurredAtMillis = occurredAtMillis;
        }

        @Override
        public String getEventId() {
            return this.fixedEventId;
        }

        @Override
        public Instant getOccurredAt() {
            return Instant.ofEpochMilli(this.fixedOccurredAtMillis);
        }
    }

    /** Default derived-type event (no overrides at all). */
    private static class OrderShippedEvent extends AbstractDomainEvent<TestIdentifier, TestPayload> {

        OrderShippedEvent(TestIdentifier aggregateId, TestPayload payload) {
            super(aggregateId, payload);
        }
    }

    /** Event overriding getEventType with a stable wire name. */
    private static class StableTypeEvent extends AbstractDomainEvent<TestIdentifier, TestPayload> {

        StableTypeEvent(TestIdentifier aggregateId, TestPayload payload) {
            super(aggregateId, payload);
        }

        @Override
        public EventType getEventType() {
            return EventType.valueOf("order.shipped.v2");
        }
    }

    /** Event masking its payload entirely. */
    private static class SensitiveEvent extends AbstractDomainEvent<TestIdentifier, TestPayload> {

        SensitiveEvent(TestIdentifier aggregateId, TestPayload payload) {
            super(aggregateId, payload);
        }

        @Override
        protected PayloadRedactionPolicy redactionPolicy() {
            return PayloadRedactionPolicy.MASKED;
        }
    }

    /** Event with a partial-masking custom policy. */
    private static class CustomRedactedEvent extends AbstractDomainEvent<TestIdentifier, TestPayload> {

        CustomRedactedEvent(TestIdentifier aggregateId, TestPayload payload) {
            super(aggregateId, payload);
        }

        @Override
        protected PayloadRedactionPolicy redactionPolicy() {
            return payload -> {
                if (!(payload instanceof TestPayload tp)) return PayloadRedactionPolicy.REDACTED_PLACEHOLDER;
                String data = tp.data();
                if (data.length() <= 4) return PayloadRedactionPolicy.REDACTED_PLACEHOLDER;
                return "****" + data.substring(data.length() - 4);
            };
        }
    }

    /** Event carrying a non-Serializable payload (allowed since the bound was dropped). */
    private static class EventWithNonSerializablePayload
            extends AbstractDomainEvent<TestIdentifier, NonSerializablePayload> {

        EventWithNonSerializablePayload(TestIdentifier aggregateId, NonSerializablePayload payload) {
            super(aggregateId, payload);
        }
    }
}
