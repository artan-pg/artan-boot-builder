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

import ir.artanpg.boot.application.port.driven.event.DomainEventDescriptor;
import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.event.DomainEventException;
import ir.artanpg.boot.domain.model.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultDomainEventRegistry}.
 *
 * @author Mohammad Yazdian
 */
class DefaultDomainEventRegistryTests {

    private DefaultDomainEventRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultDomainEventRegistry();
    }

    @Nested
    @DisplayName("No-arg constructor")
    class NoArgConstructor {

        @Test
        @DisplayName("Should create registry with empty state")
        void constructor_ShouldCreateRegistryWithEmptyState_WhenInvokedWithNoArgs() {
            // given

            // when
            DefaultDomainEventRegistry freshRegistry = new DefaultDomainEventRegistry();

            // then
            then(freshRegistry.registeredTypes())
                    .as("A freshly created registry must contain no event types")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Constructor with initial capacity")
    class InitialCapacityConstructor {

        @Test
        @DisplayName("Should create registry when initial capacity is positive")
        void constructor_ShouldCreateRegistry_WhenInitialCapacityIsPositive() {
            // given
            int initialCapacity = 128;

            // when
            DefaultDomainEventRegistry freshRegistry = new DefaultDomainEventRegistry(initialCapacity);

            // then
            then(freshRegistry.registeredTypes())
                    .as("Registry must be created with the given positive capacity")
                    .isEmpty();
        }

        @Test
        @DisplayName("Should create registry when initial capacity is zero")
        void constructor_ShouldCreateRegistry_WhenInitialCapacityIsZero() {
            // given
            int initialCapacity = 0;

            // when
            DefaultDomainEventRegistry freshRegistry = new DefaultDomainEventRegistry(initialCapacity);

            // then
            then(freshRegistry.registeredTypes())
                    .as("Registry must be created when initial capacity is zero")
                    .isEmpty();
        }

        @Test
        @DisplayName("Should throw DomainEventException when initial capacity is negative")
        void constructor_ShouldThrowDomainEventException_WhenInitialCapacityIsNegative() {
            // given
            int initialCapacity = -1;

            // when & then
            thenThrownBy(() -> new DefaultDomainEventRegistry(initialCapacity))
                    .as("A negative initial capacity must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The initialCapacity cannot be negative");
        }
    }

    @Nested
    @DisplayName("register(DomainEventDescriptor)")
    class RegisterDescriptor {

        @Test
        @DisplayName("Should register descriptor successfully")
        void register_ShouldRegisterDescriptor_WhenDescriptorIsValid() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            DomainEventDescriptor descriptor = DefaultDomainEventDescriptor.of(
                    eventType, ValidEvent.class, String.class, TestId.class);

            // when
            registry.register(descriptor);

            // then
            then(registry.findDescriptor(eventType))
                    .as("Descriptor must be retrievable after registration")
                    .isPresent()
                    .contains(descriptor);
        }

        @Test
        @DisplayName("Should throw DomainEventException when descriptor is null")
        @SuppressWarnings("DataFlowIssue")
        void register_ShouldThrowDomainEventException_WhenDescriptorIsNull() {
            // given

            // when & then
            thenThrownBy(() -> registry.register(null))
                    .as("Null descriptor must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The descriptor cannot be null");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Should throw DomainEventException when descriptor carries null event type")
        void register_ShouldThrowDomainEventException_WhenDescriptorHasNullEventType() {
            // given
            DomainEventDescriptor descriptor = mock(DomainEventDescriptor.class);
            when(descriptor.eventType()).thenReturn(null);

            // when & then
            thenThrownBy(() -> registry.register(descriptor))
                    .as("Descriptor with null event type must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventType cannot be null");
        }

        @Test
        @DisplayName("Should throw DomainEventException when descriptor carries null event class")
        void register_ShouldThrowDomainEventException_WhenDescriptorHasNullEventClass() {
            // given
            DomainEventDescriptor descriptor = mock(DomainEventDescriptor.class);
            when(descriptor.eventType()).thenReturn(EventType.of("SomeEvent"));

            // when & then
            thenThrownBy(() -> registry.register(descriptor))
                    .as("Descriptor with null domain event class must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventClass cannot be null");
        }

        @Test
        @DisplayName("Should throw DomainEventException when descriptor carries an abstract class")
        void register_ShouldThrowDomainEventException_WhenDescriptorCarriesAbstractClass() {
            // given
            DomainEventDescriptor descriptor = mock(DomainEventDescriptor.class);
            when(descriptor.eventType()).thenReturn(EventType.of("AbstractEvent"));
            when(descriptor.domainEvent()).thenAnswer(_ -> AbstractEvent.class);

            // when & then
            thenThrownBy(() -> registry.register(descriptor))
                    .as("Abstract domain event classes must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageStartingWith("Cannot register an abstract class or interface");
        }

        @Test
        @DisplayName("Should throw DomainEventException when descriptor carries an interface")
        void register_ShouldThrowDomainEventException_WhenDescriptorCarriesInterface() {
            // given
            DomainEventDescriptor descriptor = mock(DomainEventDescriptor.class);
            when(descriptor.eventType()).thenReturn(EventType.of("InterfaceEvent"));
            when(descriptor.domainEvent()).thenAnswer(_ -> InterfaceEvent.class);

            // when & then
            thenThrownBy(() -> registry.register(descriptor))
                    .as("Interface domain event types must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageStartingWith("Cannot register an abstract class or interface");
        }

        @Test
        @DisplayName("Should be idempotent when registering same descriptor twice")
        void register_ShouldBeIdempotent_WhenSameDescriptorRegisteredTwice() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            DomainEventDescriptor descriptor = DefaultDomainEventDescriptor.of(
                    eventType, ValidEvent.class, String.class, TestId.class);

            // when
            registry.register(descriptor);
            registry.register(descriptor);

            // then
            then(registry.findDescriptor(eventType))
                    .as("Re-registering the same descriptor must be a no-op")
                    .isPresent()
                    .contains(descriptor);
        }

        @Test
        @DisplayName("Should be idempotent when registering same type with same class via new descriptor")
        void register_ShouldBeIdempotent_WhenSameTypeAndSameClassRegisteredViaNewDescriptor() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            DomainEventDescriptor first = DefaultDomainEventDescriptor.of(
                    eventType, ValidEvent.class, String.class, TestId.class);
            DomainEventDescriptor second = DefaultDomainEventDescriptor.of(
                    eventType, ValidEvent.class, String.class, TestId.class);

            // when
            registry.register(first);
            registry.register(second);

            // then
            then(registry.findDescriptor(eventType))
                    .as("Re-registering with the same class must be idempotent")
                    .isPresent();
        }

        @Test
        @DisplayName("Should throw DomainEventException when registering same type with different class")
        void register_ShouldThrowDomainEventException_WhenSameTypeRegisteredWithDifferentClass() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            DomainEventDescriptor first = DefaultDomainEventDescriptor.of(
                    eventType, ValidEvent.class, String.class, TestId.class);
            DomainEventDescriptor second = DefaultDomainEventDescriptor.of(
                    eventType, AnotherEvent.class, String.class, TestId.class);

            registry.register(first);

            // when & then
            thenThrownBy(() -> registry.register(second))
                    .as("Registering the same event type with a different class must fail")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("already registered to class");
        }
    }

    @Nested
    @DisplayName("register(EventType, Class)")
    class RegisterWithType {

        @Test
        @DisplayName("Should register successfully when class extends AbstractDomainEvent")
        void register_ShouldRegisterSuccessfully_WhenClassExtendsAbstractDomainEvent() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");

            // when
            registry.register(eventType, ValidEvent.class);

            // then
            then(registry.findDescriptor(eventType))
                    .as("Descriptor must be built and stored")
                    .isPresent()
                    .hasValueSatisfying(descriptor -> {
                        then(descriptor.eventType())
                                .as("Stored descriptor must carry the event type")
                                .isEqualTo(eventType);
                        then(descriptor.domainEvent())
                                .as("Stored descriptor must carry the event class")
                                .isEqualTo(ValidEvent.class);
                        then(descriptor.payloadType())
                                .as("Stored descriptor must carry the extracted payload type")
                                .isEqualTo(String.class);
                        then(descriptor.identifierType())
                                .as("Stored descriptor must carry the extracted identifier type")
                                .isEqualTo(TestId.class);
                    });
        }

        @Test
        @DisplayName("Should throw DomainEventException when event type is null")
        @SuppressWarnings("DataFlowIssue")
        void register_ShouldThrowDomainEventException_WhenEventTypeIsNull() {
            // given

            // when & then
            thenThrownBy(() -> registry.register(null, ValidEvent.class))
                    .as("Null event type must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventType cannot be null");
        }

        @Test
        @DisplayName("Should throw DomainEventException when event class is null")
        @SuppressWarnings("DataFlowIssue")
        void register_ShouldThrowDomainEventException_WhenEventClassIsNull() {
            // given
            EventType eventType = EventType.of("SomeEvent");

            // when & then
            thenThrownBy(() -> registry.register(eventType, null))
                    .as("Null event class must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventClass cannot be null");
        }

        @Test
        @DisplayName("Should throw DomainEventException when class is abstract")
        void register_ShouldThrowDomainEventException_WhenClassIsAbstract() {
            // given
            EventType eventType = EventType.of("AbstractEvent");

            // when & then
            thenThrownBy(() -> registry.register(eventType, AbstractEvent.class))
                    .as("Abstract event classes must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageStartingWith("Cannot register an abstract class or interface");
        }

        @Test
        @DisplayName("Should throw DomainEventException when class is an interface")
        void register_ShouldThrowDomainEventException_WhenClassIsInterface() {
            // given
            EventType eventType = EventType.of("InterfaceEvent");

            // when & then
            thenThrownBy(() -> registry.register(eventType, InterfaceEvent.class))
                    .as("Event interfaces must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageStartingWith("Cannot register an abstract class or interface");
        }

        @Test
        @DisplayName("Should throw DomainEventException when class does not extend AbstractDomainEvent")
        void register_ShouldThrowDomainEventException_WhenClassDoesNotExtendAbstractDomainEvent() {
            // given
            EventType eventType = EventType.of("OrphanEvent");

            // when & then
            thenThrownBy(() -> registry.register(eventType, NonExtendingEvent.class))
                    .as("A class that does not extend AbstractDomainEvent must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("Cannot resolve AbstractDomainEvent generic parameters");
        }

        @Test
        @DisplayName("Should be idempotent when registering same type and same class twice")
        void register_ShouldBeIdempotent_WhenSameTypeAndSameClassRegisteredTwice() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");

            // when
            registry.register(eventType, ValidEvent.class);
            registry.register(eventType, ValidEvent.class);

            // then
            then(registry.registeredTypes())
                    .as("Re-registering the same class must not duplicate the entry")
                    .containsExactly(eventType);
        }

        @Test
        @DisplayName("Should throw DomainEventException when registering same type with different class")
        void register_ShouldThrowDomainEventException_WhenSameTypeRegisteredWithDifferentClass() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            registry.register(eventType, ValidEvent.class);

            // when & then
            thenThrownBy(() -> registry.register(eventType, AnotherEvent.class))
                    .as("Registering the same event type with a different class must fail")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("already registered to class");
        }

        @SuppressWarnings({"unchecked", "RedundantCast", "rawtypes"})
        @Test
        @DisplayName("Should throw when class extends raw AbstractDomainEvent")
        void register_ShouldThrowDomainEventException_WhenClassExtendsRawAbstractDomainEvent() {
            // given
            EventType eventType = EventType.of("RawEvent");
            Class<? extends DomainEvent<?, ?>> rawClass =
                    (Class<? extends DomainEvent<?, ?>>) (Class) RawEvent.class;

            // when & then
            thenThrownBy(() -> registry.register(eventType, rawClass))
                    .as("A class that extends AbstractDomainEvent as a raw type must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("Cannot resolve AbstractDomainEvent generic parameters");
        }

        @Test
        @DisplayName("Should register successfully when payload is a parameterized type")
        void register_ShouldRegisterSuccessfully_WhenPayloadIsParameterizedType() {
            // given
            EventType eventType = EventType.of("GenericPayloadEvent");

            // when
            registry.register(eventType, EventWithParameterizedPayload.class);

            // then
            then(registry.findDescriptor(eventType))
                    .as("Descriptor must be built for parameterized payload")
                    .isPresent()
                    .hasValueSatisfying(descriptor -> then(descriptor.payloadType())
                            .as("Payload type must be resolved to the raw class of the parameterized type")
                            .isEqualTo(java.util.List.class));
        }

        @Test
        @DisplayName("Should throw when class extends a parameterized class that is not AbstractDomainEvent")
        void register_ShouldThrowDomainEventException_WhenClassExtendsParameterizedNonAbstractDomainEvent() {
            // given
            EventType eventType = EventType.of("WrongSuperEvent");

            // when & then
            thenThrownBy(() -> registry.register(eventType, EventWithWrongParameterizedSuper.class))
                    .as("A class extending a parameterized class other than AbstractDomainEvent must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("Cannot resolve AbstractDomainEvent generic parameters");
        }

        @Test
        @DisplayName("Should register successfully when class inherits from an intermediate event class")
        void register_ShouldRegisterSuccessfully_WhenClassInheritsFromIntermediateEventClass() {
            // given
            EventType eventType = EventType.of("DerivedEvent", "derived", "1");

            // when
            registry.register(eventType, DerivedEvent.class);

            // then
            then(registry.findDescriptor(eventType))
                    .as("Descriptor must be resolved through the intermediate class hierarchy")
                    .isPresent()
                    .hasValueSatisfying(descriptor -> {
                        then(descriptor.payloadType())
                                .as("Payload type must be resolved from the intermediate class")
                                .isEqualTo(String.class);
                        then(descriptor.identifierType())
                                .as("Identifier type must be resolved from the intermediate class")
                                .isEqualTo(TestId.class);
                    });
        }

        @Test
        @DisplayName("Should throw when payload type is an unresolved type variable")
        @SuppressWarnings({"rawtypes", "unchecked", "RedundantCast"})
        void register_ShouldThrowDomainEventException_WhenPayloadIsUnresolvedTypeVariable() {
            // given
            EventType eventType = EventType.of("TypeVariableEvent");
            Class<? extends DomainEvent<?, ?>> rawClass =
                    (Class<? extends DomainEvent<?, ?>>) (Class) GenericEventWithTypeVariable.class;

            // when & then
            thenThrownBy(() -> registry.register(eventType, rawClass))
                    .as("A class with an unresolved type variable payload must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("Unsupported type argument");
        }
    }

    @Nested
    @DisplayName("findDescriptor(EventType)")
    class FindDescriptor {

        @Test
        @DisplayName("Should return descriptor when event type is registered")
        void findDescriptor_ShouldReturnDescriptor_WhenEventTypeIsRegistered() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            registry.register(eventType, ValidEvent.class);

            // when
            java.util.Optional<DomainEventDescriptor> result = registry.findDescriptor(eventType);

            // then
            then(result)
                    .as("Descriptor must be found for a registered event type")
                    .isPresent();
        }

        @Test
        @DisplayName("Should return empty when event type is not registered")
        void findDescriptor_ShouldReturnEmpty_WhenEventTypeIsNotRegistered() {
            // given
            EventType eventType = EventType.of("UnknownEvent");

            // when
            java.util.Optional<DomainEventDescriptor> result = registry.findDescriptor(eventType);

            // then
            then(result)
                    .as("Unknown event type must yield an empty optional")
                    .isEmpty();
        }

        @Test
        @DisplayName("Should throw DomainEventException when event type is null")
        @SuppressWarnings("DataFlowIssue")
        void findDescriptor_ShouldThrowDomainEventException_WhenEventTypeIsNull() {
            // given

            // when & then
            thenThrownBy(() -> registry.findDescriptor(null))
                    .as("Null event type must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventType cannot be null");
        }
    }

    @Nested
    @DisplayName("findDescriptor(String, String, String) default method")
    class FindDescriptorByCoordinates {

        @Test
        @DisplayName("Should return descriptor when coordinates match a registered type")
        void findDescriptorByCoordinates_ShouldReturnDescriptor_WhenCoordinatesMatch() {
            // given
            registry.register(EventType.of("UserCreated", "users", "1"), ValidEvent.class);

            // when
            java.util.Optional<DomainEventDescriptor> result =
                    registry.findDescriptor("UserCreated", "users", "1");

            // then
            then(result)
                    .as("Coordinates must be resolved to a registered descriptor")
                    .isPresent();
        }

        @Test
        @DisplayName("Should return empty when coordinates do not match any registered type")
        void findDescriptorByCoordinates_ShouldReturnEmpty_WhenCoordinatesDoNotMatch() {
            // given

            // when
            java.util.Optional<DomainEventDescriptor> result =
                    registry.findDescriptor("Unknown", "none", "0");

            // then
            then(result)
                    .as("Unknown coordinates must yield an empty optional")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("findClass(EventType) default method")
    class FindClass {

        @Test
        @DisplayName("Should return class when event type is registered")
        void findClass_ShouldReturnClass_WhenEventTypeIsRegistered() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            registry.register(eventType, ValidEvent.class);

            // when
            java.util.Optional<Class<? extends DomainEvent<?, ?>>> result = registry.findClass(eventType);

            // then
            then(result)
                    .as("Registered event type must expose its class via findClass")
                    .isPresent()
                    .contains(ValidEvent.class);
        }

        @Test
        @DisplayName("Should return empty when event type is not registered")
        void findClass_ShouldReturnEmpty_WhenEventTypeIsNotRegistered() {
            // given
            EventType eventType = EventType.of("UnknownEvent");

            // when
            java.util.Optional<Class<? extends DomainEvent<?, ?>>> result = registry.findClass(eventType);

            // then
            then(result)
                    .as("Unknown event type must yield an empty optional")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("isRegistered(EventType)")
    class IsRegistered {

        @Test
        @DisplayName("Should return true when event type is registered")
        void isRegistered_ShouldReturnTrue_WhenEventTypeIsRegistered() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            registry.register(eventType, ValidEvent.class);

            // when
            boolean result = registry.isRegistered(eventType);

            // then
            then(result)
                    .as("Registered event type must be reported as registered")
                    .isTrue();
        }

        @Test
        @DisplayName("Should return false when event type is not registered")
        void isRegistered_ShouldReturnFalse_WhenEventTypeIsNotRegistered() {
            // given
            EventType eventType = EventType.of("UnknownEvent");

            // when
            boolean result = registry.isRegistered(eventType);

            // then
            then(result)
                    .as("Unknown event type must be reported as not registered")
                    .isFalse();
        }

        @Test
        @DisplayName("Should throw DomainEventException when event type is null")
        @SuppressWarnings("DataFlowIssue")
        void isRegistered_ShouldThrowDomainEventException_WhenEventTypeIsNull() {
            // given

            // when & then
            thenThrownBy(() -> registry.isRegistered(null))
                    .as("Null event type must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventType cannot be null");
        }
    }

    @Nested
    @DisplayName("isRegistered(String, String, String) default method")
    class IsRegisteredByCoordinates {

        @Test
        @DisplayName("Should return true when coordinates match a registered type")
        void isRegisteredByCoordinates_ShouldReturnTrue_WhenCoordinatesMatch() {
            // given
            registry.register(EventType.of("UserCreated", "users", "1"), ValidEvent.class);

            // when
            boolean result = registry.isRegistered("UserCreated", "users", "1");

            // then
            then(result)
                    .as("Coordinates that match a registered type must return true")
                    .isTrue();
        }

        @Test
        @DisplayName("Should return false when coordinates do not match any registered type")
        void isRegisteredByCoordinates_ShouldReturnFalse_WhenCoordinatesDoNotMatch() {
            // given

            // when
            boolean result = registry.isRegistered("Unknown", "none", "0");

            // then
            then(result)
                    .as("Unknown coordinates must return false")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("registeredTypes")
    class RegisteredTypes {

        @Test
        @DisplayName("Should return empty set when no event type is registered")
        void registeredTypes_ShouldReturnEmptySet_WhenNothingIsRegistered() {
            // given

            // when
            Set<EventType> result = registry.registeredTypes();

            // then
            then(result)
                    .as("Empty registry must yield an empty set of event types")
                    .isEmpty();
        }

        @Test
        @DisplayName("Should return all registered event types")
        void registeredTypes_ShouldReturnAllRegisteredTypes_WhenSeveralTypesAreRegistered() {
            // given
            EventType first = EventType.of("First", "group", "1");
            EventType second = EventType.of("Second", "group", "1");
            EventType third = EventType.of("Third");
            registry.register(first, ValidEvent.class);
            registry.register(second, AnotherEvent.class);
            registry.register(third, ThirdEvent.class);

            // when
            Set<EventType> result = registry.registeredTypes();

            // then
            then(result)
                    .as("All registered event types must be returned")
                    .containsExactlyInAnyOrder(first, second, third);
        }

        @Test
        @DisplayName("Should return an immutable snapshot")
        void registeredTypes_ShouldReturnImmutableSnapshot_WhenInvoked() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            registry.register(eventType, ValidEvent.class);

            // when
            Set<EventType> result = registry.registeredTypes();

            // then
            thenThrownBy(() -> result.add(EventType.of("Extra")))
                    .as("The returned set must be immutable")
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("resolve(EventType)")
    class Resolve {

        @Test
        @DisplayName("Should return class when event type is registered")
        void resolve_ShouldReturnClass_WhenEventTypeIsRegistered() {
            // given
            EventType eventType = EventType.of("UserCreated", "users", "1");
            registry.register(eventType, ValidEvent.class);

            // when
            DomainEventDescriptor result = registry.resolve(eventType);

            // then
            then(result.domainEvent())
                    .as("Registered event type must be resolved to its class")
                    .isEqualTo(ValidEvent.class);
        }

        @Test
        @DisplayName("Should throw DomainEventException when event type is not registered")
        void resolve_ShouldThrowDomainEventException_WhenEventTypeIsNotRegistered() {
            // given
            EventType eventType = EventType.of("UnknownEvent");

            // when & then
            thenThrownBy(() -> registry.resolve(eventType))
                    .as("Unregistered event type must be rejected with a descriptive exception")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessageContaining("No domain event class registered for event type");
        }

        @Test
        @DisplayName("Should throw DomainEventException when event type is null")
        @SuppressWarnings("DataFlowIssue")
        void resolve_ShouldThrowDomainEventException_WhenEventTypeIsNull() {
            // given

            // when & then
            thenThrownBy(() -> registry.resolve(null))
                    .as("Null event type must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventType cannot be null");
        }
    }

    @Nested
    @DisplayName("equals")
    class EqualsBehavior {

        @SuppressWarnings("EqualsWithItself")
        @Test
        @DisplayName("Should equal itself")
        void equals_ShouldReturnTrue_WhenComparedToItself() {
            // given

            // when & then
            then(registry.equals(registry))
                    .as("A registry must be equal to itself")
                    .isTrue();
        }

        @Test
        @DisplayName("Should equal another registry with the same registrations")
        void equals_ShouldReturnTrue_WhenComparedToAnotherRegistryWithSameRegistrations() {
            // given
            DefaultDomainEventRegistry other = new DefaultDomainEventRegistry();
            EventType eventType = EventType.of("UserCreated", "users", "1");
            registry.register(eventType, ValidEvent.class);
            other.register(eventType, ValidEvent.class);

            // when
            boolean result = registry.equals(other);

            // then
            then(result)
                    .as("Two registries with identical registrations must be equal")
                    .isTrue();
        }

        @Test
        @DisplayName("Should not equal another registry with different registrations")
        void equals_ShouldReturnFalse_WhenComparedToAnotherRegistryWithDifferentRegistrations() {
            // given
            DefaultDomainEventRegistry other = new DefaultDomainEventRegistry();
            registry.register(EventType.of("First"), ValidEvent.class);
            other.register(EventType.of("Second"), AnotherEvent.class);

            // when
            boolean result = registry.equals(other);

            // then
            then(result)
                    .as("Two registries with different registrations must not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("Should not equal null")
        void equals_ShouldReturnFalse_WhenComparedToNull() {
            // given

            // when
            boolean result = registry.equals(null);

            // then
            then(result)
                    .as("A registry must not be equal to null")
                    .isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("Should not equal an object of a different type")
        void equals_ShouldReturnFalse_WhenComparedToObjectOfDifferentType() {
            // given

            // when
            boolean result = registry.equals("not a registry");

            // then
            then(result)
                    .as("A registry must not be equal to an object of a different type")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("hashCode")
    class HashCodeBehavior {

        @Test
        @DisplayName("Should produce the same hash code for equal registries")
        void hashCode_ShouldProduceSameHash_WhenRegistriesAreEqual() {
            // given
            DefaultDomainEventRegistry other = new DefaultDomainEventRegistry();
            EventType eventType = EventType.of("UserCreated", "users", "1");
            registry.register(eventType, ValidEvent.class);
            other.register(eventType, ValidEvent.class);

            // when
            int firstHash = registry.hashCode();
            int secondHash = other.hashCode();

            // then
            then(firstHash)
                    .as("Equal registries must produce the same hash code")
                    .isEqualTo(secondHash);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringBehavior {

        @Test
        @DisplayName("Should report zero registered types on empty registry")
        void toString_ShouldReportZero_WhenRegistryIsEmpty() {
            // given

            // when
            String result = registry.toString();

            // then
            then(result)
                    .as("Empty registry toString must report zero registered types")
                    .isEqualTo("DefaultDomainEventRegistry[registeredTypes=0]");
        }

        @Test
        @DisplayName("Should report the current count of registered types")
        void toString_ShouldReportCurrentCount_WhenTypesAreRegistered() {
            // given
            registry.register(EventType.of("First"), ValidEvent.class);
            registry.register(EventType.of("Second"), AnotherEvent.class);

            // when
            String result = registry.toString();

            // then
            then(result)
                    .as("toString must reflect the number of registered types")
                    .isEqualTo("DefaultDomainEventRegistry[registeredTypes=2]");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers and test doubles
    // -------------------------------------------------------------------------

    private static class BaseEvent extends AbstractDomainEvent<TestId, String> {

        protected BaseEvent(TestId aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("BaseEvent", "base", "1"));
        }
    }

    private static final class DerivedEvent extends BaseEvent {

        DerivedEvent() {
            super(new TestId("id"), "payload");
        }
    }

    private record TestId(String value) implements Identifier<String> {

        @Serial
        private static final long serialVersionUID = 2924569730900396485L;
    }

    private static final class ValidEvent extends AbstractDomainEvent<TestId, String> {

        ValidEvent(TestId aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("UserCreated", "users", "1"));
        }
    }

    private static final class AnotherEvent extends AbstractDomainEvent<TestId, String> {

        AnotherEvent(TestId aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("OrderCreated", "orders", "1"));
        }
    }

    private static final class ThirdEvent extends AbstractDomainEvent<TestId, String> {

        ThirdEvent(TestId aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("Third"));
        }
    }

    private abstract static class AbstractEvent extends AbstractDomainEvent<TestId, String> {

        AbstractEvent(TestId aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("Abstract"));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class RawEvent extends AbstractDomainEvent {

        RawEvent() {
            super(new TestId("id"), "payload", EventType.of("Raw"));
        }
    }

    private static final class NonExtendingEvent implements DomainEvent<TestId, String> {

        @Override
        public String getEventId() {
            return "test-id";
        }

        @Override
        public EventType getEventType() {
            return EventType.of("OrphanEvent");
        }

        @Override
        public Instant getOccurredAt() {
            return Instant.now();
        }

        @Override
        public TestId getAggregateId() {
            return new TestId("aggregate-id");
        }

        @Override
        public String getPayload() {
            return "payload";
        }
    }

    private interface InterfaceEvent extends DomainEvent<TestId, String> {
    }

    private static final class EventWithParameterizedPayload extends AbstractDomainEvent<TestId, List<String>> {

        EventWithParameterizedPayload() {
            super(new TestId("id"), List.of("a", "b"), EventType.of("GenericPayload"));
        }
    }

    @SuppressWarnings("unused")
    private static ParameterizedType forceReflectionCoverage() {
        return (ParameterizedType) ValidEvent.class.getGenericSuperclass();
    }

    private static final class EventWithWrongParameterizedSuper extends ArrayList<String>
            implements DomainEvent<TestId, String> {

        @Serial
        private static final long serialVersionUID = -5157606024429195960L;

        @Override
        public String getEventId() {
            return "test-id";
        }

        @Override
        public EventType getEventType() {
            return EventType.of("WrongSuperEvent");
        }

        @Override
        public java.time.Instant getOccurredAt() {
            return java.time.Instant.now();
        }

        @Override
        public TestId getAggregateId() {
            return new TestId("aggregate-id");
        }

        @Override
        public String getPayload() {
            return "payload";
        }
    }

    private static final class GenericEventWithTypeVariable<T> extends AbstractDomainEvent<TestId, T> {

        GenericEventWithTypeVariable(T payload) {
            super(new TestId("id"), payload, EventType.of("TypeVariable"));
        }
    }
}
