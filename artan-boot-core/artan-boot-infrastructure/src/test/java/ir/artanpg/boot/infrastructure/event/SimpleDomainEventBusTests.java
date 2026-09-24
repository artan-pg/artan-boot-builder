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

package ir.artanpg.boot.infrastructure.event;

import ir.artanpg.boot.application.port.driven.event.DomainEventListener;
import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import ir.artanpg.boot.application.port.driven.event.DomainEventSubscription;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventTopic;
import ir.artanpg.boot.infrastructure.event.support.TestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link SimpleDomainEventBus}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
class SimpleDomainEventBusTests {

    @Mock
    private DomainEventPublisher typeBasedPublisher;

    private SimpleDomainEventBus bus;

    private TestDomainEvent event;

    private EventTopic topic;

    @BeforeEach
    void setUp() {
        bus = new SimpleDomainEventBus(typeBasedPublisher);
        event = new TestDomainEvent(new TestIdentifier("agg-1"), "payload");
        topic = EventTopic.of(event.getEventType());
    }

    @Test
    void publish_ShouldDispatchToTopicSubscribersAndTypeBasedPublisher_WhenEventIsPublished() {
        // given
        List<DomainEvent> received = new ArrayList<>();
        bus.subscribe(topic, received::add);

        // when
        bus.publish(event);

        // then
        then(received).containsExactly(event);
        verify(typeBasedPublisher).publish(event);
    }

    @Test
    void publish_ShouldUseExplicitTopic_WhenTopicIsProvided() {
        // given
        EventTopic customTopic = EventTopic.of("custom.topic");
        List<DomainEvent> received = new ArrayList<>();
        bus.subscribe(customTopic, received::add);

        // when
        bus.publish(customTopic, event);

        // then
        then(received).containsExactly(event);
        verify(typeBasedPublisher).publish(event);
    }

    @Test
    void publish_ShouldNotInvokeOtherTopicListeners_WhenTopicDoesNotMatch() {
        // given
        List<DomainEvent> received = new ArrayList<>();
        bus.subscribe(EventTopic.of("other.topic"), received::add);

        // when
        bus.publish(event);

        // then
        then(received).isEmpty();
        verify(typeBasedPublisher).publish(event);
    }

    @Test
    void subscribe_ShouldRespectOrder_WhenMultipleListenersAreRegistered() {
        // given
        List<String> order = new ArrayList<>();
        bus.subscribe(topic, e -> order.add("second"), 20);
        bus.subscribe(topic, e -> order.add("first"), 10);

        // when
        bus.publish(event);

        // then
        then(order).containsExactly("first", "second");
    }

    @Test
    void unsubscribe_ShouldStopReceivingEvents_WhenSubscriptionIsCancelled() {
        // given
        AtomicInteger count = new AtomicInteger();
        DomainEventSubscription subscription = bus.subscribe(topic, e -> count.incrementAndGet());

        // when
        bus.publish(event);
        subscription.unsubscribe();
        bus.publish(event);

        // then
        then(count.get()).isEqualTo(1);
        then(subscription.isActive()).isFalse();
        then(subscription.getTopic()).isEqualTo(topic);
    }

    @Test
    void publish_ShouldThrowNullPointerException_WhenEventIsNull() {
        // given / when / then
        thenThrownBy(() -> bus.publish((DomainEvent) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_ShouldThrowNullPointerException_WhenPublisherIsNull() {
        // given / when / then
        thenThrownBy(() -> new SimpleDomainEventBus(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("typeBasedPublisher must not be null");
    }
}
