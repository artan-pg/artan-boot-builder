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

import ir.artanpg.boot.domain.transaction.TransactionDefinition;
import ir.artanpg.boot.domain.transaction.exception.TransactionException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents the runtime status of an active transaction.
 *
 * <p>A {@code TransactionStatus} instance is obtained either from
 * {@link TransactionManager#getCurrentTransaction()} or is passed as an
 * argument to {@link TransactionCallback#doInTransaction(TransactionStatus)}.
 * It provides a controlled API for inspecting and influencing the current
 * transactional context.
 *
 * <p>Typical capabilities exposed by this interface:
 * <ul>
 *   <li>Determine whether the current transaction was newly started for this
 *       invocation or joined an existing one</li>
 *   <li>Create, roll back to, and release savepoint (nested transaction
 *       support)</li>
 *   <li>Mark the transaction as rollback-only</li>
 *   <li>Query completion and rollback-only state</li>
 *   <li>Optionally retrieve a logical transaction name</li>
 * </ul>
 *
 * <p>Implementations are expected to be lightweight and thread-bound; a status
 * object must not be shared across threads.
 *
 * @author Mohammad Yazdian
 * @see TransactionManager
 * @see TransactionCallback
 * @see TransactionSynchronization
 * @since 0.1.0
 */
public interface TransactionStatus {

    /**
     * Indicates whether the current transaction was newly started for this
     * invocation rather than joining an already existing one.
     *
     * @return {@code true} if a new transaction was started, {@code false} if an
     *         existing transaction is being participated in
     */
    boolean isNewTransaction();

    /**
     * Indicates whether a savepoint has been created for the current
     * transaction.
     *
     * <p>Savepoint enable nested transaction semantics (partial rollback).
     *
     * @return {@code true} if at least one savepoint exists, {@code false} otherwise
     */
    boolean hasSavepoint();

    /**
     * Creates a new savepoint within the current transaction.
     *
     * <p>The returned opaque object can later be passed to
     * {@link #rollbackToSavepoint(Object)} or
     * {@link #releaseSavepoint(Object)}.
     *
     * @return an opaque savepoint object
     * @throws TransactionException if savepoint are not supported or creation fails
     */
    @NonNull
    Object createSavepoint() throws TransactionException;

    /**
     * Rolls the current transaction back to the given savepoint.
     *
     * <p>All changes performed after the savepoint was created are undone.
     * The transaction itself remains active.
     *
     * @param savepoint the savepoint obtained from {@link #createSavepoint()}
     * @throws TransactionException if the savepoint is invalid or the operation fails
     */
    void rollbackToSavepoint(@NonNull Object savepoint) throws TransactionException;

    /**
     * Releases the given savepoint.
     *
     * <p>After release the savepoint can no longer be used for rollback.
     * Releasing a savepoint does not affect the outcome of the surrounding
     * transaction.
     *
     * @param savepoint the savepoint to release
     * @throws TransactionException if the savepoint is invalid or the operation fails
     */
    void releaseSavepoint(@NonNull Object savepoint) throws TransactionException;

    /**
     * Marks the current transaction as rollback-only.
     *
     * <p>Once marked, the transaction will be rolled back on completion even
     * if the callback returns normally. This is useful when a business rule
     * violation is detected after some work has already been performed.
     */
    void setRollbackOnly();

    /**
     * Indicates whether the current transaction has been marked as
     * rollback-only.
     *
     * @return {@code true} if {@link #setRollbackOnly()} has been called, {@code false} otherwise
     */
    boolean isRollbackOnly();

    /**
     * Indicates whether the transaction has already been completed (committed
     * or rolled back).
     *
     * @return {@code true} if the transaction is completed, {@code false} if it is still active
     */
    boolean isCompleted();

    /**
     * Returns the logical name of the current transaction, if available.
     *
     * <p>The default implementation returns {@code null}. Concrete
     * implementations may override this method to expose the name supplied via
     * {@link TransactionDefinition#getName()}.
     *
     * @return the transaction name, or {@code null} if not available
     */
    @Nullable
    default String getTransactionName() {
        return null;
    }
}
