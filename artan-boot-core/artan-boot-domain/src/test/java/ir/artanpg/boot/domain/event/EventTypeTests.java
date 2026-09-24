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

    private static final String ERROR_NAME_NULL_OR_BLANK = "The name cannot be null or blank";

    @Nested
    @DisplayName("Default methods")
    class DefaultMethods {

        @Test
        @DisplayName("getCategory should return empty string when using default implementation")
        void getCategory_ShouldReturnEmptyString_WhenCalledOnDefaultImplementation() {
            // given
            EventType eventType = new CustomEventType("test");

            // when
            String category = eventType.getCategory();

            // then
            then(category)
                    .as("Category should be empty string by default")
                    .isEmpty();
        }

        @Test
        @DisplayName("getVersion should return empty string when using default implementation")
        void getVersion_ShouldReturnEmptyString_WhenCalledOnDefaultImplementation() {
            // given
            EventType eventType = new CustomEventType("test");

            // when
            String version = eventType.getVersion();

            // then
            then(version)
                    .as("Version should be empty string by default")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("named factory method")
    class NamedFactory {

        @Test
        @DisplayName("should create NamedEventType with valid name")
        void named_ShouldCreateNamedEventType_WhenValidNameProvided() {
            // given
            String name = "OrderPlacedEvent";

            // when
            EventType eventType = EventType.named(name);

            // then
            then(eventType)
                    .as("Should return NamedEventType instance")
                    .isInstanceOf(EventType.NamedEventType.class);
            then(eventType.getName())
                    .as("Name should match")
                    .isEqualTo(name);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainEventException when name is null")
        void named_ShouldThrowDomainEventException_WhenNameIsNull() {
            // given
            String nullName = null;

            // when & then
            thenThrownBy(() -> EventType.named(nullName))
                    .as("Should throw DomainEventException when name is null")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is blank")
        void named_ShouldThrowDomainEventException_WhenNameIsBlank() {
            // given
            String blankName = "   ";

            // when & then
            thenThrownBy(() -> EventType.named(blankName))
                    .as("Should throw DomainEventException when name is blank")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is empty")
        void named_ShouldThrowDomainEventException_WhenNameIsEmpty() {
            // given
            String emptyName = "";

            // when & then
            thenThrownBy(() -> EventType.named(emptyName))
                    .as("Should throw DomainEventException when name is empty")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name contains invalid characters")
        void named_ShouldThrowDomainEventException_WhenNameContainsInvalidCharacters() {
            // given
            String invalidName = "Order Placed Event!";

            // when & then
            thenThrownBy(() -> EventType.named(invalidName))
                    .as("Should throw DomainEventException when name contains invalid characters")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("is not a valid EventType name");
        }

        @Test
        @DisplayName("should throw DomainEventException when name does not follow structured syntax")
        void named_ShouldThrowDomainEventException_WhenNameDoesNotFollowStructuredSyntax() {
            // given
            String invalidName = "123..Invalid";

            // when & then
            thenThrownBy(() -> EventType.named(invalidName))
                    .as("Should throw DomainEventException when name does not follow structured syntax")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("does not follow the required 'category.name.version' syntax");
        }
    }

    @Nested
    @DisplayName("NamedEventType")
    class NamedEventTypeTests {

        @Nested
        @DisplayName("Constructor")
        class Constructor {

            @Test
            @DisplayName("should create instance with valid single-segment name")
            void constructor_ShouldCreateInstance_WhenValidSingleSegmentNameProvided() {
                // given
                String name = "OrderPlacedEvent";

                // when
                EventType.NamedEventType eventType = new EventType.NamedEventType(name);

                // then
                then(eventType.getName())
                        .as("Name should match")
                        .isEqualTo(name);
            }

            @Test
            @DisplayName("should create instance with valid two-segment name")
            void constructor_ShouldCreateInstance_WhenValidTwoSegmentNameProvided() {
                // given
                String name = "customer.registered";

                // when
                EventType.NamedEventType eventType = new EventType.NamedEventType(name);

                // then
                then(eventType.getName())
                        .as("Name should match")
                        .isEqualTo(name);
            }

            @Test
            @DisplayName("should create instance with valid three-segment name")
            void constructor_ShouldCreateInstance_WhenValidThreeSegmentNameProvided() {
                // given
                String name = "order.OrderPlaced.v1";

                // when
                EventType.NamedEventType eventType = new EventType.NamedEventType(name);

                // then
                then(eventType.getName())
                        .as("Name should match")
                        .isEqualTo(name);
            }

            @SuppressWarnings("ConstantValue")
            @Test
            @DisplayName("should throw DomainEventException when name is null")
            void constructor_ShouldThrowDomainEventException_WhenNameIsNull() {
                // given
                String nullName = null;

                // when & then
                thenThrownBy(() -> new EventType.NamedEventType(nullName))
                        .as("Should throw DomainEventException when name is null")
                        .isInstanceOf(DomainEventException.class)
                        .hasMessage(ERROR_NAME_NULL_OR_BLANK);
            }

            @Test
            @DisplayName("should throw DomainEventException when name is blank")
            void constructor_ShouldThrowDomainEventException_WhenNameIsBlank() {
                // given
                String blankName = "   ";

                // when & then
                thenThrownBy(() -> new EventType.NamedEventType(blankName))
                        .as("Should throw DomainEventException when name is blank")
                        .isInstanceOf(DomainEventException.class)
                        .hasMessage(ERROR_NAME_NULL_OR_BLANK);
            }

            @Test
            @DisplayName("should throw DomainEventException when name is empty")
            void constructor_ShouldThrowDomainEventException_WhenNameIsEmpty() {
                // given
                String emptyName = "";

                // when & then
                thenThrownBy(() -> new EventType.NamedEventType(emptyName))
                        .as("Should throw DomainEventException when name is empty")
                        .isInstanceOf(DomainEventException.class)
                        .hasMessage(ERROR_NAME_NULL_OR_BLANK);
            }

            @Test
            @DisplayName("should throw DomainEventException when name contains invalid characters")
            void constructor_ShouldThrowDomainEventException_WhenNameContainsInvalidCharacters() {
                // given
                String invalidName = "Order@Placed";

                // when & then
                thenThrownBy(() -> new EventType.NamedEventType(invalidName))
                        .as("Should throw DomainEventException when name contains invalid characters")
                        .isInstanceOf(DomainEventException.class)
                        .hasMessageContaining("is not a valid EventType name");
            }
        }

        @Nested
        @DisplayName("getCategory")
        class GetCategory {

            @Test
            @DisplayName("should return empty string when name has no dots")
            void getCategory_ShouldReturnEmptyString_WhenNameHasNoDots() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("OrderPlacedEvent");

                // when
                String category = eventType.getCategory();

                // then
                then(category)
                        .as("Category should be empty for single-segment name")
                        .isEmpty();
            }

            @Test
            @DisplayName("should return first segment when name has one dot")
            void getCategory_ShouldReturnFirstSegment_WhenNameHasOneDot() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("customer.registered");

                // when
                String category = eventType.getCategory();

                // then
                then(category)
                        .as("Category should be first segment for two-segment name")
                        .isEqualTo("customer");
            }

            @Test
            @DisplayName("should return segments before last two when name has two dots")
            void getCategory_ShouldReturnSegmentsBeforeLastTwo_WhenNameHasTwoDots() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("order.OrderPlaced.v1");

                // when
                String category = eventType.getCategory();

                // then
                then(category)
                        .as("Category should be first segment for three-segment name")
                        .isEqualTo("order");
            }

            @Test
            @DisplayName("should return all segments except last two when name has multiple dots")
            void getCategory_ShouldReturnAllSegmentsExceptLastTwo_WhenNameHasMultipleDots() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("sales.order.OrderPlaced.v1");

                // when
                String category = eventType.getCategory();

                // then
                then(category)
                        .as("Category should include all segments before last two")
                        .isEqualTo("sales.order");
            }
        }

        @Nested
        @DisplayName("getVersion")
        class GetVersion {

            @Test
            @DisplayName("should return empty string when name has no dots")
            void getVersion_ShouldReturnEmptyString_WhenNameHasNoDots() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("OrderPlacedEvent");

                // when
                String version = eventType.getVersion();

                // then
                then(version)
                        .as("Version should be empty for single-segment name")
                        .isEmpty();
            }

            @Test
            @DisplayName("should return empty string when name has one dot")
            void getVersion_ShouldReturnEmptyString_WhenNameHasOneDot() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("customer.registered");

                // when
                String version = eventType.getVersion();

                // then
                then(version)
                        .as("Version should be empty for two-segment name")
                        .isEmpty();
            }

            @Test
            @DisplayName("should return last segment when name has two dots")
            void getVersion_ShouldReturnLastSegment_WhenNameHasTwoDots() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("order.OrderPlaced.v1");

                // when
                String version = eventType.getVersion();

                // then
                then(version)
                        .as("Version should be last segment for three-segment name")
                        .isEqualTo("v1");
            }

            @Test
            @DisplayName("should return last segment when name has multiple dots")
            void getVersion_ShouldReturnLastSegment_WhenNameHasMultipleDots() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("sales.order.OrderPlaced.v2");

                // when
                String version = eventType.getVersion();

                // then
                then(version)
                        .as("Version should be last segment")
                        .isEqualTo("v2");
            }
        }

        @Nested
        @DisplayName("getSimpleName")
        class GetSimpleName {

            @Test
            @DisplayName("should return full name when name has no dots")
            void getSimpleName_ShouldReturnFullName_WhenNameHasNoDots() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("OrderPlacedEvent");

                // when
                String simpleName = eventType.getSimpleName();

                // then
                then(simpleName)
                        .as("Simple name should be full name for single-segment")
                        .isEqualTo("OrderPlacedEvent");
            }

            @Test
            @DisplayName("should return second segment when name has one dot")
            void getSimpleName_ShouldReturnSecondSegment_WhenNameHasOneDot() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("customer.registered");

                // when
                String simpleName = eventType.getSimpleName();

                // then
                then(simpleName)
                        .as("Simple name should be second segment for two-segment name")
                        .isEqualTo("registered");
            }

            @Test
            @DisplayName("should return middle segment when name has two dots")
            void getSimpleName_ShouldReturnMiddleSegment_WhenNameHasTwoDots() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("order.OrderPlaced.v1");

                // when
                String simpleName = eventType.getSimpleName();

                // then
                then(simpleName)
                        .as("Simple name should be middle segment for three-segment name")
                        .isEqualTo("OrderPlaced");
            }

            @Test
            @DisplayName("should return second-to-last segment when name has multiple dots")
            void getSimpleName_ShouldReturnSecondToLastSegment_WhenNameHasMultipleDots() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("sales.order.OrderPlaced.v1");

                // when
                String simpleName = eventType.getSimpleName();

                // then
                then(simpleName)
                        .as("Simple name should be second-to-last segment")
                        .isEqualTo("OrderPlaced");
            }
        }

        @Nested
        @DisplayName("equals and hashCode")
        class EqualsAndHashCode {

            @Test
            @DisplayName("should be equal when names are same")
            void equals_ShouldBeEqual_WhenNamesAreSame() {
                // given
                EventType.NamedEventType eventType1 = new EventType.NamedEventType("OrderPlacedEvent");
                EventType.NamedEventType eventType2 = new EventType.NamedEventType("OrderPlacedEvent");

                // when & then
                then(eventType1)
                        .as("Events with same name should be equal")
                        .isEqualTo(eventType2);
            }

            @Test
            @DisplayName("should not be equal when names differ")
            void equals_ShouldNotBeEqual_WhenNamesDiffer() {
                // given
                EventType.NamedEventType eventType1 = new EventType.NamedEventType("OrderPlacedEvent");
                EventType.NamedEventType eventType2 = new EventType.NamedEventType("OrderCancelledEvent");

                // when & then
                then(eventType1)
                        .as("Events with different names should not be equal")
                        .isNotEqualTo(eventType2);
            }

            @Test
            @DisplayName("should be equal to itself")
            void equals_ShouldBeEqualToItself_WhenCompared() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("OrderPlacedEvent");

                // when & then
                then(eventType)
                        .as("Event should be equal to itself")
                        .isEqualTo(eventType);
            }

            @Test
            @DisplayName("should not be equal to null")
            void equals_ShouldNotBeEqualToNull_WhenCompared() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("OrderPlacedEvent");

                // when & then
                then(eventType)
                        .as("Event should not be equal to null")
                        .isNotEqualTo(null);
            }

            @Test
            @DisplayName("should not be equal to different type")
            void equals_ShouldNotBeEqualToDifferentType_WhenCompared() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("OrderPlacedEvent");

                // when & then
                then(eventType)
                        .as("Event should not be equal to different type")
                        .isNotEqualTo("not an event type");
            }

            @Test
            @DisplayName("should return same hash code for equal instances")
            void hashCode_ShouldReturnSameHashCode_WhenEqual() {
                // given
                EventType.NamedEventType eventType1 = new EventType.NamedEventType("OrderPlacedEvent");
                EventType.NamedEventType eventType2 = new EventType.NamedEventType("OrderPlacedEvent");

                // when & then
                then(eventType1.hashCode())
                        .as("Equal events should have same hash code")
                        .isEqualTo(eventType2.hashCode());
            }

            @Test
            @DisplayName("should return different hash codes for different instances")
            void hashCode_ShouldReturnDifferentHashCodes_WhenDifferent() {
                // given
                EventType.NamedEventType eventType1 = new EventType.NamedEventType("OrderPlacedEvent");
                EventType.NamedEventType eventType2 = new EventType.NamedEventType("OrderCancelledEvent");

                // when & then
                then(eventType1.hashCode())
                        .as("Different events should have different hash codes")
                        .isNotEqualTo(eventType2.hashCode());
            }
        }

        @Nested
        @DisplayName("toString")
        class ToString {

            @Test
            @DisplayName("should contain name in toString output")
            void toString_ShouldContainName_WhenCalled() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("OrderPlacedEvent");

                // when
                String result = eventType.toString();

                // then
                then(result)
                        .as("toString should contain name")
                        .contains("OrderPlacedEvent");
            }
        }

        @Nested
        @DisplayName("Record methods")
        class RecordMethods {

            @Test
            @DisplayName("should return name from component accessor")
            void name_ShouldReturnName_WhenCalled() {
                // given
                EventType.NamedEventType eventType = new EventType.NamedEventType("OrderPlacedEvent");

                // when
                String name = eventType.name();

                // then
                then(name)
                        .as("name() should return the name")
                        .isEqualTo("OrderPlacedEvent");
            }
        }
    }

    // ========================================================================================
    // Test Doubles
    // ========================================================================================

    /**
     * Custom EventType implementation for testing default methods.
     */
    private record CustomEventType(String name) implements EventType {
        @Override
        public String getName() {
            return name;
        }
    }
}
