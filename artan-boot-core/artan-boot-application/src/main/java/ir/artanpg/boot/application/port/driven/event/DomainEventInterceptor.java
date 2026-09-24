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
 * Interceptor to observe and optionally influence the processing of domain
 * events.
 *
 * <p>Typical use cases include:
 * <ul>
 *   <li>Logging and auditing</li>
 *   <li>Metrics collection</li>
 *   <li>Distributed tracing</li>
 *   <li>Performance monitoring</li>
 *   <li>Security checks</li>
 * </ul>
 *
 * <p>All methods have default empty implementations so that implementors only
 * need to override the callbacks they are interested in.
 *
 * @author Mohammad Yazdian
 * @see DomainEventListener
 * @since 0.1.0
 */
public interface DomainEventInterceptor {

    /**
     * Invoked before a listener processes the given event.
     *
     * <p>This method is called for every matching listener, immediately before
     * {@link DomainEventListener#process(DomainEvent)} is invoked.
     *
     * @param event    the domain event about to be processed
     * @param listener the listener that will process the event
     */
    default void beforeHandle(@NonNull DomainEvent event, @NonNull DomainEventListener listener) {
        // default no-op
    }

    /**
     * Invoked after a listener has successfully processed the given event.
     *
     * <p>This method is called only when
     * {@link DomainEventListener#process(DomainEvent)} completes without
     * throwing an exception.
     *
     * @param event    the domain event that was processed
     * @param listener the listener that processed the event
     */
    default void afterHandle(@NonNull DomainEvent event, @NonNull DomainEventListener listener) {
        // default no-op
    }

    /**
     * Invoked when a listener throws an exception while processing an event.
     *
     * <p>This method is called before the listener's own
     * {@link DomainEventListener#exceptionHandler()} is invoked.
     *
     * @param event     the domain event that was being processed
     * @param listener  the listener that threw the exception
     * @param exception the exception that was thrown
     */
    default void onError(@NonNull DomainEvent event,
                         @NonNull DomainEventListener listener,
                         @NonNull Exception exception) {
        // default no-op
    }
}
