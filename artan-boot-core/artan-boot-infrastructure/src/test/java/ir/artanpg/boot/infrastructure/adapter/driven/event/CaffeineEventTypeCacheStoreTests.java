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

package ir.artanpg.boot.infrastructure.adapter.driven.event;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link CaffeineEventTypeCacheStore}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("CaffeineEventTypeCacheStore")
class CaffeineEventTypeCacheStoreTests {

    private static final String KEY_EXCEPTION_MESSAGE = "The key cannot be null or blank";
    private static final String MAXIMUM_SIZE_EXCEPTION_MESSAGE = "The maximumSize must be positive";
    private static final String VALUE_EXCEPTION_MESSAGE = "The value cannot be null";
    private static final String CACHE_EXCEPTION_MESSAGE = "The Caffeine cache cannot be null";
    private static final String EXPIRE_EXCEPTION_MESSAGE =
            "The expireAfterWrite duration cannot be null, zero, or negative";

    @SuppressWarnings("DataFlowIssue")
    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create store with default constructor")
        void constructor_ShouldCreateStore_WhenDefaultConstructorUsed() {
            // given
            // nothing

            // when
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore();

            // then
            EventType type = EventType.of("OrderPlaced");
            then(store.putIfAbsent(type.toCacheKey(), type)).isSameAs(type);
        }

        @Test
        @DisplayName("should create store with positive maximumSize")
        void constructor_ShouldCreateStore_WhenMaximumSizeIsPositive() {
            // given
            long maximumSize = 100;

            // when
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(maximumSize);

            // then
            EventType type = EventType.of("OrderPlaced");
            then(store.putIfAbsent(type.toCacheKey(), type)).isSameAs(type);
        }

        @Test
        @DisplayName("should throw DomainEventException when maximumSize is zero")
        void constructor_ShouldThrowDomainEventException_WhenMaximumSizeIsZero() {
            // given
            long maximumSize = 0;

            // when & then
            thenThrownBy(() -> new CaffeineEventTypeCacheStore(maximumSize))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(MAXIMUM_SIZE_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when maximumSize is negative")
        void constructor_ShouldThrowDomainEventException_WhenMaximumSizeIsNegative() {
            // given
            long maximumSize = -1;

            // when & then
            thenThrownBy(() -> new CaffeineEventTypeCacheStore(maximumSize))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(MAXIMUM_SIZE_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should create store with valid maximumSize and expireAfterWrite")
        void constructor_ShouldCreateStore_WhenMaximumSizeAndExpireAfterWriteAreValid() {
            // given
            long maximumSize = 100;
            Duration expireAfterWrite = Duration.ofMinutes(10);

            // when
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(maximumSize, expireAfterWrite);

            // then
            EventType type = EventType.of("OrderPlaced");
            then(store.putIfAbsent(type.toCacheKey(), type)).isSameAs(type);
        }

        @Test
        @DisplayName("should throw DomainEventException when expireAfterWrite is null")
        void constructor_ShouldThrowDomainEventException_WhenExpireAfterWriteIsNull() {
            // given
            long maximumSize = 100;
            Duration expireAfterWrite = null;

            // when & then
            thenThrownBy(() -> new CaffeineEventTypeCacheStore(maximumSize, expireAfterWrite))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(EXPIRE_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when expireAfterWrite is zero")
        void constructor_ShouldThrowDomainEventException_WhenExpireAfterWriteIsZero() {
            // given
            long maximumSize = 100;
            Duration expireAfterWrite = Duration.ZERO;

            // when & then
            thenThrownBy(() -> new CaffeineEventTypeCacheStore(maximumSize, expireAfterWrite))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(EXPIRE_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when expireAfterWrite is negative")
        void constructor_ShouldThrowDomainEventException_WhenExpireAfterWriteIsNegative() {
            // given
            long maximumSize = 100;
            Duration expireAfterWrite = Duration.ofSeconds(-1);

            // when & then
            thenThrownBy(() -> new CaffeineEventTypeCacheStore(maximumSize, expireAfterWrite))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(EXPIRE_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when maximumSize is negative and expireAfterWrite in valid")
        void constructor_ShouldThrowDomainEventException_WhenMaximumSizeIsNegativeAndExpireAfterWriteInValid() {
            // given
            long maximumSize = -1;
            Duration expireAfterWrite = Duration.ofSeconds(100);

            // when & then
            thenThrownBy(() -> new CaffeineEventTypeCacheStore(maximumSize, expireAfterWrite))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(MAXIMUM_SIZE_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should create store with valid Caffeine cache")
        void constructor_ShouldCreateStore_WhenCaffeineCacheIsValid() {
            // given
            Cache<String, EventType> cache = Caffeine.newBuilder().maximumSize(100).build();

            // when
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(cache);

            // then
            EventType type = EventType.of("OrderPlaced");
            then(store.putIfAbsent(type.toCacheKey(), type)).isSameAs(type);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainEventException when Caffeine cache is null")
        void constructor_ShouldThrowDomainEventException_WhenCaffeineCacheIsNull() {
            // given
            Cache<String, EventType> cache = null;

            // when & then
            thenThrownBy(() -> new CaffeineEventTypeCacheStore(cache))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(CACHE_EXCEPTION_MESSAGE);
        }
    }

    @Nested
    @DisplayName("getIfPresent")
    class GetIfPresentTests {

        @Test
        @DisplayName("should return EventType when key exists")
        void getIfPresent_ShouldReturnEventType_WhenKeyExists() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            EventType type = EventType.of("OrderPlaced");
            store.putIfAbsent(type.toCacheKey(), type);

            // when
            EventType result = store.getIfPresent(type.toCacheKey());

            // then
            then(result).isSameAs(type);
        }

        @Test
        @DisplayName("should return null when key does not exist")
        void getIfPresent_ShouldReturnNull_WhenKeyDoesNotExist() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String key = "nonexistent-key";

            // when
            EventType result = store.getIfPresent(key);

            // then
            then(result).isNull();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when key is null")
        void getIfPresent_ShouldThrowDomainEventException_WhenKeyIsNull() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String nullKey = null;

            // when & then
            thenThrownBy(() -> store.getIfPresent(nullKey))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(KEY_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when key is blank")
        void getIfPresent_ShouldThrowDomainEventException_WhenKeyIsBlank() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String blankKey = "   ";

            // when & then
            thenThrownBy(() -> store.getIfPresent(blankKey))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(KEY_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when key is empty")
        void getIfPresent_ShouldThrowDomainEventException_WhenKeyIsEmpty() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String emptyKey = "";

            // when & then
            thenThrownBy(() -> store.getIfPresent(emptyKey))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(KEY_EXCEPTION_MESSAGE);
        }
    }

    @Nested
    @DisplayName("putIfAbsent")
    class PutIfAbsentTests {

        @Test
        @DisplayName("should store and return value when key does not exist")
        void putIfAbsent_ShouldStoreAndReturnValue_WhenKeyDoesNotExist() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            EventType type = EventType.of("OrderPlaced");
            String key = type.toCacheKey();

            // when
            EventType result = store.putIfAbsent(key, type);

            // then
            then(result).isSameAs(type);
            then(store.getIfPresent(key)).isSameAs(type);
        }

        @Test
        @DisplayName("should return previous value when key exists")
        void putIfAbsent_ShouldReturnPreviousValue_WhenKeyExists() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            EventType first = EventType.of("OrderPlaced", "order", "v1");
            EventType second = EventType.of("OrderPlaced", "order", "v1");
            String key = first.toCacheKey();
            store.putIfAbsent(key, first);

            // when
            EventType result = store.putIfAbsent(key, second);

            // then
            then(result).isSameAs(first);
            then(store.getIfPresent(key)).isSameAs(first);
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when key is null")
        void putIfAbsent_ShouldThrowDomainEventException_WhenKeyIsNull() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String nullKey = null;
            EventType type = EventType.of("OrderPlaced");

            // when & then
            thenThrownBy(() -> store.putIfAbsent(nullKey, type))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(KEY_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when key is blank")
        void putIfAbsent_ShouldThrowDomainEventException_WhenKeyIsBlank() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String blankKey = "   ";
            EventType type = EventType.of("OrderPlaced");

            // when & then
            thenThrownBy(() -> store.putIfAbsent(blankKey, type))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(KEY_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when key is empty")
        void putIfAbsent_ShouldThrowDomainEventException_WhenKeyIsEmpty() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String emptyKey = "";
            EventType type = EventType.of("OrderPlaced");

            // when & then
            thenThrownBy(() -> store.putIfAbsent(emptyKey, type))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(KEY_EXCEPTION_MESSAGE);
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when value is null")
        void putIfAbsent_ShouldThrowDomainEventException_WhenValueIsNull() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String key = "test-key";
            EventType nullValue = null;

            // when & then
            thenThrownBy(() -> store.putIfAbsent(key, nullValue))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(VALUE_EXCEPTION_MESSAGE);
        }
    }

    @Nested
    @DisplayName("invalidate")
    class InvalidateTests {

        @Test
        @DisplayName("should remove entry when key exists")
        void invalidate_ShouldRemoveEntry_WhenKeyExists() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            EventType type = EventType.of("OrderPlaced");
            String key = type.toCacheKey();
            store.putIfAbsent(key, type);

            // when
            store.invalidate(key);

            // then
            then(store.getIfPresent(key)).isNull();
        }

        @Test
        @DisplayName("should not throw when key does not exist")
        void invalidate_ShouldNotThrow_WhenKeyDoesNotExist() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String key = "nonexistent-key";

            // when & then
            store.invalidate(key);
            then(store.getIfPresent(key)).isNull();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when key is null")
        void invalidate_ShouldThrowDomainEventException_WhenKeyIsNull() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String nullKey = null;

            // when & then
            thenThrownBy(() -> store.invalidate(nullKey))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(KEY_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when key is blank")
        void invalidate_ShouldThrowDomainEventException_WhenKeyIsBlank() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String blankKey = "   ";

            // when & then
            thenThrownBy(() -> store.invalidate(blankKey))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(KEY_EXCEPTION_MESSAGE);
        }

        @Test
        @DisplayName("should throw DomainEventException when key is empty")
        void invalidate_ShouldThrowDomainEventException_WhenKeyIsEmpty() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            String emptyKey = "";

            // when & then
            thenThrownBy(() -> store.invalidate(emptyKey))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(KEY_EXCEPTION_MESSAGE);
        }
    }

    @Nested
    @DisplayName("invalidateAll")
    class InvalidateAllTests {

        @Test
        @DisplayName("should clear all entries")
        void invalidateAll_ShouldClearAllEntries_WhenCalled() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);
            EventType type1 = EventType.of("OrderPlaced");
            EventType type2 = EventType.of("OrderCancelled");
            store.putIfAbsent(type1.toCacheKey(), type1);
            store.putIfAbsent(type2.toCacheKey(), type2);

            // when
            store.invalidateAll();

            // then
            then(store.getIfPresent(type1.toCacheKey())).isNull();
            then(store.getIfPresent(type2.toCacheKey())).isNull();
        }

        @Test
        @DisplayName("should not throw when cache is empty")
        void invalidateAll_ShouldNotThrow_WhenCacheIsEmpty() {
            // given
            CaffeineEventTypeCacheStore store = new CaffeineEventTypeCacheStore(128);

            // when & then
            store.invalidateAll();
        }
    }
}
