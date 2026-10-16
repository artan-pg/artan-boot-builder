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

import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.event.DomainEventException;
import ir.artanpg.boot.domain.model.Identifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link SimpleDomainEventPublisher}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleDomainEventPublisher")
class SimpleDomainEventPublisherTests {

    private static final String ERROR_MULTICASTER_NULL = "The DomainEventMulticaster cannot be null";
    private static final String ERROR_EVENT_NULL = "The event cannot be null";

    @Mock
    private DomainEventMulticaster multicaster;

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("Creates publisher when multicaster is valid")
        void constructor_ShouldCreatePublisher_WhenMulticasterIsValid() {
            // given

            // when
            SimpleDomainEventPublisher publisher = new SimpleDomainEventPublisher(multicaster);

            // then
            then(publisher)
                    .as("publisher should be created when multicaster dependency is valid")
                    .isNotNull();
        }

        @Test
        @DisplayName("Throws DomainEventException when multicaster is null")
        @SuppressWarnings("ConstantConditions")
        void constructor_ShouldThrowDomainEventException_WhenMulticasterIsNull() {
            // given
            DomainEventMulticaster nullMulticaster = null;

            // when & then
            thenThrownBy(() -> new SimpleDomainEventPublisher(nullMulticaster))
                    .as("constructor must reject a null DomainEventMulticaster")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_MULTICASTER_NULL);
        }
    }

    @Nested
    @DisplayName("publish")
    class PublishTests {

        @Test
        @DisplayName("Delegates event to multicaster")
        void publish_ShouldDelegateToMulticaster_WhenEventIsValid() {
            // given
            SimpleDomainEventPublisher publisher = new SimpleDomainEventPublisher(multicaster);
            DomainEvent<?, ?> event = new SampleEvent(new SampleIdentifier("agg-1"), "payload");

            // when
            publisher.publish(event);

            // then
            then(event)
                    .as("published event passed to multicaster should remain the same instance")
                    .isSameAs(event);
            BDDMockito.then(multicaster).should().multicastEvent(event);
        }

        @Test
        @DisplayName("Throws DomainEventException when event is null")
        @SuppressWarnings("ConstantConditions")
        void publish_ShouldThrowDomainEventException_WhenEventIsNull() {
            // given
            SimpleDomainEventPublisher publisher = new SimpleDomainEventPublisher(multicaster);

            // when & then
            thenThrownBy(() -> publisher.publish(null))
                    .as("publish must reject a null domain event")
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage(ERROR_EVENT_NULL);

            BDDMockito.then(multicaster).should(never()).multicastEvent(any());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers & test doubles
    // -------------------------------------------------------------------------

    private record SampleIdentifier(String value) implements Identifier<String> {
    }

    private static final class SampleEvent extends AbstractDomainEvent<SampleIdentifier, String> {

        SampleEvent(SampleIdentifier aggregateId, String payload) {
            super(aggregateId, payload, EventType.of("SampleEvent"));
        }
    }
}
