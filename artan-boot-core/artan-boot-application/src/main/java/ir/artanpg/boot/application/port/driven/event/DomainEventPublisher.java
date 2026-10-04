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
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Driven port that encapsulates domain event publication.
 *
 * <p>Implementations may deliver events synchronously to in-process listeners
 * or asynchronously to a message broker. Callers must only invoke this port
 * <strong>after</strong> the surrounding business transaction has committed
 * successfully.
 *
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @since 0.1.0
 */
@FunctionalInterface
public interface DomainEventPublisher {

    /**
     * Publishes a single domain event to all matching listeners/channels.
     *
     * @param event the event to publish
     * @throws DomainEventException if the event is {@code null} or publication fails fatally
     */
    void publish(@NonNull DomainEvent<?, ?> event);

    /**
     * Publishes all events in order.
     *
     * @param events events to publish
     * @throws DomainEventException if the events list is {@code null} or publication fails fatally
     */
    @SuppressWarnings("ConstantValue")
    default void publishAll(@NonNull List<? extends DomainEvent<?, ?>> events) {
        if (events == null) throw new DomainEventException("events must not be null");

        for (DomainEvent<?, ?> event : events) {
            publish(event);
        }
    }
}
