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

import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.jspecify.annotations.NonNull;

/**
 * Central port for programmatic transaction management.
 *
 * <p>All methods are expected to be safe for use from the thread that owns
 * the current transaction. Cross-thread propagation of transactional context
 * is implementation-specific.
 *
 * @author Mohammad Yazdian
 * @see TransactionCallback
 * @see TransactionDefinition
 * @see TransactionStatus
 * @since 0.1.0
 */
public interface TransactionManager {

    /**
     * Executes the given callback inside a transaction and commits the
     * transaction when the callback completes normally.
     *
     * <p>A default {@link TransactionDefinition} is used to start the
     * transaction. If the callback throws an exception, the transaction
     * is rolled back and the exception is propagated as a
     * {@link TransactionalDomainException}.
     *
     * @param <T>       the type of result produced by the callback
     * @param operation the callback to execute
     * @return the value returned by the callback, may be {@code null} if the callback returns {@code null}
     * @throws TransactionalDomainException if the transaction cannot be started, committed, or rolled back,
     *                                      or if the callback itself fails
     */
    <T> T executeInTransaction(@NonNull TransactionCallback<T> operation) throws TransactionalDomainException;

    /**
     * Executes the given callback inside a transaction configured by the
     * supplied definition and commits the transaction when the callback
     * completes normally.
     *
     * <p>The {@code definition} controls propagation, isolation, timeout,
     * read-only flag, and the transaction name. If the callback throws an
     * exception, the transaction is rolled back and the exception is
     * propagated as a {@link TransactionalDomainException}.
     *
     * @param <T>        the type of result produced by the callback
     * @param operation  the callback to execute
     * @param definition the transaction configuration
     * @return the value returned by the callback, may be {@code null} if the callback returns {@code null}
     * @throws TransactionalDomainException if the transaction cannot be started, committed, or rolled back,
     *                                      or if the callback itself fails
     */
    <T> T executeInTransaction(@NonNull TransactionCallback<T> operation,
                               @NonNull TransactionDefinition definition) throws TransactionalDomainException;

    /**
     * Returns the status of the current transaction.
     *
     * <p>When no transaction is active for the calling thread, an
     * implementation may return a status whose
     * {@link TransactionStatus#isCompleted()} returns {@code true}
     * or whose state is otherwise marked as inactive. Callers should
     * not assume that a non-null return value implies an active
     * transaction.
     *
     * @return the current transaction status
     */
    TransactionStatus getStatus();
}
