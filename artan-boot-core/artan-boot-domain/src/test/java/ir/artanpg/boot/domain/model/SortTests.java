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

import ir.artanpg.boot.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link Sort}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("Sort")
class SortTests {

    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_AGE = "age";
    private static final String PROPERTY_EMAIL = "email";
    private static final String BLANK_PROPERTY = "   ";
    private static final String ERROR_PROPERTY_NULL = "The property cannot be null or blank";
    private static final String ERROR_PROPERTIES_NULL = "The properties cannot be null or blank";
    private static final String ERROR_SORT_NULL = "The sort must not be null";

    @Nested
    @DisplayName("unsorted")
    class Unsorted {

        @Test
        @DisplayName("should return empty sort")
        void unsorted_ShouldReturnEmptySort_WhenCalled() {
            // when
            Sort sort = Sort.unsorted();

            // then
            then(sort.isEmpty())
                    .as("Unsorted should be empty")
                    .isTrue();
            then(sort.size())
                    .as("Unsorted size should be zero")
                    .isZero();
        }
    }

    @Nested
    @DisplayName("asc(String)")
    class AscSingle {

        @Test
        @DisplayName("should create ascending sort for valid property")
        void asc_ShouldCreateAscendingSort_WhenPropertyIsValid() {
            // when
            Sort sort = Sort.asc(PROPERTY_NAME);

            // then
            then(sort.getDirection(PROPERTY_NAME))
                    .as("Direction should be ASC")
                    .isEqualTo(Sort.Direction.ASC);
            then(sort.size())
                    .as("Size should be one")
                    .isEqualTo(1);
        }

        @SuppressWarnings({"DataFlowIssue", "ConstantValue"})
        @Test
        @DisplayName("should throw DomainException when property is null")
        void asc_ShouldThrowDomainException_WhenPropertyIsNull() {
            // given
            String nullProperty = null;

            // when & then
            thenThrownBy(() -> Sort.asc(nullProperty))
                    .as("Should throw when property is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when property is blank")
        void asc_ShouldThrowDomainException_WhenPropertyIsBlank() {
            // when & then
            thenThrownBy(() -> Sort.asc(BLANK_PROPERTY))
                    .as("Should throw when property is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }
    }

    @Nested
    @DisplayName("asc(String...)")
    class AscMultiple {

        @Test
        @DisplayName("should create ascending sort for multiple properties")
        void asc_ShouldCreateAscendingSort_WhenMultiplePropertiesProvided() {
            // when
            Sort sort = Sort.asc(PROPERTY_NAME, PROPERTY_AGE);

            // then
            then(sort.getDirection(PROPERTY_NAME))
                    .as("First property should be ASC")
                    .isEqualTo(Sort.Direction.ASC);
            then(sort.getDirection(PROPERTY_AGE))
                    .as("Second property should be ASC")
                    .isEqualTo(Sort.Direction.ASC);
            then(sort.size())
                    .as("Size should be two")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("should return unsorted when no properties provided")
        void asc_ShouldReturnUnsorted_WhenNoPropertiesProvided() {
            // when
            Sort sort = Sort.asc();

            // then
            then(sort.isEmpty())
                    .as("Should be empty when no properties")
                    .isTrue();
        }

        @SuppressWarnings({"DataFlowIssue", "ConstantValue"})
        @Test
        @DisplayName("should throw DomainException when properties array is null")
        void asc_ShouldThrowDomainException_WhenPropertiesArrayIsNull() {
            // given
            String[] nullProperties = null;

            // when & then
            thenThrownBy(() -> Sort.asc(nullProperties))
                    .as("Should throw when array is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTIES_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when any property is null")
        void asc_ShouldThrowDomainException_WhenAnyPropertyIsNull() {
            // given
            String[] properties = {PROPERTY_NAME, null};

            // when & then
            thenThrownBy(() -> Sort.asc(properties))
                    .as("Should throw when any property is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when any property is blank")
        void asc_ShouldThrowDomainException_WhenAnyPropertyIsBlank() {
            // given
            String[] properties = {PROPERTY_NAME, BLANK_PROPERTY};

            // when & then
            thenThrownBy(() -> Sort.asc(properties))
                    .as("Should throw when any property is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }
    }

    @Nested
    @DisplayName("desc(String)")
    class DescSingle {

        @Test
        @DisplayName("should create descending sort for valid property")
        void desc_ShouldCreateDescendingSort_WhenPropertyIsValid() {
            // when
            Sort sort = Sort.desc(PROPERTY_NAME);

            // then
            then(sort.getDirection(PROPERTY_NAME))
                    .as("Direction should be DESC")
                    .isEqualTo(Sort.Direction.DESC);
        }

        @SuppressWarnings({"DataFlowIssue", "ConstantValue"})
        @Test
        @DisplayName("should throw DomainException when property is null")
        void desc_ShouldThrowDomainException_WhenPropertyIsNull() {
            // given
            String nullProperty = null;

            // when & then
            thenThrownBy(() -> Sort.desc(nullProperty))
                    .as("Should throw when property is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when property is blank")
        void desc_ShouldThrowDomainException_WhenPropertyIsBlank() {
            // given

            // when & then
            thenThrownBy(() -> Sort.desc(BLANK_PROPERTY))
                    .as("Should throw when property is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }
    }

    @Nested
    @DisplayName("desc(String...)")
    class DescMultiple {

        @Test
        @DisplayName("should create descending sort for multiple properties")
        void desc_ShouldCreateDescendingSort_WhenMultiplePropertiesProvided() {
            // when
            Sort sort = Sort.desc(PROPERTY_NAME, PROPERTY_AGE);

            // then
            then(sort.getDirection(PROPERTY_NAME))
                    .as("First property should be DESC")
                    .isEqualTo(Sort.Direction.DESC);
            then(sort.getDirection(PROPERTY_AGE))
                    .as("Second property should be DESC")
                    .isEqualTo(Sort.Direction.DESC);
        }

        @Test
        @DisplayName("should return unsorted when no properties provided")
        void desc_ShouldReturnUnsorted_WhenNoPropertiesProvided() {
            // when
            Sort sort = Sort.desc();

            // then
            then(sort.isEmpty())
                    .as("Should be empty when no properties")
                    .isTrue();
        }

        @SuppressWarnings({"DataFlowIssue", "ConstantValue"})
        @Test
        @DisplayName("should throw DomainException when properties array is null")
        void desc_ShouldThrowDomainException_WhenPropertiesArrayIsNull() {
            // given
            String[] nullProperties = null;

            // when & then
            thenThrownBy(() -> Sort.desc(nullProperties))
                    .as("Should throw when array is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTIES_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when any property is null")
        void desc_ShouldThrowDomainException_WhenAnyPropertyIsNull() {
            // given
            String[] properties = {PROPERTY_NAME, null};

            // when & then
            thenThrownBy(() -> Sort.desc(properties))
                    .as("Should throw when any property is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when any property is blank")
        void desc_ShouldThrowDomainException_WhenAnyPropertyIsBlank() {
            // given
            String[] properties = {PROPERTY_NAME, BLANK_PROPERTY};

            // when & then
            thenThrownBy(() -> Sort.desc(properties))
                    .as("Should throw when any property is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }
    }

    @Nested
    @DisplayName("andAsc")
    class AndAsc {

        @Test
        @DisplayName("should add ascending property to existing sort")
        void andAsc_ShouldAddAscendingProperty_WhenPropertyIsValid() {
            // given
            Sort initial = Sort.asc(PROPERTY_NAME);

            // when
            Sort result = initial.andAsc(PROPERTY_AGE);

            // then
            then(result.size())
                    .as("Size should be two")
                    .isEqualTo(2);
            then(result.getDirection(PROPERTY_NAME))
                    .as("First property should remain ASC")
                    .isEqualTo(Sort.Direction.ASC);
            then(result.getDirection(PROPERTY_AGE))
                    .as("Second property should be ASC")
                    .isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("should not modify original sort")
        void andAsc_ShouldNotModifyOriginalSort_WhenCalled() {
            // given
            Sort initial = Sort.asc(PROPERTY_NAME);

            // when
            initial.andAsc(PROPERTY_AGE);

            // then
            then(initial.size())
                    .as("Original sort should be unchanged")
                    .isEqualTo(1);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when property is null")
        void andAsc_ShouldThrowDomainException_WhenPropertyIsNull() {
            // given
            Sort initial = Sort.asc(PROPERTY_NAME);
            String nullProperty = null;

            // when & then
            thenThrownBy(() -> initial.andAsc(nullProperty))
                    .as("Should throw when property is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when property is blank")
        void andAsc_ShouldThrowDomainException_WhenPropertyIsBlank() {
            // given
            Sort initial = Sort.asc(PROPERTY_NAME);

            // when & then
            thenThrownBy(() -> initial.andAsc(BLANK_PROPERTY))
                    .as("Should throw when property is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }
    }

    @Nested
    @DisplayName("andDesc")
    class AndDesc {

        @Test
        @DisplayName("should add descending property to existing sort")
        void andDesc_ShouldAddDescendingProperty_WhenPropertyIsValid() {
            // given
            Sort initial = Sort.asc(PROPERTY_NAME);

            // when
            Sort result = initial.andDesc(PROPERTY_AGE);

            // then
            then(result.getDirection(PROPERTY_AGE))
                    .as("Added property should be DESC")
                    .isEqualTo(Sort.Direction.DESC);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when property is null")
        void andDesc_ShouldThrowDomainException_WhenPropertyIsNull() {
            // given
            Sort initial = Sort.asc(PROPERTY_NAME);
            String nullProperty = null;

            // when & then
            thenThrownBy(() -> initial.andDesc(nullProperty))
                    .as("Should throw when property is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when property is blank")
        void andDesc_ShouldThrowDomainException_WhenPropertyIsBlank() {
            // given
            Sort initial = Sort.asc(PROPERTY_NAME);

            // when & then
            thenThrownBy(() -> initial.andDesc(BLANK_PROPERTY))
                    .as("Should throw when property is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PROPERTY_NULL);
        }
    }

    @Nested
    @DisplayName("and")
    class And {

        @Test
        @DisplayName("should combine two sorts")
        void and_ShouldCombineTwoSorts_WhenBothAreValid() {
            // given
            Sort first = Sort.asc(PROPERTY_NAME);
            Sort second = Sort.desc(PROPERTY_AGE);

            // when
            Sort result = first.and(second);

            // then
            then(result.size())
                    .as("Combined size should be two")
                    .isEqualTo(2);
            then(result.getDirection(PROPERTY_NAME))
                    .as("First property should be ASC")
                    .isEqualTo(Sort.Direction.ASC);
            then(result.getDirection(PROPERTY_AGE))
                    .as("Second property should be DESC")
                    .isEqualTo(Sort.Direction.DESC);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when sort is null")
        void and_ShouldThrowDomainException_WhenSortIsNull() {
            // given
            Sort initial = Sort.asc(PROPERTY_NAME);
            Sort nullSort = null;

            // when & then
            thenThrownBy(() -> initial.and(nullSort))
                    .as("Should throw when sort is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_SORT_NULL);
        }
    }

    @Nested
    @DisplayName("getDirection")
    class GetDirection {

        @Test
        @DisplayName("should return direction for existing property")
        void getDirection_ShouldReturnDirection_WhenPropertyExists() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when
            Sort.Direction direction = sort.getDirection(PROPERTY_NAME);

            // then
            then(direction)
                    .as("Should return ASC")
                    .isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("should return null for non-existing property")
        void getDirection_ShouldReturnNull_WhenPropertyDoesNotExist() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when
            Sort.Direction direction = sort.getDirection(PROPERTY_AGE);

            // then
            then(direction)
                    .as("Should return null for non-existing property")
                    .isNull();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return null when property is null")
        void getDirection_ShouldReturnNull_WhenPropertyIsNull() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);
            String nullProperty = null;

            // when
            Sort.Direction direction = sort.getDirection(nullProperty);

            // then
            then(direction)
                    .as("Should return null for null property")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("getOrders")
    class GetOrders {

        @Test
        @DisplayName("should return unmodifiable map of orders")
        void getOrders_ShouldReturnUnmodifiableMap_WhenCalled() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when
            var orders = sort.getOrders();

            // then
            then(orders)
                    .as("Orders map should not be null")
                    .isNotNull()
                    .as("Orders map should contain the property")
                    .containsKey(PROPERTY_NAME)
                    .as("Orders map should be unmodifiable")
                    .isUnmodifiable();
        }

        @Test
        @DisplayName("should throw UnsupportedOperationException when trying to modify")
        void getOrders_ShouldThrowException_WhenTryingToModify() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);
            var orders = sort.getOrders();

            // when & then
            thenThrownBy(() -> orders.put(PROPERTY_AGE, Sort.Direction.DESC))
                    .as("Should throw when trying to modify orders")
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should return empty map for unsorted")
        void getOrders_ShouldReturnEmptyMap_WhenUnsorted() {
            // given
            Sort sort = Sort.unsorted();

            // when
            var orders = sort.getOrders();

            // then
            then(orders)
                    .as("Unsorted orders should be empty")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("getProperties")
    class GetProperties {

        @Test
        @DisplayName("should return all property names")
        void getProperties_ShouldReturnAllPropertyNames_WhenCalled() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME, PROPERTY_AGE);

            // when
            var properties = sort.getProperties();

            // then
            then(properties)
                    .as("Should contain both properties")
                    .containsExactly(PROPERTY_NAME, PROPERTY_AGE);
        }
    }

    @Nested
    @DisplayName("size and isEmpty")
    class SizeAndIsEmpty {

        @Test
        @DisplayName("should return correct size")
        void size_ShouldReturnCorrectSize_WhenCalled() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME, PROPERTY_AGE, PROPERTY_EMAIL);

            // when & then
            then(sort.size())
                    .as("Size should be three")
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("should return true when sort is empty")
        void isEmpty_ShouldReturnTrue_WhenSortIsEmpty() {
            // given
            Sort sort = Sort.unsorted();

            // when & then
            then(sort.isEmpty())
                    .as("Unsorted should be empty")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when sort is not empty")
        void isEmpty_ShouldReturnFalse_WhenSortIsNotEmpty() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when & then
            then(sort.isEmpty())
                    .as("Non-empty sort should not be empty")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("contains")
    class Contains {

        @Test
        @DisplayName("should return true when property exists")
        void contains_ShouldReturnTrue_WhenPropertyExists() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when & then
            then(sort.contains(PROPERTY_NAME))
                    .as("Should contain the property")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when property does not exist")
        void contains_ShouldReturnFalse_WhenPropertyDoesNotExist() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when & then
            then(sort.contains(PROPERTY_AGE))
                    .as("Should not contain the property")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when sorts are identical")
        void equals_ShouldBeEqual_WhenSortsAreIdentical() {
            // given
            Sort sort1 = Sort.asc(PROPERTY_NAME);
            Sort sort2 = Sort.asc(PROPERTY_NAME);

            // when & then
            then(sort1)
                    .as("Identical sorts should be equal")
                    .isEqualTo(sort2);
            then(sort1.hashCode())
                    .as("Equal sorts should have same hash")
                    .isEqualTo(sort2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when sorts differ")
        void equals_ShouldNotBeEqual_WhenSortsDiffer() {
            // given
            Sort sort1 = Sort.asc(PROPERTY_NAME);
            Sort sort2 = Sort.desc(PROPERTY_NAME);

            // when & then
            then(sort1)
                    .as("Different sorts should not be equal")
                    .isNotEqualTo(sort2);
        }

        @SuppressWarnings({"EqualsWithItself", "ConstantValue"})
        @Test
        @DisplayName("should return true when comparing with itself")
        void equals_ShouldReturnTrue_WhenComparingWithItself() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when
            boolean result = sort.equals(sort);

            // then
            then(result)
                    .as("Sort should be equal to itself")
                    .isTrue();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return false when comparing with null")
        void equals_ShouldReturnFalse_WhenComparingWithNull() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when
            boolean result = sort.equals(null);

            // then
            then(result)
                    .as("Sort should not be equal to null")
                    .isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("should return false when comparing with different type")
        void equals_ShouldReturnFalse_WhenComparingWithDifferentType() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when
            boolean result = sort.equals("not a sort");

            // then
            then(result)
                    .as("Sort should not be equal to different type")
                    .isFalse();
        }

        @Test
        @DisplayName("should return true when orders are identical")
        void equals_ShouldReturnTrue_WhenOrdersAreIdentical() {
            // given
            Sort sort1 = Sort.asc(PROPERTY_NAME).andDesc(PROPERTY_AGE);
            Sort sort2 = Sort.asc(PROPERTY_NAME).andDesc(PROPERTY_AGE);

            // when
            boolean result = sort1.equals(sort2);

            // then
            then(result)
                    .as("Sorts with identical orders should be equal")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when orders differ in direction")
        void equals_ShouldReturnFalse_WhenOrdersDifferInDirection() {
            // given
            Sort sort1 = Sort.asc(PROPERTY_NAME);
            Sort sort2 = Sort.desc(PROPERTY_NAME);

            // when
            boolean result = sort1.equals(sort2);

            // then
            then(result)
                    .as("Sorts with different directions should not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("should return false when orders differ in properties")
        void equals_ShouldReturnFalse_WhenOrdersDifferInProperties() {
            // given
            Sort sort1 = Sort.asc(PROPERTY_NAME);
            Sort sort2 = Sort.asc(PROPERTY_AGE);

            // when
            boolean result = sort1.equals(sort2);

            // then
            then(result)
                    .as("Sorts with different properties should not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("should return false when orders differ in size")
        void equals_ShouldReturnFalse_WhenOrdersDifferInSize() {
            // given
            Sort sort1 = Sort.asc(PROPERTY_NAME);
            Sort sort2 = Sort.asc(PROPERTY_NAME, PROPERTY_AGE);

            // when
            boolean result = sort1.equals(sort2);

            // then
            then(result)
                    .as("Sorts with different sizes should not be equal")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should contain class name and properties")
        void toString_ShouldContainClassNameAndProperties_WhenCalled() {
            // given
            Sort sort = Sort.asc(PROPERTY_NAME);

            // when
            String result = sort.toString();

            // then
            then(result)
                    .as("Should contain class name")
                    .contains("Sort")
                    .as("Should contain property")
                    .contains(PROPERTY_NAME)
                    .as("Should contain direction")
                    .contains("ASC");
        }

        @Test
        @DisplayName("should return [empty] for unsorted")
        void toString_ShouldReturnEmpty_WhenSortIsEmpty() {
            // given
            Sort sort = Sort.unsorted();

            // when
            String result = sort.toString();

            // then
            then(result)
                    .as("Should indicate empty")
                    .contains("empty");
        }
    }

    @Nested
    @DisplayName("Direction")
    class DirectionTests {

        @Test
        @DisplayName("ASC should be ascending")
        void asc_ShouldBeAscending_WhenChecked() {
            // when & then
            then(Sort.Direction.ASC.isAscending())
                    .as("ASC should be ascending")
                    .isTrue();
            then(Sort.Direction.ASC.isDescending())
                    .as("ASC should not be descending")
                    .isFalse();
        }

        @Test
        @DisplayName("DESC should be descending")
        void desc_ShouldBeDescending_WhenChecked() {
            // when & then
            then(Sort.Direction.DESC.isDescending())
                    .as("DESC should be descending")
                    .isTrue();
            then(Sort.Direction.DESC.isAscending())
                    .as("DESC should not be ascending")
                    .isFalse();
        }
    }
}
