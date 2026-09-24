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
 * Unit tests for {@link SimplePageable}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("SimplePageable")
class SimplePageableTests {

    private static final String ERROR_PAGE_NEGATIVE = "The page number must not be less than zero";
    private static final String ERROR_SIZE_LESS_THAN_ONE = "The page size must not be less than one";
    private static final String ERROR_SORT_NULL = "The sort must not be null";

    @Nested
    @DisplayName("of()")
    class OfDefault {

        @Test
        @DisplayName("should create default pageable")
        void of_ShouldCreateDefaultPageable_WhenCalled() {
            // when
            Pageable pageable = SimplePageable.of();

            // then
            then(pageable.getPageNumber())
                    .as("Default page should be 0")
                    .isZero();
            then(pageable.getPageSize())
                    .as("Default size should be 10")
                    .isEqualTo(10);
            then(pageable.getSort().isEmpty())
                    .as("Default sort should be empty")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("of(int pageSize)")
    class OfPageSize {

        @Test
        @DisplayName("should create pageable with specified page size")
        void of_ShouldCreatePageableWithSpecifiedSize_WhenSizeIsValid() {
            // when
            Pageable pageable = SimplePageable.of(20);

            // then
            then(pageable.getPageSize())
                    .as("Size should match")
                    .isEqualTo(20);
            then(pageable.getPageNumber())
                    .as("Page should be 0")
                    .isZero();
        }

        @Test
        @DisplayName("should throw DomainException when size is less than 1")
        void of_ShouldThrowDomainException_WhenSizeIsLessThanOne() {
            // when & then
            thenThrownBy(() -> SimplePageable.of(0))
                    .as("Should throw when size < 1")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_SIZE_LESS_THAN_ONE);
        }
    }

    @Nested
    @DisplayName("of(Sort sort)")
    class OfSort {

        @Test
        @DisplayName("should create pageable with specified sort")
        void of_ShouldCreatePageableWithSpecifiedSort_WhenSortIsValid() {
            // given
            Sort sort = Sort.asc("name");

            // when
            Pageable pageable = SimplePageable.of(sort);

            // then
            then(pageable.getSort())
                    .as("Sort should match")
                    .isEqualTo(sort);
        }
    }

    @Nested
    @DisplayName("of(int pageNumber, int pageSize)")
    class OfPageAndSize {

        @Test
        @DisplayName("should create pageable with page and size")
        void of_ShouldCreatePageable_WhenPageAndSizeAreValid() {
            // when
            Pageable pageable = SimplePageable.of(2, 15);

            // then
            then(pageable.getPageNumber())
                    .as("Page should match")
                    .isEqualTo(2);
            then(pageable.getPageSize())
                    .as("Size should match")
                    .isEqualTo(15);
        }

        @Test
        @DisplayName("should throw DomainException when page is negative")
        void of_ShouldThrowDomainException_WhenPageIsNegative() {
            // when & then
            thenThrownBy(() -> SimplePageable.of(-1, 10))
                    .as("Should throw when page is negative")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PAGE_NEGATIVE);
        }

        @Test
        @DisplayName("should throw DomainException when size is less than 1")
        void of_ShouldThrowDomainException_WhenSizeIsLessThanOne() {
            // when & then
            thenThrownBy(() -> SimplePageable.of(0, 0))
                    .as("Should throw when size < 1")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_SIZE_LESS_THAN_ONE);
        }
    }

    @Nested
    @DisplayName("of(int pageNumber, int pageSize, Sort sort)")
    class OfFull {

        @Test
        @DisplayName("should create pageable with all parameters")
        void of_ShouldCreatePageable_WhenAllParametersAreValid() {
            // given
            Sort sort = Sort.asc("name");

            // when
            Pageable pageable = SimplePageable.of(3, 25, sort);

            // then
            then(pageable.getPageNumber())
                    .as("Page should match")
                    .isEqualTo(3);
            then(pageable.getPageSize())
                    .as("Size should match")
                    .isEqualTo(25);
            then(pageable.getSort())
                    .as("Sort should match")
                    .isEqualTo(sort);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when sort is null")
        void of_ShouldThrowDomainException_WhenSortIsNull() {
            // given
            Sort nullSort = null;

            // when & then
            thenThrownBy(() -> SimplePageable.of(0, 10, nullSort))
                    .as("Should throw when sort is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_SORT_NULL);
        }
    }

    @Nested
    @DisplayName("getOffset")
    class GetOffset {

        @Test
        @DisplayName("should return zero offset for first page")
        void getOffset_ShouldReturnZero_WhenOnFirstPage() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);

            // when & then
            then(pageable.getOffset())
                    .as("Offset should be zero for first page")
                    .isZero();
        }

        @Test
        @DisplayName("should return correct offset for other pages")
        void getOffset_ShouldReturnCorrectOffset_WhenOnOtherPage() {
            // given
            Pageable pageable = SimplePageable.of(3, 10);

            // when & then
            then(pageable.getOffset())
                    .as("Offset should be 30 (3 * 10)")
                    .isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("nextPage")
    class NextPage {

        @Test
        @DisplayName("should increment page number")
        void nextPage_ShouldIncrementPageNumber_WhenCalled() {
            // given
            Pageable pageable = SimplePageable.of(2, 10);

            // when
            Pageable next = pageable.nextPage();

            // then
            then(next.getPageNumber())
                    .as("Next page should be 3")
                    .isEqualTo(3);
            then(next.getPageSize())
                    .as("Size should be preserved")
                    .isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("previousOrFirstPage")
    class PreviousOrFirstPage {

        @Test
        @DisplayName("should return previous page when not on first")
        void previousOrFirstPage_ShouldReturnPrevious_WhenNotOnFirst() {
            // given
            Pageable pageable = SimplePageable.of(2, 10);

            // when
            Pageable previous = pageable.previousOrFirstPage();

            // then
            then(previous.getPageNumber())
                    .as("Previous page should be 1")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should return first page when on first page")
        void previousOrFirstPage_ShouldReturnFirst_WhenOnFirstPage() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);

            // when
            Pageable result = pageable.previousOrFirstPage();

            // then
            then(result.getPageNumber())
                    .as("Should remain on first page")
                    .isZero();
        }
    }

    @Nested
    @DisplayName("firstPage")
    class FirstPage {

        @Test
        @DisplayName("should reset to first page")
        void firstPage_ShouldResetToFirstPage_WhenCalled() {
            // given
            Pageable pageable = SimplePageable.of(5, 10);

            // when
            Pageable first = pageable.firstPage();

            // then
            then(first.getPageNumber())
                    .as("Should be first page")
                    .isZero();
            then(first.getPageSize())
                    .as("Size should be preserved")
                    .isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("hasPrevious")
    class HasPrevious {

        @Test
        @DisplayName("should return true when page > 0")
        void hasPrevious_ShouldReturnTrue_WhenPageIsGreaterThanZero() {
            // given
            Pageable pageable = SimplePageable.of(1, 10);

            // when & then
            then(pageable.hasPrevious())
                    .as("Should have previous")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when on first page")
        void hasPrevious_ShouldReturnFalse_WhenOnFirstPage() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);

            // when & then
            then(pageable.hasPrevious())
                    .as("Should not have previous on first page")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when all fields are same")
        void equals_ShouldBeEqual_WhenAllFieldsAreSame() {
            // given
            Sort sort = Sort.asc("name");
            Pageable p1 = SimplePageable.of(2, 10, sort);
            Pageable p2 = SimplePageable.of(2, 10, sort);

            // when & then
            then(p1)
                    .as("Equal pageable should be equal")
                    .isEqualTo(p2);
            then(p1.hashCode())
                    .as("Equal pageable should have same hash")
                    .isEqualTo(p2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when page differs")
        void equals_ShouldNotBeEqual_WhenPageDiffers() {
            // given
            Pageable p1 = SimplePageable.of(1, 10);
            Pageable p2 = SimplePageable.of(2, 10);

            // when & then
            then(p1)
                    .as("Different pageable should not be equal")
                    .isNotEqualTo(p2);
        }

        @Test
        @DisplayName("should not be equal when size differs")
        void equals_ShouldNotBeEqual_WhenSizeDiffers() {
            // given
            Pageable p1 = SimplePageable.of(0, 10);
            Pageable p2 = SimplePageable.of(0, 20);

            // when & then
            then(p1)
                    .as("Different pageable should not be equal")
                    .isNotEqualTo(p2);
        }

        @Test
        @DisplayName("should not be equal when sort differs")
        void equals_ShouldNotBeEqual_WhenSortDiffers() {
            // given
            Pageable p1 = SimplePageable.of(Sort.asc("name"));
            Pageable p2 = SimplePageable.of(Sort.desc("name"));

            // when & then
            then(p1)
                    .as("Different pageable should not be equal")
                    .isNotEqualTo(p2);
        }

        @SuppressWarnings({"EqualsWithItself", "ConstantValue"})
        @Test
        @DisplayName("should return true when comparing with itself")
        void equals_ShouldReturnTrue_WhenComparingWithItself() {
            // given
            Pageable pageable = SimplePageable.of(2, 10);

            // when
            boolean result = pageable.equals(pageable);

            // then
            then(result)
                    .as("Pageable should be equal to itself")
                    .isTrue();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return false when comparing with null")
        void equals_ShouldReturnFalse_WhenComparingWithNull() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);

            // when
            boolean result = pageable.equals(null);

            // then
            then(result)
                    .as("Pageable should not be equal to null")
                    .isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("should return false when comparing with different type")
        void equals_ShouldReturnFalse_WhenComparingWithDifferentType() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);

            // when
            boolean result = pageable.equals("not a pageable");

            // then
            then(result)
                    .as("Pageable should not be equal to different type")
                    .isFalse();
        }

        @Test
        @DisplayName("should return true when all fields are same")
        void equals_ShouldReturnTrue_WhenAllFieldsAreSame() {
            // given
            Sort sort = Sort.asc("name");
            Pageable p1 = SimplePageable.of(2, 10, sort);
            Pageable p2 = SimplePageable.of(2, 10, sort);

            // when
            boolean result = p1.equals(p2);

            // then
            then(result)
                    .as("Pageable with same fields should be equal")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when pageNumber differs")
        void equals_ShouldReturnFalse_WhenPageNumberDiffers() {
            // given
            Pageable p1 = SimplePageable.of(1, 10);
            Pageable p2 = SimplePageable.of(2, 10);

            // when
            boolean result = p1.equals(p2);

            // then
            then(result)
                    .as("Pageable with different pageNumber should not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("should return false when pageSize differs")
        void equals_ShouldReturnFalse_WhenPageSizeDiffers() {
            // given
            Pageable p1 = SimplePageable.of(0, 10);
            Pageable p2 = SimplePageable.of(0, 20);

            // when
            boolean result = p1.equals(p2);

            // then
            then(result)
                    .as("Pageable with different pageSize should not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("should return false when sort differs")
        void equals_ShouldReturnFalse_WhenSortDiffers() {
            // given
            Pageable p1 = SimplePageable.of(Sort.asc("name"));
            Pageable p2 = SimplePageable.of(Sort.desc("name"));

            // when
            boolean result = p1.equals(p2);

            // then
            then(result)
                    .as("   Pageable with different sort should not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("should be symmetric")
        void equals_ShouldBeSymmetric_WhenComparing() {
            // given
            Pageable p1 = SimplePageable.of(2, 10);
            Pageable p2 = SimplePageable.of(2, 10);

            // when
            boolean result1 = p1.equals(p2);
            boolean result2 = p2.equals(p1);

            // then
            then(result1)
                    .as("equals should be symmetric")
                    .isEqualTo(result2);
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void equals_ShouldHaveConsistentHashCode_WhenEqual() {
            // given
            Pageable p1 = SimplePageable.of(2, 10);
            Pageable p2 = SimplePageable.of(2, 10);

            // when & then
            then(p1.hashCode())
                    .as("Equal pageable should have same hashCode")
                    .isEqualTo(p2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should contain class name and fields")
        void toString_ShouldContainClassNameAndFields_WhenCalled() {
            // given
            Pageable pageable = SimplePageable.of(2, 15);

            // when
            String result = pageable.toString();

            // then
            then(result)
                    .as("Should contain page")
                    .contains("page=2")
                    .as("Should contain size")
                    .contains("size=15");
        }
    }
}
