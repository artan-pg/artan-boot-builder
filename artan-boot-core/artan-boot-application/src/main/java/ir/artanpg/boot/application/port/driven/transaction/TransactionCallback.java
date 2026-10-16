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
 * Functional callback for executing user-defined work inside a transaction.
 *
 * <p>Implementations encapsulate the body of a transactional unit of work.
 * The callback is invoked by the transaction manager once the transaction has
 * been started and its {@link TransactionStatus} has been made available to
 * the callback.
 *
 * <p>The value returned by {@link #doInTransaction(TransactionStatus)} is
 * passed back to the caller unchanged; the transaction manager commits the
 * transaction after the callback returns normally and rolls it back if the
 * callback throws an exception.
 *
 * @param <T> the type of result produced by the callback, use {@link Void} when no result is needed
 * @author Mohammad Yazdian
 * @see TransactionManager
 * @see TransactionStatus
 * @since 0.1.0
 */
@FunctionalInterface
public interface TransactionCallback<T> {

    /**
     * Executes the transactional work of this callback.
     *
     * <p>The supplied {@code status} describes the active transaction and can
     * be used to inspect its state or to mark it as rollback-only via
     * {@link TransactionStatus#setRollbackOnly()}. A rollback-only status will
     * cause the transaction manager to roll back the transaction instead of
     * committing it.
     *
     * <p>Throwing an exception from this method causes the transaction manager
     * to roll back the transaction and to propagate the failure to the caller
     * of {@link TransactionManager#executeInTransaction}.
     *
     * @param status the status of the active transaction
     * @return the result of the transactional work, may be {@code null} when {@code T} is {@link Void}
     *         or when  the callback intentionally returns {@code null}
     * @throws TransactionalDomainException if the transactional work fails
     */
    T doInTransaction(@NonNull TransactionStatus status) throws TransactionalDomainException;
}
