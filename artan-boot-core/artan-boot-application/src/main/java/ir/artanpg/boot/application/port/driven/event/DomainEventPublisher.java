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

/**
 * Driven port that encapsulates domain event publication.
 *
 * <p>Implementations may deliver events synchronously to in-process listeners
 * or asynchronously to a message broker. Callers must only invoke this port
 * <strong>after</strong> the surrounding business transaction has committed
 * successfully.
 *
 * <h2>Delivery Semantics:</h2>
 * <ul>
 *   <li><b>Single event per call:</b> this port exposes one atomic operation —
 *       publishing a single {@link DomainEvent}. Batch publication, when
 *       needed, is the caller's responsibility (e.g. looping over the events
 *       returned by an aggregate's {@code pullDomainEvents()}).</li>
 *   <li><b>No ordering guarantee across calls:</b> unless the implementation
 *       explicitly provides one.</li>
 * </ul>
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
}
