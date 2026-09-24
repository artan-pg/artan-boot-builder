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

package ir.artanpg.boot.domain.model;

import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link AbstractAggregateRoot}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("AbstractAggregateRoot")
class AggregateRootTests {

    private static final String IDENTIFIER_1 = "21d63136-6a4c-4625-8229-1cc80f617cd6";
    private static final String IDENTIFIER_2 = "988316af-5941-46b2-b3bc-2580a5d50320";

    private static final String ID_NULL_EXCEPTION = "The identifier cannot be null";

    @Nested
    @DisplayName("Constructor with Identifier")
    class ConstructorWithIdentifier {

        @Test
        @DisplayName("should set id when valid identifier is provided")
        void constructor_ShouldSetId_WhenValidIdentifierIsProvided() {
            // given
            TestIdentifier id = createTestIdentifier();

            // when
            TestAggregate aggregate = new TestAggregate(id);

            // then
            then(aggregate.getId())
                    .as("Aggregate id should match the provided identifier")
                    .isEqualTo(id);
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainException when id is null")
        void constructor_ShouldThrowDomainException_WhenIdIsNull() {
            // given
            TestIdentifier nullId = null;

            // when & then
            thenThrownBy(() -> new TestAggregate(nullId))
                    .as("Should throw DomainException when id is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ID_NULL_EXCEPTION);
        }
    }

    @Nested
    @DisplayName("Constructor with Builder")
    class ConstructorWithBuilder {

        @Test
        @DisplayName("should set id from builder when valid builder is provided")
        void constructor_ShouldSetIdFromBuilder_WhenValidBuilderIsProvided() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregate.TestAggregateBuilder builder = new TestAggregate.TestAggregateBuilder()
                    .identifier(id);

            // when
            TestAggregate aggregate = new TestAggregate(builder);

            // then
            then(aggregate.getId())
                    .as("Aggregate id should match the builder's identifier")
                    .isEqualTo(id);
        }

        @SuppressWarnings({"DataFlowIssue", "ConstantValue"})
        @Test
        @DisplayName("should throw DomainException when builder is null")
        void constructor_ShouldThrowDomainException_WhenBuilderIsNull() {
            // given
            TestAggregate.TestAggregateBuilder nullBuilder = null;

            // when & then
            thenThrownBy(() -> new TestAggregate(nullBuilder))
                    .as("Should throw DomainException when builder is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The builder cannot be null");
        }
    }

    @Nested
    @DisplayName("getDomainEvents")
    class GetDomainEventsTests {

        @Test
        void getDomainEvents_ShouldReturnEmptyList_WhenNoEventsRegistered() {
            // given
            TestAggregate aggregate = new TestAggregate(new TestIdentifier(IDENTIFIER_1));

            // when
            List<DomainEvent> events = aggregate.getDomainEvents();

            // then
            then(events).isEmpty();
        }

        @Test
        void getDomainEvents_ShouldReturnRegisteredEvents_WhenEventsExist() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(IDENTIFIER_1);
            TestAggregate aggregate = new TestAggregate(aggregateId);

            TestDomainEvent event1 = new TestDomainEvent(aggregateId, aggregate);
            TestDomainEvent event2 = new TestDomainEvent(aggregateId, aggregate);

            aggregate.registerEvent(event1);
            aggregate.registerEvent(event2);

            // when
            List<DomainEvent> events = aggregate.getDomainEvents();

            // then
            then(events).hasSize(2);
            then(events).containsExactly(event1, event2);
        }

        @Test
        void getDomainEvents_ShouldReturnDefensiveCopy_WhenCalledMultipleTimes() {
            // given
            TestIdentifier testIdentifier = new TestIdentifier("agg-007");
            TestAggregate aggregate = new TestAggregate(testIdentifier);
            TestDomainEvent event = new TestDomainEvent(testIdentifier, aggregate);

            // when
            List<DomainEvent> events1 = aggregate.getDomainEvents();
            aggregate.registerEvent(event);
            List<DomainEvent> events2 = aggregate.getDomainEvents();

            // then
            then(events1).isEmpty();
            then(events2).hasSize(1);
            then(events1).isNotSameAs(events2);
        }
    }

    @Nested
    @DisplayName("registerEvent")
    class RegisterEventTests {

        @Test
        void registerEvent_ShouldAddEventToList_WhenValidEventProvided() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(IDENTIFIER_1);
            TestAggregate aggregate = new TestAggregate(aggregateId);

            TestDomainEvent event = new TestDomainEvent(aggregateId, aggregate);

            // when
            aggregate.registerEvent(event);

            // then
            then(aggregate.getDomainEvents()).containsExactly(event);
        }

        @Test
        void registerEvent_ShouldMaintainInsertionOrder_WhenMultipleEventsRegistered() {
            // given
            TestIdentifier aggregateId = new TestIdentifier(IDENTIFIER_1);
            TestAggregate aggregate = new TestAggregate(aggregateId);

            TestDomainEvent event1 = new TestDomainEvent(aggregateId, aggregate);
            TestDomainEvent event2 = new TestDomainEvent(aggregateId, aggregate);
            TestDomainEvent event3 = new TestDomainEvent(aggregateId, aggregate);

            // when
            aggregate.registerEvent(event1);
            aggregate.registerEvent(event2);
            aggregate.registerEvent(event3);

            // then
            then(aggregate.getDomainEvents()).containsExactly(event1, event2, event3);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        void registerEvent_ShouldThrowDomainException_WhenEventIsNull() {
            // given
            TestAggregate aggregate = new TestAggregate(new TestIdentifier(IDENTIFIER_1));
            DomainEvent nullEvent = null;

            // when & then
            thenThrownBy(() -> aggregate.registerEvent(nullEvent))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The event object cannot be null");
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @SuppressWarnings({"EqualsWithItself", "ConstantValue"})
        @Test
        @DisplayName("should return true when comparing aggregate with itself")
        void equals_ShouldReturnTrue_WhenComparingAggregateWithItself() {
            // given
            TestAggregate aggregate = createAggregate();

            // when
            boolean result = aggregate.equals(aggregate);

            // then
            then(result)
                    .as("Aggregate should be equal to itself")
                    .isTrue();
        }

        @Test
        @DisplayName("should return true when comparing aggregates with same id and same class")
        void equals_ShouldReturnTrue_WhenComparingAggregatesWithSameIdAndSameClass() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregate aggregate1 = new TestAggregate(id);
            TestAggregate aggregate2 = new TestAggregate(createTestIdentifier());

            // when
            boolean result = aggregate1.equals(aggregate2);

            // then
            then(result)
                    .as("Aggregates with same id and same class should be equal")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when comparing aggregates with different ids")
        void equals_ShouldReturnFalse_WhenComparingAggregatesWithDifferentIds() {
            // given
            TestAggregate aggregate1 = new TestAggregate(createTestIdentifier());
            TestAggregate aggregate2 = new TestAggregate(new TestIdentifier(IDENTIFIER_2));

            // when
            boolean result = aggregate1.equals(aggregate2);

            // then
            then(result)
                    .as("Aggregates with different ids should not be equal")
                    .isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("should return false when comparing aggregates with same id but different classes")
        void equals_ShouldReturnFalse_WhenComparingAggregatesWithSameIdButDifferentClasses() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregate aggregate1 = new TestAggregate(id);
            AnotherTestAggregate aggregate2 = new AnotherTestAggregate(id);

            // when
            boolean result = aggregate1.equals(aggregate2);

            // then
            then(result)
                    .as("Aggregates with same id but different classes should not be equal")
                    .isFalse();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return false when comparing with null")
        void equals_ShouldReturnFalse_WhenComparingWithNull() {
            // given
            TestAggregate aggregate = createAggregate();

            // when
            boolean result = aggregate.equals(null);

            // then
            then(result)
                    .as("Aggregate should not be equal to null")
                    .isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("should return false when comparing with different object type")
        void equals_ShouldReturnFalse_WhenComparingWithDifferentObjectType() {
            // given
            TestAggregate aggregate = createAggregate();

            // when
            boolean result = aggregate.equals("not an aggregate");

            // then
            then(result)
                    .as("Aggregate should not be equal to different object type")
                    .isFalse();
        }

        @Test
        @DisplayName("should return same hash code for equal aggregates")
        void hashCode_ShouldReturnSameHashCode_ForEqualAggregates() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregate aggregate1 = new TestAggregate(id);
            TestAggregate aggregate2 = new TestAggregate(createTestIdentifier());

            // when & then
            then(aggregate1.hashCode())
                    .as("Equal aggregates should have same hash code")
                    .isEqualTo(aggregate2.hashCode());
        }

        @Test
        @DisplayName("should return different hash code for aggregates with different ids")
        void hashCode_ShouldReturnDifferentHashCode_ForAggregatesWithDifferentIds() {
            // given
            TestAggregate aggregate1 = new TestAggregate(createTestIdentifier());
            TestAggregate aggregate2 = new TestAggregate(new TestIdentifier(IDENTIFIER_2));

            // when & then
            then(aggregate1.hashCode())
                    .as("Aggregates with different ids should have different hash codes")
                    .isNotEqualTo(aggregate2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should return string representation with class name and id")
        void toString_ShouldReturnStringRepresentation_WhenCalled() {
            // given
            TestIdentifier id = createTestIdentifier();
            TestAggregate aggregate = new TestAggregate(id);

            // when
            String result = aggregate.toString();

            // then
            then(result)
                    .as("toString should contain class name and id")
                    .contains("AbstractAggregateRoot")
                    .contains("id=21d63136-6a4c-4625-8229-1cc80f617cd6");
        }
    }

    @Nested
    @DisplayName("AbstractBuilder")
    class AbstractBuilderTests {

        @Test
        @DisplayName("should set identifier when provided")
        void builder_ShouldSetIdentifier_WhenProvided() {
            // given
            TestIdentifier id = createTestIdentifier();

            // when
            TestAggregate.TestAggregateBuilder builder = new TestAggregate.TestAggregateBuilder()
                    .identifier(id);
            TestAggregate aggregate = builder.build();

            // then
            then(aggregate.getId())
                    .as("Builder should set identifier correctly")
                    .isEqualTo(id);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when identifier is null")
        void builder_ShouldThrowDomainException_WhenIdentifierIsNull() {
            // given
            TestIdentifier nullId = null;

            // when & then
            thenThrownBy(() -> new TestAggregate.TestAggregateBuilder().identifier(nullId))
                    .as("Should throw DomainException when identifier is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ID_NULL_EXCEPTION);
        }

        @Test
        @DisplayName("should support method chaining")
        void builder_ShouldSupportMethodChaining_WhenCalled() {
            // given
            TestIdentifier id = createTestIdentifier();

            // when
            TestAggregate.TestAggregateBuilder builder = new TestAggregate.TestAggregateBuilder()
                    .identifier(id);

            // then
            then(builder)
                    .as("Builder should support method chaining")
                    .isNotNull();
        }
    }

    // ========================================================================================
    // Helper Methods
    // ========================================================================================

    private TestIdentifier createTestIdentifier() {
        return new TestIdentifier(IDENTIFIER_1);
    }

    private TestAggregate createAggregate() {
        return new TestAggregate(createTestIdentifier());
    }

    // ========================================================================================
    // Test Doubles
    // ========================================================================================

    private record TestIdentifier(String value) implements Identifier<String> {

        @Override
        public String value() {
            return this.value;
        }
    }

    private static class TestDomainEvent extends AbstractDomainEvent<TestIdentifier, TestAggregate> {

        @Serial
        private static final long serialVersionUID = 502743136739272860L;

        TestDomainEvent(TestIdentifier aggregateId, TestAggregate aggregate) {
            super(aggregateId, aggregate);
        }

        @Override
        public EventType getEventType() {
            return EventType.valueOf("TEST");
        }
    }

    private static class TestAggregate extends AbstractAggregateRoot<TestIdentifier> {

        @Serial
        private static final long serialVersionUID = -4693797180843014572L;

        TestAggregate(TestIdentifier id) {
            super(id);
        }

        TestAggregate(TestAggregateBuilder builder) {
            super(builder);
        }

        static class TestAggregateBuilder extends AbstractBuilder<TestIdentifier, TestAggregate, TestAggregateBuilder> {

            @Override
            protected TestAggregateBuilder self() {
                return this;
            }

            @Override
            public TestAggregate build() {
                return new TestAggregate(this);
            }
        }
    }

    private static class AnotherTestAggregate extends AbstractAggregateRoot<TestIdentifier> {

        @Serial
        private static final long serialVersionUID = 5781130267732695490L;

        AnotherTestAggregate(TestIdentifier id) {
            super(id);
        }
    }
}
