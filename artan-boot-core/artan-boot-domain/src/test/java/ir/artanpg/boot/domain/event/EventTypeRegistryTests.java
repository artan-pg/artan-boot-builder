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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link EventTypeRegistry}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("EventTypeRegistry")
class EventTypeRegistryTests {

    private static final String VALID_NAME = "OrderPlaced";
    private static final String VALID_NAME_2 = "OrderCancelled";
    private static final String VALID_NAME_WITH_CATEGORY = "order.OrderPlaced";
    private static final String VALID_NAME_WITH_VERSION = "order.OrderPlaced.v1";
    private static final String INVALID_NAME = "Invalid Name!";
    private static final String ERROR_NAME_NULL_OR_BLANK = "The name cannot be null or blank";
    private static final String ERROR_MAX_REGISTERED_TYPES = "The maxRegisteredTypes must be positive";

    private EventTypeRegistry eventTypeRegistry;

    @BeforeEach
    void setUp() {
        eventTypeRegistry = new EventTypeRegistry();
        eventTypeRegistry.clear();
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("should create registry with default capacity")
        void constructor_ShouldCreateRegistry_WhenDefaultConstructorUsed() {
            // given

            // when
            EventTypeRegistry registry = new EventTypeRegistry();

            // then
            then(registry).isNotNull();
            then(registry.getMaxRegisteredTypes()).isEqualTo(EventTypeRegistry.DEFAULT_MAX_REGISTERED_TYPES);
        }

        @Test
        @DisplayName("should create registry with custom capacity")
        void constructor_ShouldCreateRegistry_WhenCustomCapacityProvided() {
            // given
            int maxTypes = 100;

            // when
            EventTypeRegistry registry = new EventTypeRegistry(maxTypes);

            // then
            then(registry).isNotNull();
            then(registry.getMaxRegisteredTypes()).isEqualTo(maxTypes);
        }

        @Test
        @DisplayName("should throw DomainEventException when maxRegisteredTypes is zero")
        void constructor_ShouldThrowDomainEventException_WhenMaxRegisteredTypesIsZero() {
            // given
            int maxTypes = 0;

            // when & then
            thenThrownBy(() -> new EventTypeRegistry(maxTypes))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_MAX_REGISTERED_TYPES);
        }

        @Test
        @DisplayName("should throw DomainEventException when maxRegisteredTypes is negative")
        void constructor_ShouldThrowDomainEventException_WhenMaxRegisteredTypesIsNegative() {
            // given
            int maxTypes = -1;

            // when & then
            thenThrownBy(() -> new EventTypeRegistry(maxTypes))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_MAX_REGISTERED_TYPES);
        }
    }

    @Nested
    @DisplayName("shared")
    class Shared {

        @Test
        @DisplayName("should return shared registry instance")
        void shared_ShouldReturnSharedRegistry_WhenCalled() {
            // given
            // nothing

            // when
            EventTypeRegistry shared = EventTypeRegistry.shared();

            // then
            then(shared).isNotNull();
            then(shared).isSameAs(EventTypeRegistry.shared());
        }
    }

    @Nested
    @DisplayName("valueOf")
    class ValueOf {

        @Test
        @DisplayName("should return EventType for valid name")
        void valueOf_ShouldReturnEventType_WhenValidNameProvided() {
            // given
            // nothing

            // when
            EventType eventType = EventTypeRegistry.valueOf(VALID_NAME);

            // then
            then(eventType).isNotNull();
            then(eventType.getName()).isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("should return cached instance for same name")
        void valueOf_ShouldReturnCachedInstance_WhenSameNameProvided() {
            // given
            // nothing

            // when
            EventType eventType1 = EventTypeRegistry.valueOf(VALID_NAME);
            EventType eventType2 = EventTypeRegistry.valueOf(VALID_NAME);

            // then
            then(eventType1).isSameAs(eventType2);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainEventException when name is null")
        void valueOf_ShouldThrowDomainEventException_WhenNameIsNull() {
            // given
            String nullName = null;

            // when & then
            thenThrownBy(() -> EventTypeRegistry.valueOf(nullName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is blank")
        void valueOf_ShouldThrowDomainEventException_WhenNameIsBlank() {
            // given
            String blankName = "   ";

            // when & then
            thenThrownBy(() -> EventTypeRegistry.valueOf(blankName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is empty")
        void valueOf_ShouldThrowDomainEventException_WhenNameIsEmpty() {
            // given
            String emptyName = "";

            // when & then
            thenThrownBy(() -> EventTypeRegistry.valueOf(emptyName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name contains invalid characters")
        void valueOf_ShouldThrowDomainEventException_WhenNameContainsInvalidCharacters() {
            // given

            // when & then
            thenThrownBy(() -> EventTypeRegistry.valueOf(INVALID_NAME))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("is not a valid EventType name");
        }
    }

    @Nested
    @DisplayName("register with EventType")
    class RegisterWithEventType {

        @Test
        @DisplayName("should register EventType successfully")
        void register_ShouldRegisterEventType_WhenValidEventTypeProvided() {
            // given
            EventType eventType = EventType.named(VALID_NAME);

            // when
            EventType registered = eventTypeRegistry.register(eventType);

            // then
            then(registered).isNotNull();
            then(registered.getName()).isEqualTo(VALID_NAME);
            then(eventTypeRegistry.isRegistered(VALID_NAME)).isTrue();
        }

        @Test
        @DisplayName("should return cached instance when registering same name twice")
        void register_ShouldReturnCachedInstance_WhenSameNameRegisteredTwice() {
            // given
            EventType eventType1 = EventType.named(VALID_NAME);
            EventType eventType2 = EventType.named(VALID_NAME);

            // when
            EventType registered1 = eventTypeRegistry.register(eventType1);
            EventType registered2 = eventTypeRegistry.register(eventType2);

            // then
            then(registered1).isSameAs(registered2);
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when eventType is null")
        void register_ShouldThrowDomainEventException_WhenEventTypeIsNull() {
            // given
            EventType nullEventType = null;

            // when & then
            thenThrownBy(() -> eventTypeRegistry.register(nullEventType))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventType cannot be null");
        }

        @Test
        @DisplayName("should throw DomainEventException when registry is full")
        void register_ShouldThrowDomainEventException_WhenRegistryIsFull() {
            // given
            EventTypeRegistry smallRegistry = new EventTypeRegistry(2);
            smallRegistry.register(EventType.named("Event1"));
            smallRegistry.register(EventType.named("Event2"));
            EventType newEventType = EventType.named("Event3");

            // when & then
            thenThrownBy(() -> smallRegistry.register(newEventType))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("exceeded 2 entries");
        }
    }

    @Nested
    @DisplayName("register with String")
    class RegisterWithString {

        @Test
        @DisplayName("should register EventType by name successfully")
        void register_ShouldRegisterEventType_WhenValidNameProvided() {
            // given
            // nothing

            // when
            EventType registered = eventTypeRegistry.register(VALID_NAME);

            // then
            then(registered).isNotNull();
            then(registered.getName()).isEqualTo(VALID_NAME);
            then(eventTypeRegistry.isRegistered(VALID_NAME)).isTrue();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when name is null")
        void register_ShouldThrowDomainEventException_WhenNameIsNull() {
            // given
            String nullName = null;

            // when & then
            thenThrownBy(() -> eventTypeRegistry.register(nullName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is blank")
        void register_ShouldThrowDomainEventException_WhenNameIsBlank() {
            // given
            String blankName = "   ";

            // when & then
            thenThrownBy(() -> eventTypeRegistry.register(blankName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is empty")
        void register_ShouldThrowDomainEventException_WhenNameIsEmpty() {
            // given
            String emptyName = "";

            // when & then
            thenThrownBy(() -> eventTypeRegistry.register(emptyName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }
    }

    @Nested
    @DisplayName("unregister")
    class Unregister {

        @Test
        @DisplayName("should unregister existing name")
        void unregister_ShouldUnregisterExistingName_WhenNameExists() {
            // given
            eventTypeRegistry.register(VALID_NAME);

            // when
            boolean result = eventTypeRegistry.unregister(VALID_NAME);

            // then
            then(result).isTrue();
            then(eventTypeRegistry.isRegistered(VALID_NAME)).isFalse();
        }

        @Test
        @DisplayName("should return false when name does not exist")
        void unregister_ShouldReturnFalse_WhenNameDoesNotExist() {
            // given
            // nothing

            // when
            boolean result = eventTypeRegistry.unregister(VALID_NAME);

            // then
            then(result).isFalse();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return false when name is null")
        void unregister_ShouldReturnFalse_WhenNameIsNull() {
            // given
            String nullName = null;

            // when
            boolean result = eventTypeRegistry.unregister(nullName);

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("should return cached instance for registered name")
        void resolve_ShouldReturnCachedInstance_WhenNameIsRegistered() {
            // given
            EventType registered = eventTypeRegistry.register(VALID_NAME);

            // when
            EventType resolved = eventTypeRegistry.resolve(VALID_NAME);

            // then
            then(resolved).isSameAs(registered);
        }

        @Test
        @DisplayName("should cache and return instance for unregistered name in non-strict mode")
        void resolve_ShouldCacheAndReturnInstance_WhenNameIsUnregisteredInNonStrictMode() {
            // given
            // nothing

            // when
            EventType resolved = eventTypeRegistry.resolve(VALID_NAME);

            // then
            then(resolved).isNotNull();
            then(resolved.getName()).isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("should return cached instance when strict mode is enabled and name is registered")
        void resolve_ShouldReturnCachedInstance_WhenStrictModeEnabledAndNameIsRegistered() {
            // given
            eventTypeRegistry.register(VALID_NAME);
            eventTypeRegistry.setStrictMode(true);

            // when
            EventType resolved = eventTypeRegistry.resolve(VALID_NAME);

            // then
            then(resolved)
                    .as("Should return cached instance in strict mode when registered")
                    .isNotNull();
            then(resolved.getName())
                    .as("Name should match")
                    .isEqualTo(VALID_NAME);
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should return new instance when strict mode is enabled and name is registered but not cached")
        void resolve_ShouldReturnNewInstance_WhenStrictModeEnabledAndNameRegisteredButNotCached() {
            // given
            eventTypeRegistry.register(VALID_NAME);
            eventTypeRegistry.setStrictMode(true);

            // Remove from cachedTypes but keep in registeredNames to simulate edge case
            Field cachedTypesField;
            ConcurrentHashMap<String, EventType> cachedTypes;
            try {
                cachedTypesField = EventTypeRegistry.class.getDeclaredField("cachedTypes");
                cachedTypesField.setAccessible(true);
                cachedTypes = (ConcurrentHashMap<String, EventType>) cachedTypesField.get(eventTypeRegistry);
                cachedTypes.remove(VALID_NAME);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            // when
            EventType resolved = eventTypeRegistry.resolve(VALID_NAME);

            // then
            then(resolved)
                    .as("Should return new instance when strict mode is on and name is registered but not cached")
                    .isNotNull();
            then(resolved.getName())
                    .as("Name should match")
                    .isEqualTo(VALID_NAME);
        }

        @Test
        @DisplayName("should throw DomainEventException for unregistered name in strict mode")
        void resolve_ShouldThrowDomainEventException_WhenNameIsUnregisteredInStrictMode() {
            // given
            eventTypeRegistry.setStrictMode(true);

            // when & then
            thenThrownBy(() -> eventTypeRegistry.resolve(VALID_NAME))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("Unknown EventType");
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainEventException when name is null")
        void resolve_ShouldThrowDomainEventException_WhenNameIsNull() {
            // given
            String nullName = null;

            // when & then
            thenThrownBy(() -> eventTypeRegistry.resolve(nullName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is blank")
        void resolve_ShouldThrowDomainEventException_WhenNameIsBlank() {
            // given
            String blankName = "   ";

            // when & then
            thenThrownBy(() -> eventTypeRegistry.resolve(blankName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name is empty")
        void resolve_ShouldThrowDomainEventException_WhenNameIsEmpty() {
            // given
            String emptyName = "";

            // when & then
            thenThrownBy(() -> eventTypeRegistry.resolve(emptyName))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_NAME_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainEventException when name contains invalid characters")
        void resolve_ShouldThrowDomainEventException_WhenNameContainsInvalidCharacters() {
            // given

            // when & then
            thenThrownBy(() -> eventTypeRegistry.resolve(INVALID_NAME))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("is not a valid EventType name");
        }
    }

    @Nested
    @DisplayName("findByCategory")
    class FindByCategory {

        @Test
        @DisplayName("should return matching event types for category")
        void findByCategory_ShouldReturnMatchingEventTypes_WhenCategoryExists() {
            // given
            eventTypeRegistry.register(VALID_NAME_WITH_CATEGORY);
            eventTypeRegistry.register("order.OrderCancelled");

            // when
            List<EventType> results = eventTypeRegistry.findByCategory("order");

            // then
            then(results).hasSize(2);
            then(results).extracting(EventType::getName)
                    .containsExactly("order.OrderCancelled", VALID_NAME_WITH_CATEGORY);
        }

        @Test
        @DisplayName("should return empty list when no matches found")
        void findByCategory_ShouldReturnEmptyList_WhenNoMatchesFound() {
            // given
            eventTypeRegistry.register(VALID_NAME);

            // when
            List<EventType> results = eventTypeRegistry.findByCategory("nonexistent");

            // then
            then(results).isEmpty();
        }

        @Test
        @DisplayName("should return empty list when category is empty string")
        void findByCategory_ShouldReturnEmptyList_WhenCategoryIsEmptyString() {
            // given
            eventTypeRegistry.register(VALID_NAME_WITH_CATEGORY);

            // when
            List<EventType> results = eventTypeRegistry.findByCategory("");

            // then
            then(results).isEmpty();
        }

        @Test
        @DisplayName("should return event types without category when category is empty string")
        void findByCategory_ShouldReturnEventTypesWithoutCategory_WhenCategoryIsEmptyString() {
            // given
            eventTypeRegistry.register(VALID_NAME);

            // when
            List<EventType> results = eventTypeRegistry.findByCategory("");

            // then
            then(results).hasSize(1);
            then(results.getFirst().getName()).isEqualTo(VALID_NAME);
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should skip names not present in cache")
        void findByCategory_ShouldSkipNamesNotInCache_WhenCacheMiss() {
            // given
            eventTypeRegistry.register(VALID_NAME);

            // Use reflection to remove from cachedTypes but keep in registeredNames
            Field cachedTypesField;
            ConcurrentHashMap<String, EventType> cachedTypes;
            try {
                cachedTypesField = EventTypeRegistry.class.getDeclaredField("cachedTypes");
                cachedTypesField.setAccessible(true);

                cachedTypes = (ConcurrentHashMap<String, EventType>) cachedTypesField.get(eventTypeRegistry);
                cachedTypes.remove(VALID_NAME);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            // when
            List<EventType> results = eventTypeRegistry.findByCategory("");

            // then
            then(results)
                    .as("Should skip names not in cache")
                    .isEmpty();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when category is null")
        void findByCategory_ShouldThrowDomainEventException_WhenCategoryIsNull() {
            // given
            String nullCategory = null;

            // when & then
            thenThrownBy(() -> eventTypeRegistry.findByCategory(nullCategory))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The category cannot be null");
        }
    }

    @Nested
    @DisplayName("findByVersion")
    class FindByVersion {

        @Test
        @DisplayName("should return matching event types for version")
        void findByVersion_ShouldReturnMatchingEventTypes_WhenVersionExists() {
            // given
            eventTypeRegistry.register(VALID_NAME_WITH_VERSION);
            eventTypeRegistry.register("order.OrderCancelled.v1");

            // when
            List<EventType> results = eventTypeRegistry.findByVersion("v1");

            // then
            then(results).hasSize(2);
            then(results).extracting(EventType::getName)
                    .containsExactly("order.OrderCancelled.v1", VALID_NAME_WITH_VERSION);
        }

        @Test
        @DisplayName("should return empty list when no matches found")
        void findByVersion_ShouldReturnEmptyList_WhenNoMatchesFound() {
            // given
            eventTypeRegistry.register(VALID_NAME);

            // when
            List<EventType> results = eventTypeRegistry.findByVersion("v99");

            // then
            then(results).isEmpty();
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("should skip names not present in cache")
        void findByVersion_ShouldSkipNamesNotInCache_WhenCacheMiss() {
            // given
            eventTypeRegistry.register(VALID_NAME_WITH_VERSION);

            // Use reflection to remove from cachedTypes but keep in registeredNames
            Field cachedTypesField;
            ConcurrentHashMap<String, EventType> cachedTypes;
            try {
                cachedTypesField = EventTypeRegistry.class.getDeclaredField("cachedTypes");
                cachedTypesField.setAccessible(true);

                cachedTypes = (ConcurrentHashMap<String, EventType>) cachedTypesField.get(eventTypeRegistry);
                cachedTypes.remove(VALID_NAME_WITH_VERSION);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            // when
            List<EventType> results = eventTypeRegistry.findByVersion("v1");

            // then
            then(results)
                    .as("Should skip names not in cache")
                    .isEmpty();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainEventException when version is null")
        void findByVersion_ShouldThrowDomainEventException_WhenVersionIsNull() {
            // given
            String nullVersion = null;

            // when & then
            thenThrownBy(() -> eventTypeRegistry.findByVersion(nullVersion))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The version cannot be null");
        }
    }

    @Nested
    @DisplayName("isRegistered with String")
    class IsRegisteredWithString {

        @Test
        @DisplayName("should return true when name is registered")
        void isRegistered_ShouldReturnTrue_WhenNameIsRegistered() {
            // given
            eventTypeRegistry.register(VALID_NAME);

            // when
            boolean result = eventTypeRegistry.isRegistered(VALID_NAME);

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("should return false when name is not registered")
        void isRegistered_ShouldReturnFalse_WhenNameIsNotRegistered() {
            // given
            // nothing

            // when
            boolean result = eventTypeRegistry.isRegistered(VALID_NAME);

            // then
            then(result).isFalse();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return false when name is null")
        void isRegistered_ShouldReturnFalse_WhenNameIsNull() {
            // given
            String nullName = null;

            // when
            boolean result = eventTypeRegistry.isRegistered(nullName);

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isRegistered with EventType")
    class IsRegisteredWithEventType {

        @Test
        @DisplayName("should return true when eventType is registered")
        void isRegistered_ShouldReturnTrue_WhenEventTypeIsRegistered() {
            // given
            EventType eventType = EventType.named(VALID_NAME);
            eventTypeRegistry.register(eventType);

            // when
            boolean result = eventTypeRegistry.isRegistered(eventType);

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("should return false when eventType is not registered")
        void isRegistered_ShouldReturnFalse_WhenEventTypeIsNotRegistered() {
            // given
            EventType eventType = EventType.named(VALID_NAME);

            // when
            boolean result = eventTypeRegistry.isRegistered(eventType);

            // then
            then(result).isFalse();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should return false when eventType is null")
        void isRegistered_ShouldReturnFalse_WhenEventTypeIsNull() {
            // given
            EventType nullEventType = null;

            // when
            boolean result = eventTypeRegistry.isRegistered(nullEventType);

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("setStrictMode and isStrictMode")
    class StrictMode {

        @Test
        @DisplayName("should set strict mode to true")
        void setStrictMode_ShouldSetStrictModeToTrue_WhenTrueProvided() {
            // given
            // nothing

            // when
            eventTypeRegistry.setStrictMode(true);

            // then
            then(eventTypeRegistry.isStrictMode()).isTrue();
        }

        @Test
        @DisplayName("should set strict mode to false")
        void setStrictMode_ShouldSetStrictModeToFalse_WhenFalseProvided() {
            // given
            eventTypeRegistry.setStrictMode(true);

            // when
            eventTypeRegistry.setStrictMode(false);

            // then
            then(eventTypeRegistry.isStrictMode()).isFalse();
        }

        @Test
        @DisplayName("should return false by default")
        void isStrictMode_ShouldReturnFalse_WhenDefault() {
            // given
            // nothing

            // when
            boolean result = eventTypeRegistry.isStrictMode();

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("registeredNames")
    class RegisteredNames {

        @Test
        @DisplayName("should return empty set when no names registered")
        void registeredNames_ShouldReturnEmptySet_WhenNoNamesRegistered() {
            // given
            // nothing

            // when
            Set<String> names = eventTypeRegistry.registeredNames();

            // then
            then(names).isEmpty();
        }

        @Test
        @DisplayName("should return all registered names")
        void registeredNames_ShouldReturnAllRegisteredNames_WhenNamesRegistered() {
            // given
            eventTypeRegistry.register(VALID_NAME);
            eventTypeRegistry.register(VALID_NAME_2);

            // when
            Set<String> names = eventTypeRegistry.registeredNames();

            // then
            then(names).hasSize(2);
            then(names).containsExactlyInAnyOrder(VALID_NAME, VALID_NAME_2);
        }

        @Test
        @DisplayName("should return unmodifiable set")
        void registeredNames_ShouldReturnUnmodifiableSet_WhenCalled() {
            // given
            eventTypeRegistry.register(VALID_NAME);
            Set<String> names = eventTypeRegistry.registeredNames();

            // when & then
            thenThrownBy(() -> names.add("NewName"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("should clear all registered names")
        void clear_ShouldClearAllRegisteredNames_WhenCalled() {
            // given
            eventTypeRegistry.register(VALID_NAME);
            eventTypeRegistry.register(VALID_NAME_2);

            // when
            eventTypeRegistry.clear();

            // then
            then(eventTypeRegistry.registeredNames()).isEmpty();
        }

        @Test
        @DisplayName("should reset strict mode to false")
        void clear_ShouldResetStrictModeToFalse_WhenCalled() {
            // given
            eventTypeRegistry.setStrictMode(true);

            // when
            eventTypeRegistry.clear();

            // then
            then(eventTypeRegistry.isStrictMode()).isFalse();
        }
    }

    @Nested
    @DisplayName("getMaxRegisteredTypes")
    class GetMaxRegisteredTypes {

        @Test
        @DisplayName("should return default max registered types")
        void getMaxRegisteredTypes_ShouldReturnDefaultMaxRegisteredTypes_WhenDefaultConstructorUsed() {
            // given
            // nothing

            // when
            int maxTypes = eventTypeRegistry.getMaxRegisteredTypes();

            // then
            then(maxTypes).isEqualTo(EventTypeRegistry.DEFAULT_MAX_REGISTERED_TYPES);
        }

        @Test
        @DisplayName("should return custom max registered types")
        void getMaxRegisteredTypes_ShouldReturnCustomMaxRegisteredTypes_WhenCustomConstructorUsed() {
            // given
            EventTypeRegistry customRegistry = new EventTypeRegistry(100);

            // when
            int maxTypes = customRegistry.getMaxRegisteredTypes();

            // then
            then(maxTypes).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("setMaxRegisteredTypes")
    class SetMaxRegisteredTypes {

        @Test
        @DisplayName("should set max registered types")
        void setMaxRegisteredTypes_ShouldSetMaxRegisteredTypes_WhenValidValueProvided() {
            // given
            // nothing

            // when
            eventTypeRegistry.setMaxRegisteredTypes(200);

            // then
            then(eventTypeRegistry.getMaxRegisteredTypes()).isEqualTo(200);
        }

        @Test
        @DisplayName("should throw DomainEventException when maxRegisteredTypes is zero")
        void setMaxRegisteredTypes_ShouldThrowDomainEventException_WhenMaxRegisteredTypesIsZero() {
            // given
            int maxTypes = 0;

            // when & then
            thenThrownBy(() -> eventTypeRegistry.setMaxRegisteredTypes(maxTypes))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_MAX_REGISTERED_TYPES);
        }

        @Test
        @DisplayName("should throw DomainEventException when maxRegisteredTypes is negative")
        void setMaxRegisteredTypes_ShouldThrowDomainEventException_WhenMaxRegisteredTypesIsNegative() {
            // given
            int maxTypes = -1;

            // when & then
            thenThrownBy(() -> eventTypeRegistry.setMaxRegisteredTypes(maxTypes))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_MAX_REGISTERED_TYPES);
        }

        @Test
        @DisplayName("should allow raising limit")
        void setMaxRegisteredTypes_ShouldAllowRaisingLimit_WhenLimitRaised() {
            // given
            EventTypeRegistry smallRegistry = new EventTypeRegistry(2);
            smallRegistry.register("Event1");
            smallRegistry.register("Event2");

            // when
            smallRegistry.setMaxRegisteredTypes(3);
            smallRegistry.register("Event3");

            // then
            then(smallRegistry.isRegistered("Event3")).isTrue();
        }

        @Test
        @DisplayName("should not evict existing entries when lowering limit")
        void setMaxRegisteredTypes_ShouldNotEvictExistingEntries_WhenLimitLowered() {
            // given
            eventTypeRegistry.register(VALID_NAME);
            eventTypeRegistry.register(VALID_NAME_2);

            // when
            eventTypeRegistry.setMaxRegisteredTypes(1);

            // then
            then(eventTypeRegistry.isRegistered(VALID_NAME)).isTrue();
            then(eventTypeRegistry.isRegistered(VALID_NAME_2)).isTrue();
        }
    }
}
