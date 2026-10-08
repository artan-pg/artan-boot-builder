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

import ir.artanpg.boot.application.port.driving.event.DomainEventListener;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.exception.InterceptorVetoException;
import org.jspecify.annotations.NonNull;

/**
 * Interface for multicasting domain events to registered listeners.
 *
 * <p>A multicaster is responsible for:
 * <ul>
 *   <li>Maintaining a collection of {@link DomainEventListener}s</li>
 *   <li>Invoking listeners in order of {@link DomainEventListener#getOrder()}</li>
 *   <li>Applying registered {@link DomainEventInterceptor}s around each
 *       delivery, including the publish-level hooks and failure reporting</li>
 *   <li>Routing each listener to synchronous or asynchronous dispatch based on
 *       {@link DomainEventListener#supportsAsyncExecution()}</li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @see DomainEventListener
 * @see DomainEventPublisher
 * @see DomainEventInterceptor
 * @since 0.1.0
 */
public interface DomainEventMulticaster {

    /**
     * Add a listener to be notified of domain events.
     *
     * @param listener the listener to add
     * @return {@code true} if the listener has not already been registered, {@code false} otherwise
     */
    boolean addListener(@NonNull DomainEventListener<?> listener);

    /**
     * Remove a registered listener.
     *
     * @param listener the listener to remove
     * @return {@code true} if the listener was previously registered and is removed, {@code false} otherwise
     */
    boolean removeListener(DomainEventListener<?> listener);

    /**
     * Remove a registered listener based on its identifier.
     *
     * @param listenerId the unique listener identifier
     * @return {@code true} if the listener with the specified identifier was previously registered and is removed,
     *         {@code false} otherwise
     */
    boolean removeListener(String listenerId);

    /**
     * Remove all currently registered listeners.
     */
    void removeAllListeners();

    /**
     * Add an interceptor that will observe event processing.
     *
     * @param interceptor the interceptor to add
     * @param <E>         the event type the interceptor applies to
     */
    <E extends DomainEvent<?, ?>> void addInterceptor(@NonNull DomainEventInterceptor<E> interceptor);

    /**
     * Remove a previously registered interceptor.
     *
     * @param interceptor the interceptor to remove
     * @param <E>         the event type the interceptor removes to
     */
    <E extends DomainEvent<?, ?>> void removeInterceptor(@NonNull DomainEventInterceptor<E> interceptor);

    /**
     * Multicast the given domain event to all matching registered listeners.
     *
     * <p>Listeners are invoked in order of their {@code getOrder()} value
     * (lowest first). Listeners returning {@code true} from
     * {@code supportsAsyncExecution()} are dispatched on the configured
     * executor; the rest run on the calling thread. Interceptor hooks
     * ({@code beforePublish}, {@code afterPublish}) wrap the whole multicast;
     * per-listener hooks ({@code beforeHandle}/{@code afterHandle}/
     * {@code onError}) wrap each delivery. If any interceptor vetoes the
     * publication, an {@code InterceptorVetoException} is thrown and no
     * listener is invoked.
     *
     * @param event the domain event to multicast; must not be {@code null}
     * @throws InterceptorVetoException if an interceptor vetoes the publication
     */
    void multicastEvent(@NonNull DomainEvent<?, ?> event);
}
