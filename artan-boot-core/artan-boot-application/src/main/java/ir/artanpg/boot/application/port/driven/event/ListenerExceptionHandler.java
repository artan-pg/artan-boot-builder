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

/**
 * Strategy interface for handling exceptions thrown by
 * {@link DomainEventListener}s during the processing of a {@link DomainEvent}.
 *
 * <p>Implementations of this interface can define custom error handling
 * logic, such as logging the error, sending alerts, or triggering compensating
 * actions without disrupting the overall event dispatching mechanism.
 *
 * @author Mohammad Yazdian
 * @version 0.1.0
 * @see DomainEvent
 * @see DomainEventListener
 * @see DomainEventSmartListener
 */
public interface ListenerExceptionHandler {

    /**
     * Handles an exception that occurred while processing a domain event.
     *
     * @param exception the exception thrown during event processing
     * @param event     the domain event being processed when the exception occurred
     */
    void handleError(Exception exception, DomainEvent event);

    /**
     * Returns a composed {@link ListenerExceptionHandler} that performs, in
     * sequence, this handler's {@link #handleError(Exception, DomainEvent)}
     * operation, followed by the {@code after} handler's
     * {@link #handleError(Exception, DomainEvent)} operation.
     *
     * <p>If performing either handler throws an exception, it is propagated to
     * the caller of the composed handler. If this handler throws an exception,
     * the {@code after} handler will not be performed.
     *
     * @param after the handler to perform after this handler.
     * @return a composed {@link ListenerExceptionHandler} that performs in sequence this
     *         operation followed by the {@code after} operation
     * @throws DomainException if after is {@code null}
     */
    default ListenerExceptionHandler andThen(ListenerExceptionHandler after) {
        if (after == null) throw new DomainException("The after listener exception handler cannot be null");
        return (exception, event) -> {
            this.handleError(exception, event);
            after.handleError(exception, event);
        };
    }
}
