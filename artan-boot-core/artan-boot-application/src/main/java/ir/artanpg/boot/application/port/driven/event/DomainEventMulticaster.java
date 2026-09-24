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
 * Interface for multicasting domain events to registered listeners.
 *
 * <p>A multicaster is responsible for:
 * <ul>
 *   <li>Maintaining a collection of {@link DomainEventListener}s</li>
 *   <li>Resolving which listeners are interested in a given event</li>
 *   <li>Invoking listeners in the correct order</li>
 *   <li>Applying registered {@link DomainEventInterceptor}s</li>
 *   <li>Delegating exception handling to each listener's
 *       {@link ListenerExceptionHandler}</li>
 * </ul>
 *
 * <p>Implementations may support both synchronous and asynchronous execution.
 * The default implementation provided by the framework is synchronous.
 *
 * @author Mohammad Yazdian
 * @see DomainEventListener
 * @see DomainEventInterceptor
 * @see DomainEventPublisher
 * @since 0.1.0
 */
public interface DomainEventMulticaster {

    /**
     * Add a listener to be notified of domain events.
     *
     * @param listener the listener to add; must not be {@code null}
     */
    void addDomainEventListener(@NonNull DomainEventListener listener);

    /**
     * Remove a previously registered listener.
     *
     * @param listener the listener to remove; must not be {@code null}
     */
    void removeDomainEventListener(@NonNull DomainEventListener listener);

    /**
     * Remove all currently registered listeners.
     */
    void removeAllListeners();

    /**
     * Add an interceptor that will observe event processing.
     *
     * @param interceptor the interceptor to add; must not be {@code null}
     */
    void addInterceptor(@NonNull DomainEventInterceptor interceptor);

    /**
     * Remove a previously registered interceptor.
     *
     * @param interceptor the interceptor to remove
     */
    void removeInterceptor(@NonNull DomainEventInterceptor interceptor);

    /**
     * Multicast the given domain event to all matching registered listeners.
     *
     * <p>Listeners are invoked in order of their {@link DomainEventListener#getOrder()}
     * value (lowest value first). For each listener the registered
     * {@link DomainEventInterceptor}s are called before and after processing
     * (or on error).
     *
     * @param event the domain event to multicast
     */
    void multicastEvent(@NonNull DomainEvent event);
}
