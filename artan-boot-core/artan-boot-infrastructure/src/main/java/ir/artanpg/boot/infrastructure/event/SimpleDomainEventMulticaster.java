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

import ir.artanpg.boot.application.port.driven.event.DomainEventInterceptor;
import ir.artanpg.boot.application.port.driven.event.DomainEventListener;
import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
import ir.artanpg.boot.application.port.driven.event.DomainEventSmartListener;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.exception.DomainException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Simple synchronous implementation of {@link DomainEventMulticaster}.
 *
 * <p>This multicaster maintains an ordered collection of listeners and
 * interceptors. When an event is multicast:
 * <ol>
 *   <li>Listeners are filtered according to their type support and optional
 *       content filters</li>
 *   <li>Matching listeners are sorted by {@link DomainEventListener#getOrder()}</li>
 *   <li>For each listener the registered interceptors are invoked</li>
 *   <li>The listener processes the event</li>
 *   <li>Any exception is delegated to the listener's
 *       {@link DomainEventListener#exceptionHandler()}</li>
 * </ol>
 *
 * <p>All operations are thread-safe. Listener and interceptor collections use
 * {@link CopyOnWriteArrayList} to allow concurrent registration while events
 * are being dispatched.
 *
 * @author Mohammad Yazdian
 * @see DomainEventMulticaster
 * @see DomainEventListener
 * @see DomainEventInterceptor
 * @since 0.1.0
 */
public class SimpleDomainEventMulticaster implements DomainEventMulticaster {

    private static final Logger log = LoggerFactory.getLogger(SimpleDomainEventMulticaster.class);

    private final List<DomainEventListener> listeners = new CopyOnWriteArrayList<>();

    private final List<DomainEventInterceptor> interceptors = new CopyOnWriteArrayList<>();

    @Override
    public void multicastEvent(@NonNull DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        List<DomainEventListener> matchingListeners = resolveMatchingListeners(event);

        if (matchingListeners.isEmpty()) {
            log.trace("No matching listeners found for event type [{}]", event.getEventType().getName());
            return;
        }

        for (DomainEventListener listener : matchingListeners) {
            invokeListener(event, listener);
        }
    }

    @Override
    public void addDomainEventListener(@NonNull DomainEventListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        this.listeners.add(listener);
        log.debug("Registered domain event listener: {}", listener.getClass().getName());
    }

    @Override
    public void removeDomainEventListener(@NonNull DomainEventListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        this.listeners.remove(listener);
        log.debug("Removed domain event listener: {}", listener.getClass().getName());
    }

    @Override
    public void removeAllListeners() {
        this.listeners.clear();
        log.debug("Removed all domain event listeners");
    }

    @Override
    public void addInterceptor(@NonNull DomainEventInterceptor interceptor) {
        Objects.requireNonNull(interceptor, "interceptor must not be null");
        this.interceptors.add(interceptor);
        log.debug("Registered domain event interceptor: {}", interceptor.getClass().getName());
    }

    @Override
    public void removeInterceptor(@NonNull DomainEventInterceptor interceptor) {
        Objects.requireNonNull(interceptor, "interceptor must not be null");
        this.interceptors.remove(interceptor);
        log.debug("Removed domain event interceptor: {}", interceptor.getClass().getName());
    }

    /**
     * Resolves and returns the list of listeners that should receive the given
     * event, sorted by order (lowest value first).
     *
     * @param event the domain event
     * @return ordered list of matching listeners
     */
    private List<DomainEventListener> resolveMatchingListeners(DomainEvent event) {
        List<DomainEventListener> result = new ArrayList<>();

        for (DomainEventListener listener : this.listeners) {
            if (supportsEvent(listener, event)) {
                result.add(listener);
            }
        }

        result.sort(Comparator.comparingInt(DomainEventListener::getOrder));
        return result;
    }

    /**
     * Determines whether the given listener is interested in the event.
     *
     * @param listener the listener to check
     * @param event    the domain event
     * @return {@code true} if the listener should receive the event
     */
    private boolean supportsEvent(DomainEventListener listener, DomainEvent event) {
        if (listener instanceof DomainEventSmartListener smartListener) {
            if (!smartListener.supportsEventType(event.getClass())) {
                return false;
            }

            Predicate<DomainEvent> filter = smartListener.getFilter();
            if (filter != null && !filter.test(event)) {
                return false;
            }
        }
        // Plain DomainEventListener instances receive every event
        return true;
    }

    /**
     * Invokes a single listener for the given event, applying interceptors and
     * handling any exception that occurs.
     *
     * @param event    the domain event
     * @param listener the listener to invoke
     */
    private void invokeListener(DomainEvent event, DomainEventListener listener) {
        // beforeHandle
        for (DomainEventInterceptor interceptor : this.interceptors) {
            try {
                interceptor.beforeHandle(event, listener);
            }
            catch (Exception ex) {
                log.warn("Interceptor [{}] threw exception in beforeHandle", interceptor.getClass().getName(), ex);
            }
        }

        try {
            listener.process(event);

            // afterHandle
            for (DomainEventInterceptor interceptor : this.interceptors) {
                try {
                    interceptor.afterHandle(event, listener);
                }
                catch (Exception ex) {
                    log.warn("Interceptor [{}] threw exception in afterHandle", interceptor.getClass().getName(), ex);
                }
            }
        }
        catch (Exception ex) {
            // onError
            for (DomainEventInterceptor interceptor : this.interceptors) {
                try {
                    interceptor.onError(event, listener, ex);
                }
                catch (Exception interceptorEx) {
                    log.warn("Interceptor [{}] threw exception in onError", interceptor.getClass().getName(), interceptorEx);
                }
            }

            // Delegate to the listener's own exception handler
            try {
                listener.exceptionHandler().handleError(ex, event);
            }
            catch (Exception handlerEx) {
                log.error("Listener exception handler failed for event [{}]", event.getEventId(), handlerEx);
                // Re-throw as DomainException so the caller is aware of the failure
                throw new DomainException("Failed to handle exception from domain event listener", handlerEx);
            }
        }
    }
}
