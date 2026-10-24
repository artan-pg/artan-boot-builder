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
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.event.DomainEventException;
import ir.artanpg.boot.domain.model.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link DefaultDomainEventDescriptor}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("DefaultDomainEventDescriptor")
class DefaultDomainEventDescriptorTests {

    private static final EventType EVENT_TYPE = EventType.of("OrderCreated", "sales", "1");
    private static final EventType OTHER_EVENT_TYPE = EventType.of("OrderCancelled", "sales", "1");

    @Nested
    @DisplayName("of(EventType, Class, Class, Class)")
    class Of {

        @Test
        @DisplayName("Creates descriptor when all arguments are valid")
        void of_ShouldCreateDescriptor_WhenAllArgumentsAreValid() {
            // given

            // when
            DefaultDomainEventDescriptor descriptor =
                    DefaultDomainEventDescriptor.of(EVENT_TYPE, domainEventClass(), String.class, identifierClass());

            // then
            then(descriptor)
                    .as("descriptor must be created")
                    .isNotNull()
                    .isInstanceOf(DomainEventDescriptor.class);
            then(descriptor.eventType())
                    .as("eventType must match")
                    .isEqualTo(EVENT_TYPE);
            then(descriptor.domainEvent())
                    .as("domainEvent must match")
                    .isEqualTo(domainEventClass());
            then(descriptor.payloadType())
                    .as("payloadType must match")
                    .isEqualTo(String.class);
            then(descriptor.identifierType())
                    .as("identifierType must match")
                    .isEqualTo(identifierClass());
        }

        @Test
        @DisplayName("Throws when eventType is null")
        @SuppressWarnings("DataFlowIssue")
        void of_ShouldThrowDomainEventException_WhenEventTypeIsNull() {
            // given

            // when & then
            thenThrownBy(() -> DefaultDomainEventDescriptor.of(
                    null, domainEventClass(), String.class, identifierClass()))
                    .as("null eventType must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The eventType cannot be null");
        }

        @Test
        @DisplayName("Throws when domainEvent is null")
        @SuppressWarnings("DataFlowIssue")
        void of_ShouldThrowDomainEventException_WhenDomainEventIsNull() {
            // given

            // when & then
            thenThrownBy(() -> DefaultDomainEventDescriptor.of(
                    EVENT_TYPE, null, String.class, identifierClass()))
                    .as("null domainEvent must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The domainEvent class cannot be null");
        }

        @Test
        @DisplayName("Throws when payloadType is null")
        @SuppressWarnings("DataFlowIssue")
        void of_ShouldThrowDomainEventException_WhenPayloadTypeIsNull() {
            // given

            // when & then
            thenThrownBy(() -> DefaultDomainEventDescriptor.of(
                    EVENT_TYPE, domainEventClass(), null, identifierClass()))
                    .as("null payloadType must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The payloadType cannot be null");
        }

        @Test
        @DisplayName("Throws when identifierType is null")
        @SuppressWarnings("DataFlowIssue")
        void of_ShouldThrowDomainEventException_WhenIdentifierTypeIsNull() {
            // given

            // when & then
            thenThrownBy(() -> DefaultDomainEventDescriptor.of(
                    EVENT_TYPE, domainEventClass(), String.class, null))
                    .as("null identifierType must be rejected")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The identifierType cannot be null");
        }
    }

    @Nested
    @DisplayName("eventType()")
    class EventTypeAccessor {

        @Test
        @DisplayName("Returns the event type")
        void eventType_ShouldReturnEventType_WhenInvoked() {
            // given
            DefaultDomainEventDescriptor descriptor = descriptor();

            // when
            EventType result = descriptor.eventType();

            // then
            then(result)
                    .as("eventType must be the one provided at construction")
                    .isEqualTo(EVENT_TYPE);
        }
    }

    @Nested
    @DisplayName("domainEvent()")
    class DomainEventAccessor {

        @Test
        @DisplayName("Returns the domain event class")
        void domainEvent_ShouldReturnDomainEventClass_WhenInvoked() {
            // given
            DefaultDomainEventDescriptor descriptor = descriptor();

            // when
            Class<? extends DomainEvent<?, ?>> result = descriptor.domainEvent();

            // then
            then(result)
                    .as("domainEvent must be the one provided at construction")
                    .isEqualTo(domainEventClass());
        }
    }

    @Nested
    @DisplayName("payloadType()")
    class PayloadTypeAccessor {

        @Test
        @DisplayName("Returns the payload type")
        void payloadType_ShouldReturnPayloadType_WhenInvoked() {
            // given
            DefaultDomainEventDescriptor descriptor = descriptor();

            // when
            Class<?> result = descriptor.payloadType();

            // then
            then(result)
                    .as("payloadType must be the one provided at construction")
                    .isEqualTo(String.class);
        }
    }

    @Nested
    @DisplayName("identifierType()")
    class IdentifierTypeAccessor {

        @Test
        @DisplayName("Returns the identifier type")
        void identifierType_ShouldReturnIdentifierType_WhenInvoked() {
            // given
            DefaultDomainEventDescriptor descriptor = descriptor();

            // when
            Class<? extends Identifier<?>> result = descriptor.identifierType();

            // then
            then(result)
                    .as("identifierType must be the one provided at construction")
                    .isEqualTo(identifierClass());
        }
    }

    @Nested
    @DisplayName("equals(Object)")
    class Equals {

        @SuppressWarnings("EqualsWithItself")
        @Test
        @DisplayName("Returns true when compared to itself")
        void equals_ShouldReturnTrue_WhenSameInstance() {
            // given
            DefaultDomainEventDescriptor descriptor = descriptor();

            // when
            boolean result = descriptor.equals(descriptor);

            // then
            then(result)
                    .as("instance must equal itself")
                    .isTrue();
        }

        @Test
        @DisplayName("Returns true when all fields match")
        void equals_ShouldReturnTrue_WhenAllFieldsMatch() {
            // given
            DefaultDomainEventDescriptor first = descriptor();
            DefaultDomainEventDescriptor second = descriptor();

            // when
            boolean result = first.equals(second);

            // then
            then(result)
                    .as("descriptors with identical fields must be equal")
                    .isTrue();
        }

        @Test
        @DisplayName("Returns false when compared to null")
        void equals_ShouldReturnFalse_WhenOtherIsNull() {
            // given
            DefaultDomainEventDescriptor descriptor = descriptor();

            // when
            boolean result = descriptor.equals(null);

            // then
            then(result)
                    .as("descriptor must not equal null")
                    .isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("Returns false when compared to a different type")
        void equals_ShouldReturnFalse_WhenOtherIsDifferentType() {
            // given
            DefaultDomainEventDescriptor descriptor = descriptor();

            // when
            boolean result = descriptor.equals("not-a-descriptor");

            // then
            then(result)
                    .as("descriptor must not equal a different type")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false when eventType differs")
        void equals_ShouldReturnFalse_WhenEventTypeDiffers() {
            // given
            DefaultDomainEventDescriptor first = descriptor();
            DefaultDomainEventDescriptor second = DefaultDomainEventDescriptor.of(
                    OTHER_EVENT_TYPE, domainEventClass(), String.class, identifierClass());

            // when
            boolean result = first.equals(second);

            // then
            then(result)
                    .as("descriptors with different eventType must not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false when domainEvent differs")
        void equals_ShouldReturnFalse_WhenDomainEventDiffers() {
            // given
            DefaultDomainEventDescriptor first = descriptor();
            DefaultDomainEventDescriptor second = DefaultDomainEventDescriptor.of(
                    EVENT_TYPE, otherDomainEventClass(), String.class, identifierClass());

            // when
            boolean result = first.equals(second);

            // then
            then(result)
                    .as("descriptors with different domainEvent must not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false when payloadType differs")
        void equals_ShouldReturnFalse_WhenPayloadTypeDiffers() {
            // given
            DefaultDomainEventDescriptor first = descriptor();
            DefaultDomainEventDescriptor second = DefaultDomainEventDescriptor.of(
                    EVENT_TYPE, domainEventClass(), Integer.class, identifierClass());

            // when
            boolean result = first.equals(second);

            // then
            then(result)
                    .as("descriptors with different payloadType must not be equal")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false when identifierType differs")
        void equals_ShouldReturnFalse_WhenIdentifierTypeDiffers() {
            // given
            DefaultDomainEventDescriptor first = descriptor();
            DefaultDomainEventDescriptor second = DefaultDomainEventDescriptor.of(
                    EVENT_TYPE, domainEventClass(), String.class, otherIdentifierClass());

            // when
            boolean result = first.equals(second);

            // then
            then(result)
                    .as("descriptors with different identifierType must not be equal")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("hashCode()")
    class HashCode {

        @Test
        @DisplayName("Returns the same value for equal descriptors")
        void hashCode_ShouldReturnSameValue_WhenDescriptorsAreEqual() {
            // given
            DefaultDomainEventDescriptor first = descriptor();
            DefaultDomainEventDescriptor second = descriptor();

            // when
            int firstHash = first.hashCode();
            int secondHash = second.hashCode();

            // then
            then(firstHash)
                    .as("equal descriptors must have the same hashCode")
                    .isEqualTo(secondHash);
        }

        @Test
        @DisplayName("Returns different values when fields differ")
        void hashCode_ShouldReturnDifferentValue_WhenFieldsDiffer() {
            // given
            DefaultDomainEventDescriptor first = descriptor();
            DefaultDomainEventDescriptor second = DefaultDomainEventDescriptor.of(
                    OTHER_EVENT_TYPE, domainEventClass(), String.class, identifierClass());

            // when
            int firstHash = first.hashCode();
            int secondHash = second.hashCode();

            // then
            then(firstHash)
                    .as("descriptors with different fields should have different hashCodes")
                    .isNotEqualTo(secondHash);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("Contains all descriptor field names and values")
        void toString_ShouldContainAllFields_WhenInvoked() {
            // given
            DefaultDomainEventDescriptor descriptor = descriptor();

            // when
            String text = descriptor.toString();

            // then
            then(text)
                    .as("toString must include class name and field values")
                    .contains("DefaultDomainEventDescriptor")
                    .contains("eventType=")
                    .contains(EVENT_TYPE.toString())
                    .contains("domainEvent=")
                    .contains(domainEventClass().getName())
                    .contains("payloadType=")
                    .contains(String.class.getName())
                    .contains("identifierType=")
                    .contains(identifierClass().getName());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers and test doubles
    // -------------------------------------------------------------------------

    private static DefaultDomainEventDescriptor descriptor() {
        return DefaultDomainEventDescriptor.of(EVENT_TYPE, domainEventClass(), String.class, identifierClass());
    }

    @SuppressWarnings({"unchecked", "RedundantCast"})
    private static Class<? extends DomainEvent<?, ?>> domainEventClass() {
        return (Class<? extends DomainEvent<?, ?>>) (Class<?>) DomainEvent.class;
    }

    @SuppressWarnings({"unchecked", "RedundantCast"})
    private static Class<? extends DomainEvent<?, ?>> otherDomainEventClass() {
        return (Class<? extends DomainEvent<?, ?>>) (Class<?>) TestDomainEvent.class;
    }

    @SuppressWarnings({"unchecked", "RedundantCast"})
    private static Class<? extends Identifier<?>> identifierClass() {
        return (Class<? extends Identifier<?>>) (Class<?>) Identifier.class;
    }

    @SuppressWarnings({"unchecked", "RedundantCast"})
    private static Class<? extends Identifier<?>> otherIdentifierClass() {
        return (Class<? extends Identifier<?>>) (Class<?>) TestIdentifier.class;
    }

    private abstract static class TestDomainEvent implements DomainEvent<Identifier<?>, String> {
    }

    private interface TestIdentifier extends Identifier<String> {
    }
}
