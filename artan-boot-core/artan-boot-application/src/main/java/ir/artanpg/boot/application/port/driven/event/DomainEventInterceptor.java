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
 * Interceptor interface for domain event processing.
 *
 * <p>Interceptors can be used to add cross-cutting concerns such as logging,
 * metrics, validation, security checks, and other aspects of event processing.
 * They can intercept both the publishing phase and the handling phase of events.
 *
 * <p><b>Type safety:</b> the interceptor is generic over the concrete event type
 * {@code E} it applies to. All hooks receive a strongly typed
 * {@link DomainEventInterceptionContext}{@code <E>} — no raw/cast-based access
 * to the event is required inside an interceptor implementation. The multicaster
 * performs a single checked cast at dispatch time (see
 * {@link #appliesTo(DomainEvent)}) and only invokes the interceptor when it
 * declares compatibility with the runtime event class.
 *
 * <p>Implementations should be thread-safe if they are registered globally.
 *
 * @param <E> the event type this interceptor applies to
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public interface DomainEventInterceptor<E extends DomainEvent<?, ?>> {

    /**
     * Determines whether this interceptor applies to the given event.
     *
     * <p>This method is consulted before any hook runs; returning {@code true}
     * is the interceptor's contract that {@link #beforePublish},
     * {@link #afterPublish}, {@link #beforeHandle} and {@link #afterHandle}
     * can safely treat the context's event as type {@code E}. A typical
     * implementation is an instanceof check against the concrete class the
     * interceptor was parameterized with.
     *
     * @param event the event about to be processed, never null
     * @return {@code true} if this interceptor should participate in processing
     *         the event, {@code false} otherwise
     */
    default boolean appliesTo(@NonNull E event) {
        return true;
    }

    /**
     * Method called before the event is published.
     *
     * <p>This allows for pre-processing or validation before the event enters
     * the system.
     *
     * @param context the interception context containing event information and state
     */
    default void beforePublish(DomainEventInterceptionContext<E> context) {
    }

    /**
     * Method called after the event is published.
     *
     * <p>This allows for post-publishing actions or cleanup.
     *
     * @param context the interception context containing event information and state
     */
    default void afterPublish(DomainEventInterceptionContext<E> context) {
    }

    /**
     * Method called before the event is handled by a listener.
     *
     * <p>This allows for pre-processing or preparation before the actual
     * handling.
     *
     * @param context the interception context containing event information and state
     */
    default void beforeHandle(DomainEventInterceptionContext<E> context) {
    }

    /**
     * Method called after the event is handled by a listener.
     *
     * <p>This allows for post-handling actions or cleanup.
     *
     * @param context the interception context containing event information and state
     */
    default void afterHandle(DomainEventInterceptionContext<E> context) {
    }

    /**
     * Method called when an exception occurs during event processing.
     *
     * <p>This allows for custom error handling and recovery strategies.
     *
     * @param context   the interception context containing event information and state
     * @param throwable the exception that occurred during event processing
     */
    default void onError(DomainEventInterceptionContext<E> context, Throwable throwable) {
    }

    /**
     * Gets the order of execution for this interceptor.
     *
     * <p>Lower values have higher priority.
     *
     * @return the order value, defaults to 0
     */
    default int getOrder() {
        return 0;
    }
}
