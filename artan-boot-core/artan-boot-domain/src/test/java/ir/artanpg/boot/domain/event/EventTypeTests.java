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

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenCode;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link EventType} and {@link EventTypeRegistry}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("EventType")
class EventTypeTests {

    private static final String EVENT_TYPE_NAME = "TEST_EVENT";
    private static final String ANOTHER_EVENT_TYPE_NAME = "ANOTHER_EVENT";
    private static final String BLANK_NAME = "   ";
    private static final String ERROR_NAME_NULL_OR_BLANK = "The name cannot be null or blank";

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

        @Test
        @DisplayName("should throw DomainException when name is null")
        void valueOf_ShouldThrowDomainException_WhenNameIsNull() {
            // when & then
            thenThrownBy(() -> EventType.valueOf(null))
                    .as("Should throw DomainException when name is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainException when name is blank")
        void valueOf_ShouldThrowDomainException_WhenNameIsBlank() {
            // when & then
            thenThrownBy(() -> EventType.valueOf(BLANK_NAME))
                    .as("Should throw DomainException when name is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainException when name is empty")
        void valueOf_ShouldThrowDomainException_WhenNameIsEmpty() {
            // when & then
            thenThrownBy(() -> EventType.valueOf(""))
                    .as("Should throw DomainException when name is empty")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainException when name contains whitespace")
        void valueOf_ShouldThrowDomainException_WhenNameContainsWhitespace() {
            // when & then
            thenThrownBy(() -> EventType.valueOf("ORDER PLACED"))
                    .as("Malformed names must be rejected by the grammar")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("not a valid EventType name");
        }

        @Test
        @DisplayName("should throw DomainException when name exceeds max length")
        void valueOf_ShouldThrowDomainException_WhenNameIsTooLong() {
            // given
            String longName = "A".repeat(129);

            // when & then
            thenThrownBy(() -> EventType.valueOf(longName))
                    .as("Names longer than 128 chars must be rejected")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("not a valid EventType name");
        }
    }

    @Nested
    @DisplayName("named")
    class Named {

        @Test
        @DisplayName("should create a non-cached instance with the given name")
        void named_ShouldCreateInstance_WhenNameIsValid() {
            // when
            EventType type = EventType.named(EVENT_TYPE_NAME);

            // then
            then(type.getName()).isEqualTo(EVENT_TYPE_NAME);
            then(type).isInstanceOf(EventType.NamedEventType.class);
        }

        @Test
        @DisplayName("should reject invalid names like valueOf does")
        void named_ShouldThrow_WhenNameIsInvalid() {
            thenThrownBy(() -> EventType.named(null)).isInstanceOf(DomainException.class);
            thenThrownBy(() -> EventType.named("  ")).isInstanceOf(DomainException.class);
            thenThrownBy(() -> EventType.named("bad name")).isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("Equality contract")
    class EqualityContract {

        @Test
        @DisplayName("cached instances with same name should be equal")
        void equals_ShouldReturnTrue_WhenSameNameViaValueOf() {
            // given
            EventType t1 = EventType.valueOf("order.placed");
            EventType t2 = EventType.valueOf("order.placed");

            // when & then
            then(t1).isEqualTo(t2);
            then(t1.hashCode()).isEqualTo(t2.hashCode());
        }

        @Test
        @DisplayName("non-cached instance with same name should equal cached instance (regression: identity bug)")
        void equals_ShouldReturnTrue_WhenNonCachedMatchesCached() {
            // given
            EventType cached = EventType.valueOf("OrderShippedEvent");
            EventType nonCached = EventType.named("OrderShippedEvent");

            // when & then — this was FALSE before the name-based equality fix
            then(nonCached.equals(cached))
                    .as("Non-cached vs cached instance must be equal by name")
                    .isTrue();
            then(cached.equals(nonCached))
                    .as("Equality must be symmetric")
                    .isTrue();
            then(nonCached.hashCode())
                    .as("Equal objects must share hash code")
                    .isEqualTo(cached.hashCode());
        }

        @Test
        @DisplayName("named holder should equal cached instance of same name")
        void equals_ShouldReturnTrue_AcrossAllFactories() {
            // given
            EventType viaValueOf = EventType.valueOf("InvoiceIssued");
            EventType viaNamed = EventType.named("InvoiceIssued");

            // when & then
            then(viaValueOf).isEqualTo(viaNamed);
            then(viaNamed.hashCode()).isEqualTo(viaValueOf.hashCode());
        }

        @Test
        @DisplayName("different names should not be equal")
        void equals_ShouldReturnFalse_WhenNamesDiffer() {
            // given
            EventType t1 = EventType.valueOf(EVENT_TYPE_NAME);
            EventType t2 = EventType.valueOf(ANOTHER_EVENT_TYPE_NAME);

            // when & then
            then(t1).isNotEqualTo(t2);
        }

        @Test
        @DisplayName("equals should be reflexive, symmetric and transitive")
        void equals_ShouldSatisfyContract_WhenComparedAcrossInstances() {
            // given
            EventType a = EventType.valueOf("evt.x");
            EventType b = EventType.named("evt.x");
            EventType c = EventType.valueOf("evt.x");
            EventType other = EventType.valueOf("evt.y");

            // when & then — reflexivity
            then(a).isEqualTo(a);
            // symmetry
            then(a.equals(b)).isEqualTo(b.equals(a));
            then(a.equals(other)).isEqualTo(other.equals(a));
            // transitivity
            then(a).isEqualTo(b);
            then(b).isEqualTo(c);
            then(a).isEqualTo(c);
        }

        @Test
        @DisplayName("equals should return false for null and foreign types")
        void equals_ShouldReturnFalse_WhenArgumentIsNullOrForeign() {
            // given
            EventType type = EventType.valueOf(EVENT_TYPE_NAME);

            // when & then
            then(type.equals(null)).as("Null comparison").isFalse();
            then(type.equals(EVENT_TYPE_NAME)).as("String with same text is not an EventType").isFalse();
        }

        @Test
        @DisplayName("instances with same name should deduplicate in a HashSet")
        void hashCode_ShouldSupportSetDeduplication_WhenNamesMatch() {
            // given
            Set<EventType> set = new HashSet<>();

            // when
            set.add(EventType.valueOf("dup.name"));
            set.add(EventType.named("dup.name"));
            set.add(EventType.valueOf("other.name"));

            // then
            then(set).as("Only distinct names should remain").hasSize(2);
            then(set.contains(EventType.named("dup.name"))).isTrue();
        }

        @Test
        @DisplayName("toString should render the event type name")
        void toString_ShouldReturnName_WhenCalled() {
            // given
            EventType type = EventType.valueOf("order.placed.v1");

            // when & then
            then(type.toString()).isEqualTo("order.placed.v1");
        }
    }

    @Nested
    @DisplayName("Implementations")
    class FunctionalInterfaceTests {

        @Test
        @DisplayName("custom implementation should interoperate via name-based equality")
        void customImplementation_ShouldBeComparableByName_WhenProvided() {
            // given — anonymous class implementing the single abstract method
            EventType custom = new EventType() {
                @Override
                public String getName() {
                    return "CUSTOM_EVENT";
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (!(o instanceof EventType that)) return false;
                    return java.util.Objects.equals(getName(), that.getName());
                }

                @Override
                public int hashCode() {
                    return java.util.Objects.hashCode(getName());
                }
            };

            // when & then
            then(custom.getName()).isEqualTo("CUSTOM_EVENT");
            then(custom)
                    .as("Custom impl must equal factory instances by name")
                    .isEqualTo(EventType.valueOf("CUSTOM_EVENT"))
                    .isEqualTo(EventType.named("CUSTOM_EVENT"));
        }

        @Test
        @DisplayName("should no longer implement Serializable")
        void serializable_ShouldNotBeImplemented_WhenCreated() {
            // given
            EventType eventType = EventType.valueOf(EVENT_TYPE_NAME);

            // when & then — serialization moved to EventSerializer/Deserializer ports
            then(eventType)
                    .as("EventType must not carry the Serializable marker anymore")
                    .isNotInstanceOf(java.io.Serializable.class);
        }
    }

    @Nested
    @DisplayName("Bounded cache")
    class BoundedCache {

        @Test
        @DisplayName("cache should never grow beyond its bound under hostile input")
        void cache_ShouldStayBounded_WhenManyDistinctNamesAreResolved() {
            // given
            int hostileNames = 6_000; // > MAX_CACHED_TYPES (4096)

            // when
            for (int i = 0; i < hostileNames; i++) {
                EventType.valueOf("hostile.name." + i);
            }

            // then — interning evicts LRU entries; correctness never depends on identity
            then(EventType.valueOf("hostile.name.0"))
                    .as("Re-resolving an evicted name still yields an equal instance")
                    .isEqualTo(EventType.named("hostile.name.0"));
        }

        @Test
        @DisplayName("clearing the cache must not break equality")
        void equality_ShouldSurviveCacheClear_WhenCacheIsCleared() {
            // given
            EventType before = EventType.valueOf("survivor.name");

            // when
            EventType.EventTypeCache.CACHE.clearCache();
            EventType after = EventType.valueOf("survivor.name");

            // then
            then(after).as("New instance after eviction must still be equal")
                    .isEqualTo(before)
                    .isNotSameAs(before);
        }
    }

    @Nested
    @DisplayName("EventTypeRegistry")
    class Registry {

        @Test
        @DisplayName("register should return canonical instance and mark name registered")
        void register_ShouldStoreName_WhenValidTypeProvided() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();

            // when
            EventType canonical = registry.register(EventType.valueOf("OrderPlacedEvent"));

            // then
            then(registry.isRegistered("OrderPlacedEvent")).isTrue();
            then(registry.isRegistered(canonical)).isTrue();
            then(registry.register("OrderPlacedEvent"))
                    .as("Registration is idempotent and returns the same instance")
                    .isSameAs(canonical);
            then(registry.register(EventType.named("OrderPlacedEvent")))
                    .as("Any instance of the same name maps to the canonical one")
                    .isSameAs(canonical);
        }

        @Test
        @DisplayName("registered types should survive strict-mode resolve")
        void resolve_ShouldReturnCanonical_WhenNameIsRegisteredAndStrict() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();
            EventType canonical = registry.register("order.shipped.v2");
            registry.setStrictMode(true);

            // when
            EventType resolved = registry.resolve("order.shipped.v2");

            // then
            then(resolved).isSameAs(canonical);
        }

        @Test
        @DisplayName("strict mode should reject unregistered names")
        void resolve_ShouldThrow_WhenNameUnregisteredInStrictMode() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();
            registry.setStrictMode(true);

            // when & then
            thenThrownBy(() -> registry.resolve("never.registered"))
                    .as("Unknown types must fail fast in strict mode")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Unknown EventType");
        }

        @Test
        @DisplayName("non-strict mode should lazily cache well-formed names")
        void resolve_ShouldCacheLazily_WhenNonStrict() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();

            // when
            EventType resolved = registry.resolve("lazy.name");

            // then
            then(resolved.getName()).isEqualTo("lazy.name");
            then(registry.isRegistered("lazy.name"))
                    .as("Lazy resolution is caching, not registration")
                    .isFalse();
            then(registry.resolve("lazy.name")).isSameAs(resolved);
        }

        @Test
        @DisplayName("malformed names should be rejected even in non-strict mode")
        void resolve_ShouldRejectMalformed_WhenGrammarViolated() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();

            // when & then
            thenThrownBy(() -> registry.resolve("has spaces"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("not a valid EventType name");
            thenThrownBy(() -> registry.resolve(null))
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
            thenThrownBy(() -> registry.resolve(" "))
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("unregister should remove the name and report idempotently")
        void unregister_ShouldRemoveName_WhenPreviouslyRegistered() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();
            registry.register("temp.name");

            // when & then
            then(registry.unregister("temp.name")).as("First unregister reports removal").isTrue();
            then(registry.unregister("temp.name")).as("Second unregister is a no-op").isFalse();
            then(registry.unregister(null)).as("Null unregister is a no-op").isFalse();
            then(registry.isRegistered("temp.name")).as("Name gone after unregister").isFalse();
        }

        @Test
        @DisplayName("strict resolve should fail again after unregistering")
        void resolve_ShouldFailAfterUnregister_WhenStrict() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();
            registry.register("temp.name");
            registry.setStrictMode(true);

            // when
            registry.unregister("temp.name");

            // then
            thenThrownBy(() -> registry.resolve("temp.name"))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Unknown EventType");
        }

        @Test
        @DisplayName("registeredNames should expose an immutable snapshot")
        void registeredNames_ShouldReturnSnapshot_WhenQueried() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();
            registry.register("a.name");
            registry.register("b.name");

            // when
            Set<String> names = registry.registeredNames();

            // then
            then(names).containsExactlyInAnyOrder("a.name", "b.name");
            thenCode(() -> names.add("c.name")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("null event type should not be registrable")
        void register_ShouldThrow_WhenTypeIsNull() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();

            // when & then
            thenThrownBy(() -> registry.register((EventType) null))
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The eventType cannot be null");
        }

        @Test
        @DisplayName("registry capacity should be bounded")
        void register_ShouldThrow_WhenCapacityExceeded() {
            // given
            EventTypeRegistry registry = new EventTypeRegistry();

            // when
            for (int i = 0; i < 4096; i++) {
                registry.register("cap.name." + i);
            }

            // then
            thenThrownBy(() -> registry.register("cap.name.overflow"))
                    .as("Registry must refuse unbounded growth")
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("exceeded");
        }

        @Test
        @DisplayName("shared registry should exist and be stable")
        void shared_ShouldReturnSameInstance_WhenCalledRepeatedly() {
            then(EventTypeRegistry.shared()).isSameAs(EventTypeRegistry.shared());
        }
    }
}
