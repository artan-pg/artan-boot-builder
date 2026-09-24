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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link EventType}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("EventType")
class EventTypeTests {

    private static final String EVENT_TYPE_NAME = "TEST_EVENT";
    private static final String ANOTHER_EVENT_TYPE_NAME = "ANOTHER_EVENT";
    private static final String BLANK_NAME = "   ";
    private static final String ERROR_NAME_NULL_OR_EMPTY = "The name cannot be null or empty";

    @Nested
    @DisplayName("getName")
    class GetName {

        @Test
        @DisplayName("should return name when created via valueOf")
        void getName_ShouldReturnName_WhenCreatedViaValueOf() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);

            // when
            String result = eventType.getName();

            // then
            then(result)
                    .as("Name should match the provided name")
                    .isEqualTo(EVENT_TYPE_NAME);
        }
    }

    @Nested
    @DisplayName("valueOf")
    class ValueOf {

        @Test
        @DisplayName("should create EventType when valid name is provided")
        void valueOf_ShouldCreateEventType_WhenValidNameIsProvided() {
            // given
            String name = EVENT_TYPE_NAME;

            // when
            EventType eventType = EventType.valueOf(name);

            // then
            then(eventType)
                    .as("EventType should not be null")
                    .isNotNull();
            then(eventType.getName())
                    .as("Name should match")
                    .isEqualTo(name);
        }

        @Test
        @DisplayName("should return cached instance for same name")
        void valueOf_ShouldReturnCachedInstance_WhenSameNameIsProvided() {
            // given
            String name = ANOTHER_EVENT_TYPE_NAME;

            // when
            EventType eventType1 = EventType.valueOf(name);
            EventType eventType2 = EventType.valueOf(name);

            // then
            then(eventType1)
                    .as("Should return same cached instance")
                    .isSameAs(eventType2);
        }

        @Test
        @DisplayName("should return different instances for different names")
        void valueOf_ShouldReturnDifferentInstances_WhenDifferentNamesProvided() {
            // given

            // when
            EventType eventType1 = EventType.valueOf(EVENT_TYPE_NAME);
            EventType eventType2 = EventType.valueOf(ANOTHER_EVENT_TYPE_NAME);

            // then
            then(eventType1)
                    .as("Should return different instances")
                    .isNotSameAs(eventType2);
            then(eventType1.getName())
                    .as("Names should differ")
                    .isNotEqualTo(eventType2.getName());
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainException when name is null")
        void valueOf_ShouldThrowDomainException_WhenNameIsNull() {
            // given
            String nullName = null;

            // when & then
            thenThrownBy(() -> EventType.valueOf(nullName))
                    .as("Should throw DomainException when name is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw DomainException when name is blank")
        void valueOf_ShouldThrowDomainException_WhenNameIsBlank() {
            // given

            // when & then
            thenThrownBy(() -> EventType.valueOf(BLANK_NAME))
                    .as("Should throw DomainException when name is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw DomainException when name is empty")
        void valueOf_ShouldThrowDomainException_WhenNameIsEmpty() {
            // given
            String emptyName = "";

            // when & then
            thenThrownBy(() -> EventType.valueOf(emptyName))
                    .as("Should throw DomainException when name is empty")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_EMPTY);
        }
    }

    @Nested
    @DisplayName("Serializable")
    class SerializableTests {

        @Test
        @DisplayName("should be instance of Serializable")
        void serializable_ShouldBeInstanceOfSerializable_WhenCreated() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);

            // when & then
            then(eventType)
                    .as("Should be Serializable")
                    .isInstanceOf(java.io.Serializable.class);
        }
    }

    @Nested
    @DisplayName("Functional Interface")
    class FunctionalInterfaceTests {

        @Test
        @DisplayName("should be usable as lambda expression")
        void functionalInterface_ShouldBeUsableAsLambda_WhenProvided() {
            // given
            EventType eventType = () -> "LAMBDA_EVENT";

            // when
            String name = eventType.getName();

            // then
            then(name)
                    .as("Lambda should work as EventType")
                    .isEqualTo("LAMBDA_EVENT");
        }
    }
}
