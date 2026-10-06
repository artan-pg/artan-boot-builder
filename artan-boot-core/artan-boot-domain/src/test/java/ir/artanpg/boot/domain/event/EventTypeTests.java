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

    private static final String VALID_NAME = "OrderPlaced";
    private static final String VALID_CATEGORY = "order";
    private static final String VALID_VERSION = "v1";
    private static final String ERROR_NAME_NULL_OR_BLANK =
            "The event type name cannot be null or blank";
    private static final String ERROR_NAME_INVALID_CHARS =
            "The event type name may contain only ASCII letters, digits and '-'";
    private static final String ERROR_CATEGORY_INVALID_CHARS =
            "The event type category may contain only ASCII letters, digits and '-'";
    private static final String ERROR_VERSION_INVALID_CHARS =
            "The event type version may contain only ASCII letters, digits and '-'";
    private static final String ERROR_TOTAL_LENGTH =
            "The combined length of name, category and version must not exceed 64 characters";
    private static final String ERROR_CACHE_KEY_NULL = "The cache key cannot be null";
    private static final String ERROR_CACHE_KEY_MALFORMED = "The cache key is malformed; expected 3 segments";

    @Nested
    @DisplayName("of(name)")
    class OfName {

        @Test
        @DisplayName("should create EventType with name only")
        void of_ShouldCreateEventType_WhenValidNameProvided() {
            // given
            // nothing

            // when
            EventType eventType = EventType.of(VALID_NAME);

            // then
            then(eventType.getName()).isEqualTo(VALID_NAME);
            then(eventType.getCategory()).isNull();
            then(eventType.getVersion()).isNull();
            then(eventType.hasCategory()).isFalse();
            then(eventType.hasVersion()).isFalse();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when name is null")
        void of_ShouldThrowDomainEventException_WhenNameIsNull() {
            // given
            String nullName = null;

            // when & then
            thenThrownBy(() -> EventType.of(nullName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is blank")
        void of_ShouldThrowDomainEventException_WhenNameIsBlank() {
            // given
            String blankName = "   ";

            // when & then
            thenThrownBy(() -> EventType.of(blankName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is empty")
        void of_ShouldThrowDomainEventException_WhenNameIsEmpty() {
            // given
            String emptyName = "";

            // when & then
            thenThrownBy(() -> EventType.of(emptyName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name contains invalid characters")
        void of_ShouldThrowDomainEventException_WhenNameContainsInvalidCharacters() {
            // given
            String invalidName = "Order_Placed";

            // when & then
            thenThrownBy(() -> EventType.of(invalidName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_INVALID_CHARS);
        }
    }

    @Nested
    @DisplayName("of(name, category)")
    class OfNameCategory {

        @Test
        @DisplayName("should create EventType with name and category")
        void of_ShouldCreateEventType_WhenValidNameAndCategoryProvided() {
            // given
            // nothing

            // when
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY);

            // then
            then(eventType.getName()).isEqualTo(VALID_NAME);
            then(eventType.getCategory()).isEqualTo(VALID_CATEGORY);
            then(eventType.getVersion()).isNull();
            then(eventType.hasCategory()).isTrue();
            then(eventType.hasVersion()).isFalse();
        }

        @Test
        @DisplayName("should treat blank category as null")
        void of_ShouldTreatBlankCategoryAsNull_WhenCategoryIsBlank() {
            // given
            String blankCategory = "   ";

            // when
            EventType eventType = EventType.of(VALID_NAME, blankCategory);

            // then
            then(eventType.getCategory()).isNull();
            then(eventType.hasCategory()).isFalse();
        }

        @Test
        @DisplayName("should treat null category as null")
        void of_ShouldTreatNullCategoryAsNull_WhenCategoryIsNull() {
            // given
            String nullCategory = null;

            // when
            EventType eventType = EventType.of(VALID_NAME, nullCategory);

            // then
            then(eventType.getCategory()).isNull();
            then(eventType.hasCategory()).isFalse();
        }

        @Test
        @DisplayName("should throw DomainEventException when category contains invalid characters")
        void of_ShouldThrowDomainEventException_WhenCategoryContainsInvalidCharacters() {
            // given
            String invalidCategory = "order@domain";

            // when & then
            thenThrownBy(() -> EventType.of(VALID_NAME, invalidCategory))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_CATEGORY_INVALID_CHARS);
        }
    }

    @Nested
    @DisplayName("of(name, category, version)")
    class OfNameCategoryVersion {

        @Test
        @DisplayName("should create EventType with all coordinates")
        void of_ShouldCreateEventType_WhenAllCoordinatesProvided() {
            // given
            // nothing

            // when
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);

            // then
            then(eventType.getName()).isEqualTo(VALID_NAME);
            then(eventType.getCategory()).isEqualTo(VALID_CATEGORY);
            then(eventType.getVersion()).isEqualTo(VALID_VERSION);
            then(eventType.hasCategory()).isTrue();
            then(eventType.hasVersion()).isTrue();
        }

        @Test
        @DisplayName("should treat blank version as null")
        void of_ShouldTreatBlankVersionAsNull_WhenVersionIsBlank() {
            // given
            String blankVersion = "   ";

            // when
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY, blankVersion);

            // then
            then(eventType.getVersion()).isNull();
            then(eventType.hasVersion()).isFalse();
        }

        @Test
        @DisplayName("should treat null version as null")
        void of_ShouldTreatNullVersionAsNull_WhenVersionIsNull() {
            // given
            String nullVersion = null;

            // when
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY, nullVersion);

            // then
            then(eventType.getVersion()).isNull();
            then(eventType.hasVersion()).isFalse();
        }

        @Test
        @DisplayName("should throw DomainEventException when version contains invalid characters")
        void of_ShouldThrowDomainEventException_WhenVersionContainsInvalidCharacters() {
            // given
            String invalidVersion = "v1.0";

            // when & then
            thenThrownBy(() -> EventType.of(VALID_NAME, VALID_CATEGORY, invalidVersion))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_VERSION_INVALID_CHARS);
        }

        @Test
        @DisplayName("should throw DomainEventException when total length exceeds maximum")
        void of_ShouldThrowDomainEventException_WhenTotalLengthExceedsMaximum() {
            // given
            String longName = "A".repeat(30);
            String longCategory = "B".repeat(20);
            String longVersion = "C".repeat(15);

            // when & then
            thenThrownBy(() -> EventType.of(longName, longCategory, longVersion))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining(ERROR_TOTAL_LENGTH);
        }

        @Test
        @DisplayName("should accept total length exactly at maximum")
        void of_ShouldAcceptTotalLength_WhenExactlyAtMaximum() {
            // given
            String name = "A".repeat(30);
            String category = "B".repeat(20);
            String version = "C".repeat(14);

            // when
            EventType eventType = EventType.of(name, category, version);

            // then
            then(eventType.getName()).isEqualTo(name);
            then(eventType.getCategory()).isEqualTo(category);
            then(eventType.getVersion()).isEqualTo(version);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should create EventType using builder with all coordinates")
        void builder_ShouldCreateEventType_WhenAllCoordinatesProvided() {
            // given
            // nothing

            // when
            EventType eventType = EventType.builder()
                    .name(VALID_NAME)
                    .category(VALID_CATEGORY)
                    .version(VALID_VERSION)
                    .build();

            // then
            then(eventType.getName()).isEqualTo(VALID_NAME);
            then(eventType.getCategory()).isEqualTo(VALID_CATEGORY);
            then(eventType.getVersion()).isEqualTo(VALID_VERSION);
        }

        @Test
        @DisplayName("should create EventType using builder with name only")
        void builder_ShouldCreateEventType_WhenOnlyNameProvided() {
            // given
            // nothing

            // when
            EventType eventType = EventType.builder()
                    .name(VALID_NAME)
                    .build();

            // then
            then(eventType.getName()).isEqualTo(VALID_NAME);
            then(eventType.getCategory()).isNull();
            then(eventType.getVersion()).isNull();
        }

        @Test
        @DisplayName("should throw DomainEventException when name is null in builder")
        void builder_ShouldThrowDomainEventException_WhenNameIsNull() {
            // given
            String nullName = null;

            // when & then
            thenThrownBy(() -> EventType.builder()
                    .name(nullName)
                    .build())
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is blank in builder")
        void builder_ShouldThrowDomainEventException_WhenNameIsBlank() {
            // given
            String blankName = "   ";

            // when & then
            thenThrownBy(() -> EventType.builder()
                    .name(blankName)
                    .build())
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should allow null category in builder")
        void builder_ShouldAllowNullCategory_WhenCategoryIsNull() {
            // given
            String nullCategory = null;

            // when
            EventType eventType = EventType.builder()
                    .name(VALID_NAME)
                    .category(nullCategory)
                    .build();

            // then
            then(eventType.getCategory()).isNull();
        }

        @Test
        @DisplayName("should allow null version in builder")
        void builder_ShouldAllowNullVersion_WhenVersionIsNull() {
            // given
            String nullVersion = null;

            // when
            EventType eventType = EventType.builder()
                    .name(VALID_NAME)
                    .version(nullVersion)
                    .build();

            // then
            then(eventType.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("toCacheKey")
    class ToCacheKey {

        @Test
        @DisplayName("should generate cache key with all coordinates")
        void toCacheKey_ShouldGenerateCacheKey_WhenAllCoordinatesPresent() {
            // given
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);

            // when
            String cacheKey = eventType.toCacheKey();

            // then
            then(cacheKey).isEqualTo(VALID_NAME + '\u0000' + VALID_CATEGORY + '\u0000' + VALID_VERSION);
        }

        @Test
        @DisplayName("should generate cache key with empty category and version")
        void toCacheKey_ShouldGenerateCacheKey_WhenOnlyNamePresent() {
            // given
            EventType eventType = EventType.of(VALID_NAME);

            // when
            String cacheKey = eventType.toCacheKey();

            // then
            then(cacheKey).isEqualTo(VALID_NAME + '\u0000' + '\u0000');
        }

        @Test
        @DisplayName("should generate cache key with empty version")
        void toCacheKey_ShouldGenerateCacheKey_WhenNameAndCategoryPresent() {
            // given
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY);

            // when
            String cacheKey = eventType.toCacheKey();

            // then
            then(cacheKey).isEqualTo(VALID_NAME + '\u0000' + VALID_CATEGORY + '\u0000');
        }
    }

    @Nested
    @DisplayName("fromCacheKey")
    class FromCacheKey {

        @Test
        @DisplayName("should parse cache key with all coordinates")
        void fromCacheKey_ShouldParseCacheKey_WhenAllCoordinatesPresent() {
            // given
            String cacheKey = VALID_NAME + '\u0000' + VALID_CATEGORY + '\u0000' + VALID_VERSION;

            // when
            EventType eventType = EventType.fromCacheKey(cacheKey);

            // then
            then(eventType.getName()).isEqualTo(VALID_NAME);
            then(eventType.getCategory()).isEqualTo(VALID_CATEGORY);
            then(eventType.getVersion()).isEqualTo(VALID_VERSION);
        }

        @Test
        @DisplayName("should parse cache key with name only")
        void fromCacheKey_ShouldParseCacheKey_WhenOnlyNamePresent() {
            // given
            String cacheKey = VALID_NAME + '\u0000' + '\u0000';

            // when
            EventType eventType = EventType.fromCacheKey(cacheKey);

            // then
            then(eventType.getName()).isEqualTo(VALID_NAME);
            then(eventType.getCategory()).isNull();
            then(eventType.getVersion()).isNull();
        }

        @Test
        @DisplayName("should parse cache key with name and category")
        void fromCacheKey_ShouldParseCacheKey_WhenNameAndCategoryPresent() {
            // given
            String cacheKey = VALID_NAME + '\u0000' + VALID_CATEGORY + '\u0000';

            // when
            EventType eventType = EventType.fromCacheKey(cacheKey);

            // then
            then(eventType.getName()).isEqualTo(VALID_NAME);
            then(eventType.getCategory()).isEqualTo(VALID_CATEGORY);
            then(eventType.getVersion()).isNull();
        }

        @Test
        @DisplayName("should parse cache key with name and category and version")
        void fromCacheKey_ShouldParseCacheKey_WhenNameAndCategoryAndVersionPresent() {
            // given
            String cacheKey = VALID_NAME + '\u0000' + VALID_CATEGORY + '\u0000' + VALID_VERSION;

            // when
            EventType eventType = EventType.fromCacheKey(cacheKey);

            // then
            then(eventType.getName()).isEqualTo(VALID_NAME);
            then(eventType.getCategory()).isEqualTo(VALID_CATEGORY);
            then(eventType.getVersion()).isEqualTo(VALID_VERSION);
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw DomainEventException when cache key is null")
        void fromCacheKey_ShouldThrowDomainEventException_WhenCacheKeyIsNull() {
            // given
            String nullCacheKey = null;

            // when & then
            thenThrownBy(() -> EventType.fromCacheKey(nullCacheKey))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_CACHE_KEY_NULL);
        }

        @Test
        @DisplayName("should throw DomainEventException when cache key is malformed")
        void fromCacheKey_ShouldThrowDomainEventException_WhenCacheKeyIsMalformed() {
            // given
            String malformedCacheKey = "invalid-key";

            // when & then
            thenThrownBy(() -> EventType.fromCacheKey(malformedCacheKey))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_CACHE_KEY_MALFORMED);
        }

        @Test
        @DisplayName("should throw DomainEventException when cache key has too many segments")
        void fromCacheKey_ShouldThrowDomainEventException_WhenCacheKeyHasTooManySegments() {
            // given
            String tooManySegments = "a\u0000b\u0000c\u0000d";

            // when & then
            thenThrownBy(() -> EventType.fromCacheKey(tooManySegments))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_CACHE_KEY_MALFORMED);
        }

        @Test
        @DisplayName("should be reversible with toCacheKey")
        void fromCacheKey_ShouldBeReversible_WhenRoundTrip() {
            // given
            EventType original = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);

            // when
            String cacheKey = original.toCacheKey();
            EventType restored = EventType.fromCacheKey(cacheKey);

            // then
            then(restored).isEqualTo(original);
        }
    }

    @Nested
    @DisplayName("equals")
    class Equals {

        @Test
        @DisplayName("should be equal when all coordinates match")
        void equals_ShouldBeEqual_WhenAllCoordinatesMatch() {
            // given
            EventType eventType1 = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);
            EventType eventType2 = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);

            // when & then
            then(eventType1).isEqualTo(eventType2);
        }

        @Test
        @DisplayName("should not be equal when names differ")
        void equals_ShouldNotBeEqual_WhenNamesDiffer() {
            // given
            EventType eventType1 = EventType.of("OrderPlaced", VALID_CATEGORY, VALID_VERSION);
            EventType eventType2 = EventType.of("OrderCancelled", VALID_CATEGORY, VALID_VERSION);

            // when & then
            then(eventType1).isNotEqualTo(eventType2);
        }

        @Test
        @DisplayName("should not be equal when categories differ")
        void equals_ShouldNotBeEqual_WhenCategoriesDiffer() {
            // given
            EventType eventType1 = EventType.of(VALID_NAME, "order", VALID_VERSION);
            EventType eventType2 = EventType.of(VALID_NAME, "invoice", VALID_VERSION);

            // when & then
            then(eventType1).isNotEqualTo(eventType2);
        }

        @Test
        @DisplayName("should not be equal when versions differ")
        void equals_ShouldNotBeEqual_WhenVersionsDiffer() {
            // given
            EventType eventType1 = EventType.of(VALID_NAME, VALID_CATEGORY, "v1");
            EventType eventType2 = EventType.of(VALID_NAME, VALID_CATEGORY, "v2");

            // when & then
            then(eventType1).isNotEqualTo(eventType2);
        }

        @Test
        @DisplayName("should not be equal when one has category and other does not")
        void equals_ShouldNotBeEqual_WhenOneHasCategoryAndOtherDoesNot() {
            // given
            EventType eventType1 = EventType.of(VALID_NAME, VALID_CATEGORY);
            EventType eventType2 = EventType.of(VALID_NAME);

            // when & then
            then(eventType1).isNotEqualTo(eventType2);
        }

        @Test
        @DisplayName("should be equal to itself")
        void equals_ShouldBeEqualToItself_WhenCompared() {
            // given
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);

            // when & then
            then(eventType).isEqualTo(eventType);
        }

        @Test
        @DisplayName("should not be equal to null")
        void equals_ShouldNotBeEqualToNull_WhenCompared() {
            // given
            EventType eventType = EventType.of(VALID_NAME);

            // when & then
            then(eventType).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal to different type")
        void equals_ShouldNotBeEqualToDifferentType_WhenCompared() {
            // given
            EventType eventType = EventType.of(VALID_NAME);

            // when & then
            then(eventType).isNotEqualTo("not an event type");
        }
    }

    @Nested
    @DisplayName("hashCode")
    class HashCode {

        @Test
        @DisplayName("should return same hash code for equal instances")
        void hashCode_ShouldReturnSameHashCode_WhenEqual() {
            // given
            EventType eventType1 = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);
            EventType eventType2 = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);

            // when & then
            then(eventType1.hashCode()).isEqualTo(eventType2.hashCode());
        }

        @Test
        @DisplayName("should return different hash codes for different instances")
        void hashCode_ShouldReturnDifferentHashCodes_WhenDifferent() {
            // given
            EventType eventType1 = EventType.of("OrderPlaced", VALID_CATEGORY, VALID_VERSION);
            EventType eventType2 = EventType.of("OrderCancelled", VALID_CATEGORY, VALID_VERSION);

            // when & then
            then(eventType1.hashCode()).isNotEqualTo(eventType2.hashCode());
        }

        @Test
        @DisplayName("should be consistent on multiple calls")
        void hashCode_ShouldBeConsistent_WhenCalledMultipleTimes() {
            // given
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);

            // when
            int hashCode1 = eventType.hashCode();
            int hashCode2 = eventType.hashCode();

            // then
            then(hashCode1).isEqualTo(hashCode2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should contain all coordinates")
        void toString_ShouldContainAllCoordinates_WhenCalled() {
            // given
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY, VALID_VERSION);

            // when
            String result = eventType.toString();

            // then
            then(result)
                    .contains("EventType")
                    .contains("name='" + VALID_NAME + "'")
                    .contains("category='" + VALID_CATEGORY + "'")
                    .contains("version='" + VALID_VERSION + "'");
        }

        @Test
        @DisplayName("should contain name only when category and version are null")
        void toString_ShouldContainNameOnly_WhenCategoryAndVersionAreNull() {
            // given
            EventType eventType = EventType.of(VALID_NAME);

            // when
            String result = eventType.toString();

            // then
            then(result).contains("name='" + VALID_NAME + "'");
        }
    }

    @Nested
    @DisplayName("Validation edge cases")
    class ValidationEdgeCases {

        @Test
        @DisplayName("should accept name with dashes")
        void of_ShouldAcceptNameWithDashes_WhenNameContainsDashes() {
            // given
            String nameWithDashes = "Order-Placed-Event";

            // when
            EventType eventType = EventType.of(nameWithDashes);

            // then
            then(eventType.getName()).isEqualTo(nameWithDashes);
        }

        @Test
        @DisplayName("should accept category with dashes")
        void of_ShouldAcceptCategoryWithDashes_WhenCategoryContainsDashes() {
            // given
            String categoryWithDashes = "order-domain";

            // when
            EventType eventType = EventType.of(VALID_NAME, categoryWithDashes);

            // then
            then(eventType.getCategory()).isEqualTo(categoryWithDashes);
        }

        @Test
        @DisplayName("should accept version with dashes")
        void of_ShouldAcceptVersionWithDashes_WhenVersionContainsDashes() {
            // given
            String versionWithDashes = "v1-beta";

            // when
            EventType eventType = EventType.of(VALID_NAME, VALID_CATEGORY, versionWithDashes);

            // then
            then(eventType.getVersion()).isEqualTo(versionWithDashes);
        }

        @Test
        @DisplayName("should reject name with underscore")
        void of_ShouldRejectNameWithUnderscore_WhenNameContainsUnderscore() {
            // given
            String nameWithUnderscore = "Order_Placed";

            // when & then
            thenThrownBy(() -> EventType.of(nameWithUnderscore))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_INVALID_CHARS);
        }

        @Test
        @DisplayName("should reject category with dot")
        void of_ShouldRejectCategoryWithDot_WhenCategoryContainsDot() {
            // given
            String categoryWithDot = "order.domain";

            // when & then
            thenThrownBy(() -> EventType.of(VALID_NAME, categoryWithDot))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_CATEGORY_INVALID_CHARS);
        }

        @Test
        @DisplayName("should reject version with space")
        void of_ShouldRejectVersionWithSpace_WhenVersionContainsSpace() {
            // given
            String versionWithSpace = "v 1";

            // when & then
            thenThrownBy(() -> EventType.of(VALID_NAME, VALID_CATEGORY, versionWithSpace))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_VERSION_INVALID_CHARS);
        }
    }
}
