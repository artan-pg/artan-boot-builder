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
import org.jspecify.annotations.NonNull;

/**
 * Callback interface for code that needs to execute within an active
 * transactional context.
 *
 * <p>This functional interface is the primary abstraction used by
 * {@link TransactionManager} to run business logic inside a transaction.
 * Implementations receive a {@link TransactionStatus} that allows inspection
 * and control of the current transaction (for example marking it for rollback
 * or creating savepoint).
 *
 * <p>Any {@link DomainException} thrown from
 * {@link #doInTransaction(TransactionStatus)} will typically cause the
 * surrounding transaction to be rolled back, depending on the concrete
 * {@link TransactionManager} implementation and the configured rollback rules.
 *
 * <p>Typical usage:
 * <pre>{@code
 * transactionManager.executeInTransaction(status -> {
 *     // business logic that participates in the current transaction
 *     return result;
 * });
 * }</pre>
 *
 * @param <T> the result type returned by the callback
 * @author Mohammad Yazdian
 * @see TransactionManager
 * @see TransactionStatus
 * @since 0.1.0
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    /**
     * Executes the transactional work.
     *
     * <p>The provided {@code status} object can be used to:
     * <ul>
     *   <li>query whether the current transaction is new</li>
     *   <li>mark the transaction for rollback-only</li>
     *   <li>create, roll back to, or release savepoint (when supported)</li>
     * </ul>
     *
     * @param status the status of the currently active transaction
     * @return the result of the transactional operation (maybe {@code null} if the implementation permits it)
     * @throws DomainException if the business logic fails; the surrounding transaction is expected to be rolled back
     */
    T doInTransaction(@NonNull TransactionStatus status) throws DomainException;
}
