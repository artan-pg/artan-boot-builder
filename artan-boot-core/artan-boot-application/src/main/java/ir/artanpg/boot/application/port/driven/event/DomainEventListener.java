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
import ir.artanpg.boot.domain.exception.DomainException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface to be implemented by application event listeners.
 *
 * <p>A {@code DomainEventListener} can generically declare the event type that
 * it is interested in.
 *
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @see DomainEventSmartListener
 * @since 0.1.0
 */
@FunctionalInterface
public interface DomainEventListener {

    /**
     * Processes the given event.
     *
     * <p>This is the primary method that contains the logic to execute when an
     * event is dispatched to this listener. Implementations should perform the
     * necessary actions in response to the event, such as:
     * <ul>
     *   <li>Updating application state</li>
     *   <li>Triggering side effects or workflows</li>
     *   <li>Logging or auditing the event occurrence</li>
     * </ul>
     *
     * <p><b>Implementation Guidelines:</b>
     * <ul>
     *   <li>The method should complete execution in a reasonable time to avoid
     *       blocking the event dispatch thread.</li>
     *   <li>If the processing is time-consuming, consider delegating to an
     *       asynchronous executor.</li>
     *   <li>Any exceptions thrown may be handled by the configured
     *       {@link #exceptionHandler()}.</li>
     *   <li>The method should not modify the source or other properties of the
     *       {@link DomainEvent} unless explicitly allowed by the event
     *       contract.</li>
     * </ul>
     *
     * @param event the event to process
     */
    void process(@NonNull DomainEvent event);

    /**
     * Determine this listener's order in a set of listeners for the same
     * event.
     *
     * <p>The default implementation returns {@link Integer#MAX_VALUE}.
     *
     * <p><b>Note:</b> The <em>lowest</em> number indicates the
     * <em>highest</em> priority (executed earlier).
     *
     * @return the order value for this listener
     */
    default int getOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * Return whether this listener supports asynchronous execution.
     *
     * @return {@code true} if this listener instance can be executed asynchronously, {@code false} otherwise
     */
    default boolean supportsAsyncExecution() {
        return true;
    }

    /**
     * Returns the exception handler for managing errors during event
     * processing.
     *
     * <p>If the {@link #process(DomainEvent)} method throws an exception, the
     * returned exception handler will be invoked to manage the error. This
     * mechanism allows for graceful error recovery, logging, or custom error
     * handling strategies without interrupting the entire event processing
     * pipeline.
     *
     * @return the {@link ListenerExceptionHandler} for this listener
     * @see ListenerExceptionHandler
     */
    default ListenerExceptionHandler exceptionHandler() {
        return (exception, event) -> {
            Logger log = LoggerFactory.getLogger(DomainEventListener.class);
            log.error("Error invoking in the EventListener handler: the Event is: {}:{}",
                    event.getEventType().getName(),
                    event.getEventId(),
                    exception);
        };
    }

    /**
     * Creates a composed listener that sequentially executes this listener
     * followed by another.
     *
     * <p>This method enables functional composition of listeners, allowing the
     * creation of processing pipelines where multiple listeners execute in a
     * defined order.
     * The composed listener will:
     * <ol>
     *   <li>Execute this listener's {@link #process(DomainEvent)} method</li>
     *   <li>Execute the {@code after} listener's {@code process} method</li>
     * </ol>
     *
     * <p><b>Composition Characteristics:</b>
     * <ul>
     *   <li>If this listener throws an exception, the {@code after} listener will not be executed.</li>
     *   <li>The priority of the composed listener is determined by the first listener in the chain.</li>
     *   <li>The exception handler of the composed listener is that of the first listener.</li>
     * </ul>
     *
     * @param after the listener to execute after this listener, must not be {@code null}
     * @return a new {@link DomainEventListener} that represents the sequential execution
     *         of this listener followed by {@code after}
     * @throws DomainException if after listener is {@code null}
     */
    default DomainEventListener andThen(DomainEventListener after) {
        if (after == null) throw new DomainException("The after listener cannot be null");

        return new DomainEventListener() {
            @Override
            public void process(DomainEvent event) {
                DomainEventListener.this.process(event);
                after.process(event);
            }

            @Override
            public int getOrder() {
                return DomainEventListener.this.getOrder();
            }

            @Override
            public ListenerExceptionHandler exceptionHandler() {
                return DomainEventListener.this.exceptionHandler();
            }
        };
    }
}
