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
import org.jspecify.annotations.NonNull;

/**
 * Interface that encapsulates event publication functionality.
 *
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @since 0.1.0
 */
@FunctionalInterface
public interface DomainEventPublisher {

    /**
     * Notify all <strong>matching</strong> listeners registered with this
     * domain of a domain event.
     *
     * <p>Such an event publication step is effectively a hand-off to the
     * multicaster and does not imply synchronous/asynchronous execution
     * or even immediate execution at all. Event listeners are encouraged
     * to be as efficient as possible, individually using asynchronous
     * execution for longer-running and potentially blocking operations.
     *
     * <p>As with any asynchronous execution, thread-local data is not going to
     * be available for reactive listener methods. All state which is necessary
     * to process the event needs to be included in the event instance itself.
     *
     * @param event the event to publish
     */
    void publish(@NonNull DomainEvent event);
}
