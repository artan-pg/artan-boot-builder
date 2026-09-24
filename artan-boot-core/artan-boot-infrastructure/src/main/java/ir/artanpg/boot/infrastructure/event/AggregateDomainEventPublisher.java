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
import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.model.AggregateRoot;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Helper that publishes all domain events collected by an
 * {@link AggregateRoot}.
 *
 * <p>Supports both phase-1 ({@link DomainEventPublisher}) and phase-2
 * ({@link DomainEventBus}) dispatch. When a bus is configured, events are
 * published through the bus (which also forwards to the type-based publisher).
 * Otherwise only the publisher is used.
 *
 * <p>Typical usage after persisting the aggregate:
 * <pre>{@code
 * repository.save(account);
 * aggregateDomainEventPublisher.publishEventsFrom(account);
 * }</pre>
 *
 * @author Mohammad Yazdian
 * @see DomainEventPublisher
 * @see DomainEventBus
 * @see AggregateRoot#getDomainEvents()
 * @since 0.1.0
 */
public class AggregateDomainEventPublisher {

    private final Consumer<DomainEvent> dispatcher;

    /**
     * Creates a publisher that dispatches only through the type-based publisher.
     *
     * @param publisher the publisher; must not be {@code null}
     */
    public AggregateDomainEventPublisher(@NonNull DomainEventPublisher publisher) {
        Objects.requireNonNull(publisher, "publisher must not be null");
        this.dispatcher = publisher::publish;
    }

    /**
     * Creates a publisher that dispatches through the event bus (preferred for
     * phase 2).
     *
     * @param eventBus the event bus; must not be {@code null}
     */
    public AggregateDomainEventPublisher(@NonNull DomainEventBus eventBus) {
        Objects.requireNonNull(eventBus, "eventBus must not be null");
        this.dispatcher = eventBus::publish;
    }

    /**
     * Creates a publisher that prefers the bus when available, otherwise falls
     * back to the type-based publisher.
     *
     * @param publisher the type-based publisher; must not be {@code null}
     * @param eventBus  optional event bus
     */
    public AggregateDomainEventPublisher(@NonNull DomainEventPublisher publisher,
                                         @Nullable DomainEventBus eventBus) {
        Objects.requireNonNull(publisher, "publisher must not be null");
        if (eventBus != null) {
            this.dispatcher = eventBus::publish;
        }
        else {
            this.dispatcher = publisher::publish;
        }
    }

    /**
     * Publishes all domain events currently registered on the given aggregate.
     *
     * @param aggregate the aggregate; must not be {@code null}
     */
    public void publishEventsFrom(@NonNull AggregateRoot<?> aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        List<DomainEvent> events = aggregate.getDomainEvents();
        for (DomainEvent event : events) {
            this.dispatcher.accept(event);
        }
    }
}
