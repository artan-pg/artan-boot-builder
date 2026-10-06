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

import ir.artanpg.boot.application.port.driven.event.EventTypeCacheStore;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link DefaultEventTypeRegistry}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultEventTypeRegistry")
class DefaultEventTypeRegistryTests {

    private static final String EVENT_NAME = "OrderPlaced";
    private static final String EVENT_CATEGORY = "order";
    private static final String EVENT_VERSION = "v1";
    private static final String ERROR_CACHE_STORE_NULL = "The EventTypeCacheStore cannot be null";
    private static final String ERROR_EVENT_TYPE_NULL = "The event type cannot be null";

    @Mock
    private EventTypeCacheStore cacheStore;

    private DefaultEventTypeRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultEventTypeRegistry(cacheStore);
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create registry with valid cache store")
        void constructor_ShouldCreateRegistry_WhenCacheStoreIsValid() {
            // given
            // nothing

            // when
            DefaultEventTypeRegistry newRegistry = new DefaultEventTypeRegistry(cacheStore);

            // then
            then(newRegistry).isNotNull();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when cache store is null")
        void constructor_ShouldThrowDomainEventException_WhenCacheStoreIsNull() {
            // given
            EventTypeCacheStore nullCacheStore = null;

            // when & then
            thenThrownBy(() -> new DefaultEventTypeRegistry(nullCacheStore))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_CACHE_STORE_NULL);
        }
    }

    @Nested
    @DisplayName("get")
    class GetTests {

        @Test
        @DisplayName("should return cached type when already present")
        void get_ShouldReturnCachedType_WhenAlreadyPresent() {
            // given
            EventType cachedType = EventType.of(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);
            given(cacheStore.getIfPresent(cachedType.toCacheKey())).willReturn(cachedType);

            // when
            EventType result = registry.get(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);

            // then
            then(result).isSameAs(cachedType);
        }

        @Test
        @DisplayName("should intern and return new type when not cached")
        void get_ShouldInternAndReturnNewType_WhenNotCached() {
            // given
            given(cacheStore.getIfPresent(any())).willReturn(null);
            given(cacheStore.putIfAbsent(any(), any())).willAnswer(invocation -> invocation.getArgument(1));

            // when
            EventType result = registry.get(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);

            // then
            then(result).isNotNull();
            then(result.getName()).isEqualTo(EVENT_NAME);
            then(result.getCategory()).isEqualTo(EVENT_CATEGORY);
            then(result.getVersion()).isEqualTo(EVENT_VERSION);
            verify(cacheStore).putIfAbsent(any(), any(EventType.class));
        }

        @Test
        @DisplayName("should handle null category and version")
        void get_ShouldHandleNullCategoryAndVersion_WhenProvided() {
            // given
            given(cacheStore.getIfPresent(any())).willReturn(null);
            given(cacheStore.putIfAbsent(any(), any())).willAnswer(invocation -> invocation.getArgument(1));

            // when
            EventType result = registry.get(EVENT_NAME, null, null);

            // then
            then(result).isNotNull();
            then(result.getName()).isEqualTo(EVENT_NAME);
            then(result.getCategory()).isNull();
            then(result.getVersion()).isNull();
        }

        @Test
        @DisplayName("should handle blank category and version as null")
        void get_ShouldHandleBlankCategoryAndVersion_WhenProvided() {
            // given
            given(cacheStore.getIfPresent(any())).willReturn(null);
            given(cacheStore.putIfAbsent(any(), any())).willAnswer(invocation -> invocation.getArgument(1));

            // when
            EventType result = registry.get(EVENT_NAME, "   ", "   ");

            // then
            then(result).isNotNull();
            then(result.getName()).isEqualTo(EVENT_NAME);
            then(result.getCategory()).isNull();
            then(result.getVersion()).isNull();
        }
    }

    @Nested
    @DisplayName("intern")
    class InternTests {

        @Test
        @DisplayName("should return existing type when already cached")
        void intern_ShouldReturnExistingType_WhenAlreadyCached() {
            // given
            EventType existingType = EventType.of(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);
            given(cacheStore.getIfPresent(existingType.toCacheKey())).willReturn(existingType);

            // when
            EventType newType = EventType.of(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);
            EventType result = registry.intern(newType);

            // then
            then(result).isSameAs(existingType);
        }

        @Test
        @DisplayName("should store and return new type when not cached")
        void intern_ShouldStoreAndReturnNewType_WhenNotCached() {
            // given
            EventType newType = EventType.of(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);
            given(cacheStore.getIfPresent(newType.toCacheKey())).willReturn(null);
            given(cacheStore.putIfAbsent(newType.toCacheKey(), newType)).willReturn(newType);

            // when
            EventType result = registry.intern(newType);

            // then
            then(result).isSameAs(newType);
            verify(cacheStore).putIfAbsent(newType.toCacheKey(), newType);
        }

        @Test
        @DisplayName("should return existing when putIfAbsent returns previous value")
        void intern_ShouldReturnExisting_WhenPutIfAbsentReturnsPrevious() {
            // given
            EventType newType = EventType.of(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);
            EventType existingType = EventType.of(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);
            given(cacheStore.getIfPresent(newType.toCacheKey())).willReturn(null);
            given(cacheStore.putIfAbsent(newType.toCacheKey(), newType)).willReturn(existingType);

            // when
            EventType result = registry.intern(newType);

            // then
            then(result).isSameAs(existingType);
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when type is null")
        void intern_ShouldThrowDomainEventException_WhenTypeIsNull() {
            // given
            EventType nullType = null;

            // when & then
            thenThrownBy(() -> registry.intern(nullType))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_EVENT_TYPE_NULL);
            verifyNoInteractions(cacheStore);
        }
    }

    @Nested
    @DisplayName("contains")
    class ContainsTests {

        @Test
        @DisplayName("should return true when type exists in cache")
        void contains_ShouldReturnTrue_WhenTypeExistsInCache() {
            // given
            EventType probe = EventType.of(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);
            given(cacheStore.getIfPresent(probe.toCacheKey())).willReturn(probe);

            // when
            boolean result = registry.contains(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("should return false when type does not exist in cache")
        void contains_ShouldReturnFalse_WhenTypeDoesNotExistInCache() {
            // given
            given(cacheStore.getIfPresent(any())).willReturn(null);

            // when
            boolean result = registry.contains(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("should handle null category and version")
        void contains_ShouldHandleNullCategoryAndVersion_WhenProvided() {
            // given
            EventType probe = EventType.of(EVENT_NAME, null, null);
            given(cacheStore.getIfPresent(probe.toCacheKey())).willReturn(null);

            // when
            boolean result = registry.contains(EVENT_NAME, null, null);

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("evict")
    class EvictTests {

        @Test
        @DisplayName("should invalidate entry in cache")
        void evict_ShouldInvalidateEntryInCache_WhenCalled() {
            // given
            EventType probe = EventType.of(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);

            // when
            registry.evict(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);

            // then
            verify(cacheStore).invalidate(probe.toCacheKey());
        }

        @Test
        @DisplayName("should handle null category and version")
        void evict_ShouldHandleNullCategoryAndVersion_WhenProvided() {
            // given
            EventType probe = EventType.of(EVENT_NAME, null, null);

            // when
            registry.evict(EVENT_NAME, null, null);

            // then
            verify(cacheStore).invalidate(probe.toCacheKey());
        }

        @Test
        @DisplayName("should not throw when evicting non-existent entry")
        void evict_ShouldNotThrow_WhenEvictingNonExistentEntry() {
            // given
            EventType probe = EventType.of(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);

            // when
            registry.evict(EVENT_NAME, EVENT_CATEGORY, EVENT_VERSION);

            // then
            verify(cacheStore).invalidate(probe.toCacheKey());
        }
    }

    @Nested
    @DisplayName("clear")
    class ClearTests {

        @Test
        @DisplayName("should invalidate all entries in cache")
        void clear_ShouldInvalidateAllEntriesInCache_WhenCalled() {
            // given
            // nothing

            // when
            registry.clear();

            // then
            verify(cacheStore).invalidateAll();
        }

        @Test
        @DisplayName("should not throw when cache is already empty")
        void clear_ShouldNotThrow_WhenCacheIsEmpty() {
            // given
            // nothing

            // when
            registry.clear();

            // then
            verify(cacheStore).invalidateAll();
        }
    }

    @Nested
    @DisplayName("equals")
    class EqualsTests {

        @SuppressWarnings("EqualsWithItself")
        @Test
        @DisplayName("equals should return true when comparing object with itself")
        void equals_ShouldReturnTrue_WhenComparingObjectWithItself() {
            // given

            // when
            boolean result = registry.equals(registry);

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("should be equal when same instance")
        void equals_ShouldBeEqual_WhenSameInstance() {
            // given
            EventTypeCacheStore typeCacheStore = cacheStore;
            DefaultEventTypeRegistry otherRegister = new DefaultEventTypeRegistry(typeCacheStore);

            // when
            boolean result = registry.equals(otherRegister);

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("should not be equal when cache stores differ")
        void equals_ShouldNotBeEqual_WhenCacheStoresDiffer() {
            // given
            EventTypeCacheStore otherStore = mock(EventTypeCacheStore.class);
            DefaultEventTypeRegistry otherRegistry = new DefaultEventTypeRegistry(otherStore);

            // when
            boolean result = registry.equals(otherRegistry);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("should not be equal to null")
        void equals_ShouldNotBeEqualToNull_WhenCompared() {
            // given
            // nothing

            // when
            boolean result = registry.equals(null);

            // then
            then(result).isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("should not be equal to different type")
        void equals_ShouldNotBeEqualToDifferentType_WhenCompared() {
            // given
            // nothing

            // when
            boolean result = registry.equals("not a registry");

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hashCode")
    class HashCodeTests {

        @Test
        @DisplayName("should return same hash code for equal registries")
        void hashCode_ShouldReturnSameHashCode_WhenRegistriesAreEqual() {
            // given
            EventTypeCacheStore sameStore = cacheStore;
            DefaultEventTypeRegistry otherRegistry = new DefaultEventTypeRegistry(sameStore);

            // when
            int hash1 = registry.hashCode();
            int hash2 = otherRegistry.hashCode();

            // then
            then(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("should return hash code based on cache store")
        void hashCode_ShouldReturnHashCodeBasedOnCacheStore_WhenCalled() {
            // given
            // nothing

            // when
            int hashCode = registry.hashCode();

            // then
            then(hashCode).isEqualTo(cacheStore.hashCode());
        }

        @Test
        @DisplayName("should be consistent on multiple calls")
        void hashCode_ShouldBeConsistent_WhenCalledMultipleTimes() {
            // given
            // nothing

            // when
            int hash1 = registry.hashCode();
            int hash2 = registry.hashCode();

            // then
            then(hash1).isEqualTo(hash2);
        }
    }
}
