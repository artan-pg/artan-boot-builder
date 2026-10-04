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

package ir.artanpg.boot.application.port.driven.transaction;

import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.domain.transaction.TransactionDefinition;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Port interface for managing transactional boundaries.
 *
 * <p>This is the driven-port abstraction that the application core uses to
 * execute code inside a transactional context. Concrete adapters (for example
 * a Spring-based implementation) are responsible for starting, committing,
 * rolling back and suspending transactions according to the supplied
 * {@link TransactionDefinition}.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Execute a {@link TransactionCallback} within a new or existing
 *       transaction</li>
 *   <li>Honor the propagation, isolation, timeout and read-only attributes
 *       defined by {@link TransactionDefinition}</li>
 *   <li>Expose the currently active {@link TransactionStatus} (if any)</li>
 *   <li>Allow registration of {@link TransactionSynchronization} callbacks
 *       that participate in the transaction lifecycle</li>
 * </ul>
 *
 * <p>Implementations must guarantee that resources acquired during the
 * transaction are properly released and that any registered synchronizations
 * are invoked at the correct points in the lifecycle.
 *
 * @author Mohammad Yazdian
 * @see TransactionCallback
 * @see TransactionDefinition
 * @see TransactionStatus
 * @see TransactionSynchronization
 * @since 0.1.0
 */
public interface TransactionManager {

    /**
     * Executes the given callback inside a transaction using default
     * definition attributes.
     *
     * @param <T>       the result type of the callback
     * @param operation the transactional work to perform
     * @return the result returned by the callback
     * @throws DomainException if the callback fails or if the underlying transaction infrastructure encounters an error
     */
    <T> T executeInTransaction(@NonNull TransactionCallback<T> operation);

    /**
     * Executes the given callback inside a transaction governed by the
     * supplied definition.
     *
     * <p>The {@link TransactionDefinition} controls:
     * <ul>
     *   <li>propagation behavior</li>
     *   <li>isolation level</li>
     *   <li>timeout</li>
     *   <li>read-only flag</li>
     *   <li>logical transaction name</li>
     * </ul>
     *
     * @param <T>        the result type of the callback
     * @param operation  the transactional work to perform
     * @param definition the transaction attributes to apply
     * @return the result returned by the callback
     * @throws DomainException if the callback fails or if the underlying transaction infrastructure encounters an error
     */
    <T> T executeInTransaction(@NonNull TransactionCallback<T> operation, @NonNull TransactionDefinition definition);

    /**
     * Returns the status of the currently active transaction, if any.
     *
     * <p>When no transaction is active this method returns {@code null}.
     * Callers should treat a non-null return value as an indication that they
     * are already participating in a transactional context.
     *
     * @return the current {@link TransactionStatus}, or {@code null} if no transaction is active
     */
    @Nullable
    TransactionStatus getCurrentTransaction();

    /**
     * Registers a synchronization callback that will be notified of
     * transaction lifecycle events.
     *
     * <p>Synchronizations are invoked in the order they were registered for
     * {@code before*} callbacks and in reverse order for {@code after*}
     * callbacks. Registration is only valid while a transaction is active;
     * calling this method outside a transactional context may result in an
     * exception depending on the concrete implementation.
     *
     * @param synchronization the synchronization to register
     */
    void registerSynchronization(@NonNull TransactionSynchronization synchronization);
}
