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
import org.jspecify.annotations.NonNull;

/**
 * Contract for components that manage {@link DomainEventListener} registrations
 * and multicast {@link DomainEvent}s to all matching listeners.
 *
 * <p>A typical implementation is expected to provide:
 * <ul>
 *   <li><b>Routing:</b> dispatching based on {@code EventType} and payload type</li>
 *   <li><b>Ordering:</b> honoring {@link DomainEventListener#getOrder()}</li>
 *   <li><b>Async execution:</b> honoring {@link DomainEventListener#supportsAsyncExecution()}</li>
 *   <li><b>Filtering:</b> applying {@code DomainEventSmartListener.getFilter()}</li>
 *   <li><b>Interception:</b> invoking registered {@link DomainEventInterceptor}s around each handler call</li>
 *   <li><b>Error isolation:</b> routing failures through {@link DomainEventListener#exceptionHandler()}</li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @version 0.1.0
 * @see DomainEventListener
 * @see DomainEventPublisher
 * @since 0.1.0
 */
public interface DomainEventMulticaster {

    /**
     * Multicasts the given event to all matching registered listeners.
     *
     * @param event the domain event to dispatch; must not be {@code null}
     */
    void multicastEvent(@NonNull DomainEvent event);

    /**
     * Registers a listener to receive events from this multicaster.
     *
     * @param listener the listener to add; must not be {@code null}
     */
    void addListener(@NonNull DomainEventListener<? extends DomainEvent> listener);

    /**
     * Removes a previously registered listener. Removing a listener that is
     * not registered is a no-op.
     *
     * @param listener the listener to remove; must not be {@code null}
     */
    void removeListener(@NonNull DomainEventListener<? extends DomainEvent> listener);

    /**
     * Registers an interceptor to observe listener invocations.
     *
     * @param interceptor the interceptor to add; must not be {@code null}
     */
    void addInterceptor(@NonNull DomainEventInterceptor interceptor);

    /**
     * Removes a previously registered interceptor.
     *
     * @param interceptor the interceptor to remove; must not be {@code null}
     */
    void removeInterceptor(@NonNull DomainEventInterceptor interceptor);
}
