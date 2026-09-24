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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link SimplePage}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("SimplePage")
class SimplePageTests {

    private static final String ERROR_CONTENT_NULL = "The content list must not be null";
    private static final String ERROR_PAGEABLE_NULL = "The pageable list must not be null";
    private static final String ERROR_TOTAL_NEGATIVE = "The total elements must not be negative";
    private static final String ERROR_CONVERTER_NULL = "Function converter must not be null";

    @Nested
    @DisplayName("of")
    class Of {

        @Test
        @DisplayName("should create page with valid parameters")
        void of_ShouldCreatePage_WhenParametersAreValid() {
            // given
            List<String> content = List.of("a", "b", "c");
            Pageable pageable = SimplePageable.of(0, 10);
            long totalElements = 25;

            // when
            Page<String> page = SimplePage.of(content, pageable, totalElements);

            // then
            then(page.getContent())
                    .as("Content should match")
                    .containsExactly("a", "b", "c");
            then(page.getTotalElements())
                    .as("Total elements should match")
                    .isEqualTo(25);
            then(page.getTotalPages())
                    .as("Total pages should be 3 (25/10)")
                    .isEqualTo(3);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when content is null")
        void of_ShouldThrowDomainException_WhenContentIsNull() {
            // given
            List<String> nullContent = null;
            Pageable pageable = SimplePageable.of();

            // when & then
            thenThrownBy(() ->
                    SimplePage.of(nullContent, pageable, 0))
                    .as("Should throw when content is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CONTENT_NULL);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when pageable is null")
        void of_ShouldThrowDomainException_WhenPageableIsNull() {
            // given
            List<String> content = List.of("a");
            Pageable nullPageable = null;

            // when & then
            thenThrownBy(() ->
                    SimplePage.of(content, nullPageable, 0))
                    .as("Should throw when pageable is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PAGEABLE_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when totalElements is negative")
        void of_ShouldThrowDomainException_WhenTotalElementsIsNegative() {
            // given
            List<String> content = List.of("a");
            Pageable pageable = SimplePageable.of();

            // when & then
            thenThrownBy(() ->
                    SimplePage.of(content, pageable, -1))
                    .as("Should throw when totalElements is negative")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_TOTAL_NEGATIVE);
        }

        @Test
        @DisplayName("should accept zero totalElements")
        void of_ShouldAcceptZeroTotalElements_WhenProvided() {
            // given
            List<String> content = Collections.emptyList();
            Pageable pageable = SimplePageable.of();

            // when
            Page<String> page = SimplePage.of(content, pageable, 0);

            // then
            then(page.getTotalElements())
                    .as("Total elements should be zero")
                    .isZero();
            then(page.getTotalPages())
                    .as("Total pages should be zero")
                    .isZero();
        }
    }

    @Nested
    @DisplayName("empty")
    class Empty {

        @Test
        @DisplayName("should create empty page")
        void empty_ShouldCreateEmptyPage_WhenCalled() {
            // given
            Pageable pageable = SimplePageable.of();

            // when
            Page<String> page = SimplePage.empty(pageable);

            // then
            then(page.getContent())
                    .as("Content should be empty")
                    .isEmpty();
            then(page.getTotalElements())
                    .as("Total elements should be zero")
                    .isZero();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when pageable is null")
        void empty_ShouldThrowDomainException_WhenPageableIsNull() {
            // given
            Pageable nullPageable = null;

            // when & then
            thenThrownBy(() -> SimplePage.empty(nullPageable))
                    .as("Should throw when pageable is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_PAGEABLE_NULL);
        }
    }

    @Nested
    @DisplayName("map")
    class Map {

        @Test
        @DisplayName("should transform content using converter")
        void map_ShouldTransformContent_WhenConverterIsValid() {
            // given
            List<String> content = List.of("a", "bb", "ccc");
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page = SimplePage.of(content, pageable, 3);

            // when
            Page<Integer> mapped = page.map(String::length);

            // then
            then(mapped.getContent())
                    .as("Content should be transformed")
                    .containsExactly(1, 2, 3);
            then(mapped.getTotalElements())
                    .as("Total elements should be preserved")
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("should throw DomainException when converter is null")
        void map_ShouldThrowDomainException_WhenConverterIsNull() {
            // given
            Page<String> page = SimplePage.of(
                    List.of("a"), SimplePageable.of(), 1);

            // when & then
            thenThrownBy(() -> page.map(null))
                    .as("Should throw when converter is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CONVERTER_NULL);
        }
    }

    @Nested
    @DisplayName("isFirst and isLast")
    class IsFirstAndIsLast {

        @Test
        @DisplayName("should be first on page 0")
        void isFirst_ShouldReturnTrue_WhenOnPageZero() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page = SimplePage.of(
                    List.of("a"), pageable, 30);

            // when & then
            then(page.isFirst())
                    .as("Page 0 should be first")
                    .isTrue();
        }

        @Test
        @DisplayName("should not be first on page > 0")
        void isFirst_ShouldReturnFalse_WhenOnPageGreaterThanZero() {
            // given
            Pageable pageable = SimplePageable.of(1, 10);
            Page<String> page = SimplePage.of(
                    List.of("a"), pageable, 30);

            // when & then
            then(page.isFirst())
                    .as("Page 1 should not be first")
                    .isFalse();
        }

        @Test
        @DisplayName("should be last on last page")
        void isLast_ShouldReturnTrue_WhenOnLastPage() {
            // given
            Pageable pageable = SimplePageable.of(2, 10);
            Page<String> page = SimplePage.of(
                    List.of("a"), pageable, 25);

            // when & then
            then(page.isLast())
                    .as("Last page should be last")
                    .isTrue();
        }

        @Test
        @DisplayName("should not be last when more pages exist")
        void isLast_ShouldReturnFalse_WhenMorePagesExist() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page = SimplePage.of(
                    List.of("a"), pageable, 30);

            // when & then
            then(page.isLast())
                    .as("First page should not be last")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("should be equal when content and pageable are same")
        void equals_ShouldBeEqual_WhenContentAndPageableAreSame() {
            // given
            List<String> content = List.of("a", "b");
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page1 = SimplePage.of(content, pageable, 2);
            Page<String> page2 = SimplePage.of(content, pageable, 2);

            // when & then
            then(page1)
                    .as("Pages with same content and pageable should be equal")
                    .isEqualTo(page2);
            then(page1.hashCode())
                    .as("Equal pages should have same hash")
                    .isEqualTo(page2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when content differs")
        void equals_ShouldNotBeEqual_WhenContentDiffers() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page1 = SimplePage.of(
                    List.of("a"), pageable, 1);
            Page<String> page2 = SimplePage.of(
                    List.of("b"), pageable, 1);

            // when & then
            then(page1)
                    .as("Pages with different content should not be equal")
                    .isNotEqualTo(page2);
        }

        @SuppressWarnings({"EqualsWithItself", "ConstantValue"})
        @Test
        @DisplayName("should return true when comparing with itself")
        void equals_ShouldReturnTrue_WhenComparingWithItself() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page = SimplePage.of(
                    List.of("a", "b"), pageable, 2);

            // when
            boolean result = page.equals(page);

            // then
            then(result)
                    .as("Page should be equal to itself")
                    .isTrue();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return false when comparing with null")
        void equals_ShouldReturnFalse_WhenComparingWithNull() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page = SimplePage.of(
                    List.of("a"), pageable, 1);

            // when
            boolean result = page.equals(null);

            // then
            then(result)
                    .as("Page should not be equal to null")
                    .isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("should return false when comparing with different type")
        void equals_ShouldReturnFalse_WhenComparingWithDifferentType() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page = SimplePage.of(
                    List.of("a"), pageable, 1);

            // when
            boolean result = page.equals("not a page");

            // then
            then(result)
                    .as("Page should not be equal to different type")
                    .isFalse();
        }

        @Test
        @DisplayName("should return true when content and pageable are same")
        void equals_ShouldReturnTrue_WhenContentAndPageableAreSame() {
            // given
            List<String> content = List.of("a", "b");
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page1 = SimplePage.of(content, pageable, 2);
            Page<String> page2 = SimplePage.of(content, pageable, 2);

            // when
            boolean result = page1.equals(page2);

            // then
            then(result)
                    .as("Pages with same content and pageable should be equal")
                    .isTrue();
        }

        @Test
        @DisplayName("should return false when content differs")
        void equals_ShouldReturnFalse_WhenContentDiffers() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page1 = SimplePage.of(
                    List.of("a"), pageable, 1);
            Page<String> page2 = SimplePage.of(
                    List.of("b"), pageable, 1);

            // when
            boolean result = page1.equals(page2);

            // then
            then(result)
                    .as("Pages with different content should not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("should return false when pageable differs")
        void equals_ShouldReturnFalse_WhenPageableDiffers() {
            // given
            List<String> content = List.of("a");
            Pageable pageable1 = SimplePageable.of(0, 10);
            Pageable pageable2 = SimplePageable.of(1, 10);
            Page<String> page1 = SimplePage.of(content, pageable1, 1);
            Page<String> page2 = SimplePage.of(content, pageable2, 1);

            // when
            boolean result = page1.equals(page2);

            // then
            then(result)
                    .as("Pages with different pageable should not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("should be symmetric")
        void equals_ShouldBeSymmetric_WhenComparing() {
            // given
            List<String> content = List.of("a");
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page1 = SimplePage.of(content, pageable, 1);
            Page<String> page2 = SimplePage.of(content, pageable, 1);

            // when
            boolean result1 = page1.equals(page2);
            boolean result2 = page2.equals(page1);

            // then
            then(result1)
                    .as("equals should be symmetric")
                    .isEqualTo(result2);
        }

        @Test
        @DisplayName("should have consistent hashCode")
        void equals_ShouldHaveConsistentHashCode_WhenEqual() {
            // given
            List<String> content = List.of("a");
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page1 = SimplePage.of(content, pageable, 1);
            Page<String> page2 = SimplePage.of(content, pageable, 1);

            // when & then
            then(page1.hashCode())
                    .as("Equal pages should have same hashCode")
                    .isEqualTo(page2.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should contain class name and metadata")
        void toString_ShouldContainClassNameAndMetadata_WhenCalled() {
            // given
            Pageable pageable = SimplePageable.of(0, 10);
            Page<String> page = SimplePage.of(List.of("a"), pageable, 25);

            // when
            String result = page.toString();

            // then
            then(result)
                    .as("Should contain class name")
                    .contains("Page")
                    .as("Should contain totalElements")
                    .contains("totalElements=25")
                    .as("Should contain totalPages")
                    .contains("totalPages=3");
        }
    }
}
