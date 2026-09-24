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

package ir.artanpg.boot.application.port.driven.event;

import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventTopic;
import org.jspecify.annotations.NonNull;

/**
 * Topic-based domain event bus.
 *
 * <p>The bus routes events to listeners that have subscribed to a given
 * {@link EventTopic}. The default topic for an event is derived from
 * {@link ir.artanpg.boot.domain.event.EventType#getName()}.
 *
 * <p>This port complements {@link DomainEventPublisher}: the publisher remains
 * the type-based dispatch mechanism (phase 1), while the bus adds explicit
 * topic routing and programmatic subscribe/unsubscribe (phase 2).
 *
 * @author Mohammad Yazdian
 * @see DomainEventPublisher
 * @see EventTopic
 * @see DomainEventSubscription
 * @since 0.1.0
 */
public interface DomainEventBus {

    /**
     * Publishes the event to its default topic
     * ({@code EventTopic.of(event.getEventType())}).
     *
     * @param event the domain event; must not be {@code null}
     */
    void publish(@NonNull DomainEvent event);

    /**
     * Publishes the event to the given topic.
     *
     * @param topic the target topic; must not be {@code null}
     * @param event the domain event; must not be {@code null}
     */
    void publish(@NonNull EventTopic topic, @NonNull DomainEvent event);

    /**
     * Subscribes a listener to the given topic.
     *
     * @param topic    the topic; must not be {@code null}
     * @param listener the listener; must not be {@code null}
     * @return a subscription that can be used to unsubscribe
     */
    DomainEventSubscription subscribe(@NonNull EventTopic topic, @NonNull DomainEventListener listener);

    /**
     * Subscribes a listener to the given topic with an explicit order.
     *
     * <p>Lower order values are invoked first. If the listener already defines
     * {@link DomainEventListener#getOrder()}, the {@code order} parameter
     * takes precedence for this subscription.
     *
     * @param topic    the topic; must not be {@code null}
     * @param listener the listener; must not be {@code null}
     * @param order    the order for this subscription
     * @return a subscription that can be used to unsubscribe
     */
    DomainEventSubscription subscribe(@NonNull EventTopic topic,
                                      @NonNull DomainEventListener listener,
                                      int order);
}
