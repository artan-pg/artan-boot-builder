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

import ir.artanpg.boot.application.port.driven.event.DomainEventBus;
import ir.artanpg.boot.application.port.driven.event.DomainEventListener;
import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import ir.artanpg.boot.application.port.driven.event.DomainEventSubscription;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventTopic;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-process implementation of {@link DomainEventBus}.
 *
 * <p>Each topic is backed by its own {@link SimpleDomainEventMulticaster}.
 * Publishing an event without an explicit topic uses
 * {@link EventTopic#of(DomainEvent)} (i.e. the event type name).
 *
 * <p>In addition, every publish operation is forwarded to the phase-1
 * {@link DomainEventPublisher} so that type-based listeners continue to work.
 *
 * @author Mohammad Yazdian
 * @see DomainEventBus
 * @see DomainEventPublisher
 * @since 0.1.0
 */
public class SimpleDomainEventBus implements DomainEventBus {

    private static final Logger log = LoggerFactory.getLogger(SimpleDomainEventBus.class);

    private final ConcurrentMap<EventTopic, DomainEventMulticaster> topicMulticasters = new ConcurrentHashMap<>();

    private final DomainEventPublisher typeBasedPublisher;

    /**
     * Creates a bus that also forwards events to the given type-based publisher.
     *
     * @param typeBasedPublisher the phase-1 publisher; must not be {@code null}
     */
    public SimpleDomainEventBus(@NonNull DomainEventPublisher typeBasedPublisher) {
        this.typeBasedPublisher = Objects.requireNonNull(typeBasedPublisher, "typeBasedPublisher must not be null");
    }

    @Override
    public void publish(@NonNull DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        publish(EventTopic.of(event), event);
    }

    @Override
    public void publish(@NonNull EventTopic topic, @NonNull DomainEvent event) {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(event, "event must not be null");

        DomainEventMulticaster multicaster = this.topicMulticasters.get(topic);
        if (multicaster != null) {
            multicaster.multicastEvent(event);
        }
        else {
            log.trace("No subscribers for topic [{}]", topic.getName());
        }

        // Always forward to type-based publisher so phase-1 handlers still receive the event
        this.typeBasedPublisher.publish(event);
    }

    @Override
    public DomainEventSubscription subscribe(@NonNull EventTopic topic, @NonNull DomainEventListener listener) {
        return subscribe(topic, listener, listener.getOrder());
    }

    @Override
    public DomainEventSubscription subscribe(@NonNull EventTopic topic,
                                             @NonNull DomainEventListener listener,
                                             int order) {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        DomainEventListener orderedListener = wrapWithOrder(listener, order);
        DomainEventMulticaster multicaster = this.topicMulticasters.computeIfAbsent(
                topic, t -> new SimpleDomainEventMulticaster());
        multicaster.addDomainEventListener(orderedListener);

        log.debug("Subscribed listener [{}] to topic [{}] with order [{}]",
                listener.getClass().getSimpleName(), topic.getName(), order);

        return new SimpleDomainEventSubscription(topic, orderedListener, multicaster);
    }

    private static DomainEventListener wrapWithOrder(DomainEventListener listener, int order) {
        if (listener.getOrder() == order) {
            return listener;
        }
        return new DomainEventListener() {
            @Override
            public void process(DomainEvent event) {
                listener.process(event);
            }

            @Override
            public int getOrder() {
                return order;
            }

            @Override
            public boolean supportsAsyncExecution() {
                return listener.supportsAsyncExecution();
            }

            @Override
            public ir.artanpg.boot.application.port.driven.event.ListenerExceptionHandler exceptionHandler() {
                return listener.exceptionHandler();
            }
        };
    }

    /**
     * Simple subscription that removes the listener from the topic multicaster.
     */
    private static final class SimpleDomainEventSubscription implements DomainEventSubscription {

        private final EventTopic topic;
        private final DomainEventListener listener;
        private final DomainEventMulticaster multicaster;
        private final AtomicBoolean active = new AtomicBoolean(true);

        private SimpleDomainEventSubscription(EventTopic topic,
                                              DomainEventListener listener,
                                              DomainEventMulticaster multicaster) {
            this.topic = topic;
            this.listener = listener;
            this.multicaster = multicaster;
        }

        @Override
        public void unsubscribe() {
            if (this.active.compareAndSet(true, false)) {
                this.multicaster.removeDomainEventListener(this.listener);
                log.debug("Unsubscribed listener from topic [{}]", this.topic.getName());
            }
        }

        @Override
        public boolean isActive() {
            return this.active.get();
        }

        @Override
        public EventTopic getTopic() {
            return this.topic;
        }
    }
}
