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
import ir.artanpg.boot.domain.model.IdGenerator;
import ir.artanpg.boot.domain.model.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link DomainEvent}.
 *
 * @author Mohammad Yazdian
 */
class DomainEventTests {

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Constructor should initialize fields when valid arguments provided")
        void constructor_ShouldInitializeFields_WhenValidArgumentsProvided() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            String payload = "payload";

            // when
            TestEvent event = new TestEvent(aggregateId, payload);

            // then
            then(event.getAggregateId()).isSameAs(aggregateId);
            then(event.getPayload()).isEqualTo(payload);
            then(event.getEventId()).isNotBlank();
            then(event.getOccurredAt()).isNotNull();
        }

        @Test
        @DisplayName("Constructor should throw DomainEventException when aggregateId is null")
        void constructor_ShouldThrowDomainEventException_WhenAggregateIdIsNull() {
            // given
            String payload = "payload";

            // when & then
            thenThrownBy(() -> new TestEvent(null, payload))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The aggregateId cannot be null");
        }

        @Test
        @DisplayName("Constructor should throw DomainEventException when payload is null")
        void constructor_ShouldThrowDomainEventException_WhenPayloadIsNull() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);

            // when & then
            thenThrownBy(() -> new TestEvent(aggregateId, null))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The payload cannot be null");
        }

        @Test
        @DisplayName("Constructor should use provided clock when clock is not null")
        void constructor_ShouldUseProvidedClock_WhenClockIsNotNull() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            String payload = "payload";
            Instant fixedInstant = Instant.parse("2023-01-01T00:00:00Z");
            Clock clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

            // when
            TestEvent event = new TestEvent(aggregateId, payload, clock);

            // then
            then(event.getOccurredAt()).isEqualTo(fixedInstant);
        }

        @Test
        @DisplayName("Constructor should use default clock when clock is null")
        void constructor_ShouldUseDefaultClock_WhenClockIsNull() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            String payload = "payload";
            Instant before = Instant.now();

            // when
            TestEvent event = new TestEvent(aggregateId, payload, (Clock) null);
            Instant after = Instant.now();

            // then
            then(event.getOccurredAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("Constructor should use provided id generator when id generator is not null")
        void constructor_ShouldUseProvidedIdGenerator_WhenIdGeneratorIsNotNull() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            String payload = "payload";
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("custom-id");

            // when
            TestEvent event = new TestEvent(aggregateId, payload, null, idGenerator);

            // then
            then(event.getEventId()).isEqualTo("custom-id");
        }

        @Test
        @DisplayName("Constructor should use default id generator when id generator is null")
        void constructor_ShouldUseDefaultIdGenerator_WhenIdGeneratorIsNull() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            String payload = "payload";

            // when
            TestEvent event = new TestEvent(aggregateId, payload, null, null);

            // then
            then(event.getEventId()).isNotBlank();
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Constructor should throw DomainEventException when generated event id is null")
        void constructor_ShouldThrowDomainEventException_WhenGeneratedEventIdIsNull() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            String payload = "payload";
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn(null);

            // when & then
            thenThrownBy(() -> new TestEvent(aggregateId, payload, null, idGenerator))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The generated eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Constructor should throw DomainEventException when generated event id is blank")
        void constructor_ShouldThrowDomainEventException_WhenGeneratedEventIdIsBlank() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            String payload = "payload";
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("   ");

            // when & then
            thenThrownBy(() -> new TestEvent(aggregateId, payload, null, idGenerator))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The generated eventId cannot be null or blank");
        }

        @Test
        @DisplayName("Constructor should throw DomainEventException when generated event id is empty")
        void constructor_ShouldThrowDomainEventException_WhenGeneratedEventIdIsEmpty() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            String payload = "payload";
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("");

            // when & then
            thenThrownBy(() -> new TestEvent(aggregateId, payload, null, idGenerator))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The generated eventId cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("eventType")
    class EventTypeTests {

        @Test
        @DisplayName("eventType should return a canonical type interned through the shared registry")
        void eventType_ShouldReturnCanonicalTypeFromSharedRegistry() {
            // given
            TestEvent event = new TestEvent(mock(Identifier.class), "payload");

            // when
            EventType result = event.eventType();

            // then
            then(result).isNotNull();
            then(result.getName()).isEqualTo("TestEvent");
            // Interning guarantee: same call resolves to the very same cached instance.
            then(event.eventType()).isSameAs(result);
            then(EventTypeRegistry.shared().valueOfName("TestEvent")).isSameAs(result);
        }
    }

    @Nested
    @DisplayName("metadata")
    class MetadataTests {

        @Test
        @DisplayName("metadata should return empty map when no metadata")
        void metadata_ShouldReturnEmptyMap_WhenNoMetadata() {
            // given
            TestEvent event = new TestEvent(mock(Identifier.class), "payload");

            // when
            Map<String, Object> result = event.getMetadata();

            // then
            then(result).isEmpty();
        }

        @Test
        @DisplayName("metadata should return unmodifiable map when metadata exists")
        void metadata_ShouldReturnUnmodifiableMap_WhenMetadataExists() {
            // given
            TestEvent event = new TestEvent(mock(Identifier.class), "payload", Map.of("key", "value"));

            // when
            Map<String, Object> result = event.getMetadata();

            // then
            then(result).containsEntry("key", "value");
            thenThrownBy(() -> result.put("newKey", "newValue")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("metadata should not leak changes made to the source map after construction")
        void metadata_ShouldNotLeakSourceMapChanges_AfterConstruction() {
            // given
            Map<String, Object> source = new java.util.HashMap<>();
            source.put("key", "value");
            TestEvent event = new TestEvent(mock(Identifier.class), "payload", source);

            // when
            source.put("lateKey", "lateValue");

            // then
            then(event.getMetadata()).containsEntry("key", "value").doesNotContainKey("lateKey");
        }

        @Test
        @DisplayName("constructor should drop null metadata values and keep non-null ones")
        void constructor_ShouldDropNullMetadataValues() {
            // given
            Map<String, Object> source = new java.util.HashMap<>();
            source.put("key", "value");
            source.put("nullKey", null);

            // when
            TestEvent event = new TestEvent(mock(Identifier.class), "payload", source);

            // then
            then(event.getMetadata()).containsEntry("key", "value").doesNotContainKey("nullKey");
        }

        @Test
        @DisplayName("constructor should return empty map when source is empty (not null)")
        void constructor_ShouldReturnEmptyMap_WhenSourceIsEmpty() {
            // given
            Map<String, Object> emptySource = Collections.emptyMap();

            // when
            TestEvent event = new TestEvent(mock(Identifier.class), "payload", emptySource);

            // then
            then(event.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("constructor should throw DomainEventException when a metadata key is null")
        void constructor_ShouldThrowDomainEventException_WhenMetadataKeyIsNull() {
            // given
            Map<String, Object> source = new java.util.HashMap<>();
            source.put(null, "value");

            // when & then
            thenThrownBy(() -> new TestEvent(mock(Identifier.class), "payload", source))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The metadata key cannot be null or blank");
        }

        @Test
        @DisplayName("constructor should return empty map when all metadata values are null")
        void constructor_ShouldReturnEmptyMap_WhenAllMetadataValuesAreNull() {
            // given
            Map<String, Object> source = new java.util.HashMap<>();
            source.put("key1", null);
            source.put("key2", null);
            source.put("key3", null);

            // when
            TestEvent event = new TestEvent(mock(Identifier.class), "payload", source);

            // then
            then(event.getMetadata()).isEmpty();
        }

        @Test
        @DisplayName("constructor should keep non-null values and drop null values when mixed")
        void constructor_ShouldKeepNonNullValuesAndDropNullValues_WhenMixed() {
            // given
            Map<String, Object> source = new java.util.HashMap<>();
            source.put("keep1", "value1");
            source.put("drop1", null);
            source.put("keep2", 42);
            source.put("drop2", null);

            // when
            TestEvent event = new TestEvent(mock(Identifier.class), "payload", source);

            // then
            then(event.getMetadata()).hasSize(2);
            then(event.getMetadata()).containsEntry("keep1", "value1");
            then(event.getMetadata()).containsEntry("keep2", 42);
            then(event.getMetadata()).doesNotContainKey("drop1");
            then(event.getMetadata()).doesNotContainKey("drop2");
        }

        @Test
        @DisplayName("constructor should throw DomainEventException when a metadata key is blank")
        void constructor_ShouldThrowDomainEventException_WhenMetadataKeyIsBlank() {
            // given
            Map<String, Object> source = new java.util.HashMap<>();
            source.put("  ", "value");

            // when & then
            thenThrownBy(() -> new TestEvent(mock(Identifier.class), "payload", source))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The metadata key cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("redactionPolicy")
    class RedactionPolicyTests {

        @Test
        @DisplayName("redactionPolicy should return NONE")
        void redactionPolicy_ShouldReturnNone() {
            // given
            TestEvent event = new TestEvent(mock(Identifier.class), "payload");

            // when
            PayloadRedactionPolicy result = event.getRedactionPolicy();

            // then
            then(result).isEqualTo(PayloadRedactionPolicy.NONE);
        }
    }

    @Nested
    @DisplayName("isSameEventAs")
    class IsSameEventAsTests {

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("isSameEventAs should return false when other is null")
        void isSameEventAs_ShouldReturnFalse_WhenOtherIsNull() {
            // given
            TestEvent event = new TestEvent(mock(Identifier.class), "payload");

            // when
            boolean result = event.isSameEventAs(null);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("isSameEventAs should return true when event ids are equal")
        void isSameEventAs_ShouldReturnTrue_WhenEventIdsAreEqual() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("same-id");

            TestEvent event1 = new TestEvent(aggregateId, "payload1", null, idGenerator);
            TestEvent event2 = new TestEvent(aggregateId, "payload2", null, idGenerator);

            // when
            boolean result = event1.isSameEventAs(event2);

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("isSameEventAs should return false when event ids are not equal")
        void isSameEventAs_ShouldReturnFalse_WhenEventIdsAreNotEqual() {
            // given
            TestEvent event1 = new TestEvent(mock(Identifier.class), "payload");
            TestEvent event2 = new TestEvent(mock(Identifier.class), "payload");

            // when
            boolean result = event1.isSameEventAs(event2);

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("equals() & hashCode()")
    class EqualsAndHashCodeTests {

        @SuppressWarnings({"EqualsWithItself", "ConstantValue"})
        @Test
        @DisplayName("equals should return true when same instance")
        void equals_ShouldReturnTrue_WhenSameInstance() {
            // given
            TestEvent event = new TestEvent(mock(Identifier.class), "payload");

            // when
            boolean result = event.equals(event);

            // then
            then(result).isTrue();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("equals should return false when other is null")
        void equals_ShouldReturnFalse_WhenOtherIsNull() {
            // given
            TestEvent event = new TestEvent(mock(Identifier.class), "payload");

            // when
            boolean result = event.equals(null);

            // then
            then(result).isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("equals should return false when other is not DomainEvent")
        void equals_ShouldReturnFalse_WhenOtherIsNotDomainEvent() {
            // given
            TestEvent event = new TestEvent(mock(Identifier.class), "payload");

            // when
            boolean result = event.equals("not a domain event");

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("equals should return true when all fields are equal")
        void equals_ShouldReturnTrue_WhenAllFieldsAreEqual() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            Instant occurredAt = Instant.now();
            Clock clock = Clock.fixed(occurredAt, ZoneId.of("UTC"));
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("same-id");

            TestEvent event1 = new TestEvent(aggregateId, "payload", clock, idGenerator);
            TestEvent event2 = new TestEvent(aggregateId, "payload", clock, idGenerator);

            // when
            boolean result = event1.equals(event2);

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("equals should return false when event id differs")
        void equals_ShouldReturnFalse_WhenEventIdDiffers() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            TestEvent event1 = new TestEvent(aggregateId, "payload");
            TestEvent event2 = new TestEvent(aggregateId, "payload");

            // when
            boolean result = event1.equals(event2);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("equals should return false when occurred at differs")
        void equals_ShouldReturnFalse_WhenOccurredAtDiffers() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("same-id");

            Clock fixed = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
            Clock utc = Clock.fixed(Instant.now().plusSeconds(1), ZoneId.of("UTC"));

            TestEvent event1 = new TestEvent(aggregateId, "payload", fixed, idGenerator);
            TestEvent event2 = new TestEvent(aggregateId, "payload", utc, idGenerator);

            // when
            boolean result = event1.equals(event2);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("equals should return false when payload differs")
        void equals_ShouldReturnFalse_WhenPayloadDiffers() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            Instant occurredAt = Instant.now();
            Clock clock = Clock.fixed(occurredAt, ZoneId.of("UTC"));
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("same-id");

            TestEvent event1 = new TestEvent(aggregateId, "payload1", clock, idGenerator);
            TestEvent event2 = new TestEvent(aggregateId, "payload2", clock, idGenerator);

            // when
            boolean result = event1.equals(event2);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("equals should return false when aggregate id differs")
        void equals_ShouldReturnFalse_WhenAggregateIdDiffers() {
            // given
            Identifier<?> aggregateId1 = mock(Identifier.class);
            Identifier<?> aggregateId2 = mock(Identifier.class);
            Instant occurredAt = Instant.now();
            Clock clock = Clock.fixed(occurredAt, ZoneId.of("UTC"));
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("same-id");

            TestEvent event1 = new TestEvent(aggregateId1, "payload", clock, idGenerator);
            TestEvent event2 = new TestEvent(aggregateId2, "payload", clock, idGenerator);

            // when
            boolean result = event1.equals(event2);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("hashCode should return same hash when objects are equal")
        void hashCode_ShouldReturnSameHash_WhenObjectsAreEqual() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            Instant occurredAt = Instant.now();
            Clock clock = Clock.fixed(occurredAt, ZoneId.of("UTC"));
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("same-id");

            TestEvent event1 = new TestEvent(aggregateId, "payload", clock, idGenerator);
            TestEvent event2 = new TestEvent(aggregateId, "payload", clock, idGenerator);

            // when
            int hash1 = event1.hashCode();
            int hash2 = event2.hashCode();

            // then
            then(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("equals should return false when events are of different concrete classes")
        void equals_ShouldReturnFalse_WhenDifferentConcreteClasses() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            Instant occurredAt = Instant.parse("2023-01-01T00:00:00Z");
            Clock clock = Clock.fixed(occurredAt, ZoneId.of("UTC"));
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("same-id");

            TestEvent event1 = new TestEvent(aggregateId, "payload", clock, idGenerator);
            OtherTestEvent event2 = new OtherTestEvent(aggregateId, "payload", clock, idGenerator);

            // when & then — same core state, different event kind
            then(event1).isNotEqualTo(event2);
            then(event2).isNotEqualTo(event1);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("toString should return formatted string")
        void toString_ShouldReturnFormattedString() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            given(aggregateId.toString()).willReturn("agg-id");
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("event-id");
            Instant occurredAt = Instant.parse("2023-01-01T00:00:00Z");
            Clock clock = Clock.fixed(occurredAt, ZoneId.of("UTC"));

            TestEvent event = new TestEvent(aggregateId, "payload", clock, idGenerator);

            // when
            String result = event.toString();

            // then
            then(result).contains("eventId='event-id'")
                    .contains("occurredAt=2023-01-01T00:00:00Z")
                    .contains("payload=payload")
                    .contains("aggregateId=agg-id");
        }
    }

    @Nested
    @DisplayName("isSameOccurrenceAs")
    class IsSameOccurrenceAsTests {

        @Test
        @DisplayName("isSameOccurrenceAs should return false when other is null")
        void isSameOccurrenceAs_ShouldReturnFalse_WhenOtherIsNull() {
            // given
            TestEvent event = new TestEvent(mock(Identifier.class), "payload");

            // when & then
            then(event.isSameOccurrenceAs(null)).isFalse();
        }

        @Test
        @DisplayName("isSameOccurrenceAs should return true when event id and aggregate id match")
        void isSameOccurrenceAs_ShouldReturnTrue_WhenEventIdAndAggregateIdMatch() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("same-id");

            TestEvent event1 = new TestEvent(aggregateId, "payload1", null, idGenerator);
            TestEvent event2 = new TestEvent(aggregateId, "payload2", null, idGenerator);

            // when & then
            then(event1.isSameOccurrenceAs(event2)).isTrue();
        }

        @Test
        @DisplayName("isSameOccurrenceAs should return false when aggregate ids differ even with same event id")
        void isSameOccurrenceAs_ShouldReturnFalse_WhenAggregateIdsDiffer() {
            // given
            IdGenerator idGenerator = mock(IdGenerator.class);
            given(idGenerator.nextId()).willReturn("same-id");

            TestEvent event1 = new TestEvent(mock(Identifier.class), "payload", null, idGenerator);
            TestEvent event2 = new TestEvent(mock(Identifier.class), "payload", null, idGenerator);

            // when & then
            then(event1.isSameOccurrenceAs(event2)).isFalse();
        }

        @Test
        @DisplayName("isSameOccurrenceAs should return false when event ids differ but aggregate ids match")
        void isSameOccurrenceAs_ShouldReturnFalse_WhenEventIdsDifferButAggregateIdsMatch() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            TestEvent event1 = new TestEvent(aggregateId, "payload1");
            TestEvent event2 = new TestEvent(aggregateId, "payload2");

            // when
            boolean result = event1.isSameOccurrenceAs(event2);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("isSameOccurrenceAs should return false when both event ids and aggregate ids differ")
        void isSameOccurrenceAs_ShouldReturnFalse_WhenBothEventIdsAndAggregateIdsDiffer() {
            // given
            Identifier<?> aggregateId1 = mock(Identifier.class);
            Identifier<?> aggregateId2 = mock(Identifier.class);
            TestEvent event1 = new TestEvent(aggregateId1, "payload1");
            TestEvent event2 = new TestEvent(aggregateId2, "payload2");

            // when
            boolean result = event1.isSameOccurrenceAs(event2);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("isSameOccurrenceAs should return true when comparing event with itself")
        void isSameOccurrenceAs_ShouldReturnTrue_WhenComparingWithItself() {
            // given
            Identifier<?> aggregateId = mock(Identifier.class);
            TestEvent event = new TestEvent(aggregateId, "payload");

            // when
            boolean result = event.isSameOccurrenceAs(event);

            // then
            then(result).isTrue();
        }
    }

    @Nested
    @DisplayName("getMetadata")
    class GetMetadataTests {

        @Test
        @DisplayName("getMetadata should return an empty map when not overridden")
        void getMetadata_ShouldReturnEmptyMap_WhenNotOverridden() {
            // given
            DomainEvent<Identifier<?>, String> event = new DefaultMetadataEvent();

            // when
            Map<String, Object> metadata = event.getMetadata();

            // then
            then(metadata).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMetadataValue")
    class GetMetadataValueTests {

        @Test
        @DisplayName("getMetadataValue should return null when key is null")
        void getMetadataValue_ShouldReturnNull_WhenKeyIsNull() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            Object result = event.getMetadataValue(null);

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("getMetadataValue should return null when key is empty string")
        void getMetadataValue_ShouldReturnNull_WhenKeyIsEmpty() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            Object result = event.getMetadataValue("");

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("getMetadataValue should return null when key is blank string")
        void getMetadataValue_ShouldReturnNull_WhenKeyIsBlank() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            Object result = event.getMetadataValue("   ");

            // then
            then(result).isNull();
        }

        @Test
        @DisplayName("getMetadataValue should return the value when key exists in metadata")
        void getMetadataValue_ShouldReturnValue_WhenKeyExists() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            Object result = event.getMetadataValue("key");

            // then
            then(result).isEqualTo("value");
        }

        @Test
        @DisplayName("getMetadataValue should return null when key does not exist in metadata")
        void getMetadataValue_ShouldReturnNull_WhenKeyDoesNotExist() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            Object result = event.getMetadataValue("nonexistent");

            // then
            then(result).isNull();
        }
    }

    @Nested
    @DisplayName("hasMetadata")
    class HasMetadataTests {

        @Test
        @DisplayName("hasMetadata should return false when key is null")
        void hasMetadata_ShouldReturnFalse_WhenKeyIsNull() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            boolean result = event.hasMetadata(null);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("hasMetadata should return false when key is empty string")
        void hasMetadata_ShouldReturnFalse_WhenKeyIsEmpty() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            boolean result = event.hasMetadata("");

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("hasMetadata should return false when key is blank string")
        void hasMetadata_ShouldReturnFalse_WhenKeyIsBlank() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            boolean result = event.hasMetadata("   ");

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("hasMetadata should return true when key exists in metadata")
        void hasMetadata_ShouldReturnTrue_WhenKeyExists() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            boolean result = event.hasMetadata("key");

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("hasMetadata should return false when key does not exist in metadata")
        void hasMetadata_ShouldReturnFalse_WhenKeyDoesNotExist() {
            // given
            Map<String, Object> metadata = Map.of("key", "value");
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, metadata);

            // when
            boolean result = event.hasMetadata("nonexistent");

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isOfType")
    class IsOfTypeTests {

        @Test
        @DisplayName("isOfType should return false when the provided type is null")
        void isOfType_ShouldReturnFalse_WhenTypeIsNull() {
            // given
            EventType ownType = mock(EventType.class);
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(ownType, Collections.emptyMap());

            // when
            boolean result = event.isOfType(null);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("isOfType should return false when the event's own type is null")
        void isOfType_ShouldReturnFalse_WhenOwnEventTypeIsNull() {
            // given
            EventType otherType = mock(EventType.class);
            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(null, Collections.emptyMap());

            // when
            boolean result = event.isOfType(otherType);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("isOfType should return true when both event type names are equal")
        void isOfType_ShouldReturnTrue_WhenNamesAreEqual() {
            // given
            EventType ownType = mock(EventType.class);
            given(ownType.getName()).willReturn("TEST_EVENT");

            EventType otherType = mock(EventType.class);
            given(otherType.getName()).willReturn("TEST_EVENT");

            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(ownType, Collections.emptyMap());

            // when
            boolean result = event.isOfType(otherType);

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("isOfType should return false when event type names are not equal")
        void isOfType_ShouldReturnFalse_WhenNamesAreNotEqual() {
            // given
            EventType ownType = mock(EventType.class);
            given(ownType.getName()).willReturn("TEST_EVENT_1");

            EventType otherType = mock(EventType.class);
            given(otherType.getName()).willReturn("TEST_EVENT_2");

            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(ownType, Collections.emptyMap());

            // when
            boolean result = event.isOfType(otherType);

            // then
            then(result).isFalse();
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("isOfType should return true when both event type names are null")
        void isOfType_ShouldReturnTrue_WhenBothNamesAreNull() {
            // given
            EventType ownType = mock(EventType.class);
            given(ownType.getName()).willReturn(null);

            EventType otherType = mock(EventType.class);
            given(otherType.getName()).willReturn(null);

            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(ownType, Collections.emptyMap());

            // when
            boolean result = event.isOfType(otherType);

            // then
            then(result).isTrue();
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("isOfType should return false when own name is null and other name is not null")
        void isOfType_ShouldReturnFalse_WhenOwnNameIsNullAndOtherNameIsNotNull() {
            // given
            EventType ownType = mock(EventType.class);
            given(ownType.getName()).willReturn(null);

            EventType otherType = mock(EventType.class);
            given(otherType.getName()).willReturn("TEST_EVENT");

            DomainEvent<Identifier<?>, String> event = new TestDomainEvent(ownType, Collections.emptyMap());

            // when
            boolean result = event.isOfType(otherType);

            // then
            then(result).isFalse();
        }
    }

    /**
     * Dummy implementation to test the default getMetadata() method.
     */
    private static class DefaultMetadataEvent implements DomainEvent<Identifier<?>, String> {

        @Override
        public String getEventId() {
            return "id";
        }

        @Override
        public EventType eventType() {
            return null;
        }

        @Override
        public Instant getOccurredAt() {
            return Instant.now();
        }

        @Override
        public Identifier<?> getAggregateId() {
            return mock(Identifier.class);
        }

        @Override
        public String getPayload() {
            return "payload";
        }
    }

    /**
     * Dummy implementation to test default methods with configurable event type and metadata.
     */
    private record TestDomainEvent(EventType eventType, Map<String, Object> metadata)
            implements DomainEvent<Identifier<?>, String> {

        @Override
        public String getEventId() {
            return "event-id";
        }

        @Override
        public Instant getOccurredAt() {
            return Instant.now();
        }

        @Override
        public Identifier<?> getAggregateId() {
            return mock(Identifier.class);
        }

        @Override
        public String getPayload() {
            return "payload";
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * Concrete implementation of {@link AbstractDomainEvent} for testing purposes.
     */
    private static class TestEvent extends AbstractDomainEvent<Identifier<?>, String> {

        TestEvent(Identifier<?> aggregateId, String payload) {
            super(aggregateId, payload);
        }

        TestEvent(Identifier<?> aggregateId, String payload, Clock clock) {
            super(aggregateId, payload, clock);
        }

        TestEvent(Identifier<?> aggregateId, String payload, Clock clock, IdGenerator idGenerator) {
            super(aggregateId, payload, clock, idGenerator);
        }

        TestEvent(Identifier<?> aggregateId, String payload, Map<String, Object> metadata) {
            super(aggregateId, payload, null, null, metadata);
        }

        TestEvent(Identifier<?> aggregateId, String payload, Clock clock,
                  IdGenerator idGenerator, Map<String, Object> metadata) {
            super(aggregateId, payload, clock, idGenerator, metadata);
        }

        public PayloadRedactionPolicy getRedactionPolicy() {
            return redactionPolicy();
        }
    }

    /**
     * Second concrete implementation of {@link AbstractDomainEvent} used only to
     * prove that {@code equals}/{@code hashCode} never match across different
     * event kinds, even when all core state (id, timestamp, aggregate, payload)
     * is identical.
     */
    private static class OtherTestEvent extends AbstractDomainEvent<Identifier<?>, String> {

        OtherTestEvent(Identifier<?> aggregateId, String payload, Clock clock, IdGenerator idGenerator) {
            super(aggregateId, payload, clock, idGenerator);
        }
    }
}
