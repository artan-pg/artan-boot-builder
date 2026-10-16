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

import java.io.Flushable;

/**
 * Represents the status of a transaction at a given point in time.
 *
 * <p>This interface provides a read-mostly view over the current
 * state of a transaction, along with a single mutating operation
 * ({@link #setRollbackOnly()}) that allows a participant to mark
 * the transaction for rollback without aborting it immediately.
 *
 * <p>Implementations are typically obtained from the transaction
 * manager and are valid only for the duration of the transaction
 * they describe. Callers must not cache instances across
 * transaction boundaries.
 *
 * @author Mohammad Yazdian
 * @see Status
 * @see TransactionSynchronization
 * @since 0.1.0
 */
public interface TransactionStatus extends Flushable {

    /**
     * Returns the name of the transaction.
     *
     * <p>The name is assigned when the transaction is started and is intended
     * for diagnostic and logging purposes. It is not  guaranteed to be unique
     * across transactions.
     *
     * @return the transaction name
     * @see TransactionDefinition#getName()
     */
    String getTransactionName();

    /**
     * Checks whether the transaction is new.
     *
     * <p>A transaction is considered new when it was started by the current
     * thread and no pre-existing transaction was joined or resumed. This is
     * typically used to decide whether the caller is responsible for
     * committing the transaction.
     *
     * @return {@code true} if the transaction is new, {@code false} otherwise
     */
    boolean isNewTransaction();

    /**
     * Checks whether the transaction is flagged as read-only.
     *
     * <p>A read-only transaction is expected to perform only read operations.
     * The flag is a hint to the underlying resource manager and does not by
     * itself prevent writes.
     *
     * @return {@code true} if the transaction is read-only, {@code false} otherwise
     * @see TransactionDefinition#isReadOnly()
     */
    boolean isReadOnly();

    /**
     * Marks the transaction as rollback-only.
     *
     * <p>Once marked, the transaction cannot be committed; any subsequent
     * commit attempt will result in a rollback. This operation is idempotent
     * and does not itself trigger a rollback.
     */
    void setRollbackOnly();

    /**
     * Checks whether the transaction has been marked as rollback-only.
     *
     * @return {@code true} if the transaction has been marked for rollback, {@code false} otherwise
     */
    boolean isRollbackOnly();

    /**
     * Checks whether the transaction has completed.
     *
     * <p>A transaction is considered completed after it has been either
     * committed or rolled back. Once completed, the status instance must
     * not be used for further operations.
     *
     * @return {@code true} if the transaction has completed, {@code false} otherwise
     */
    boolean isCompleted();

    /**
     * Flushes any buffered changes associated with the current transaction.
     *
     * <p>This method is inherited from {@link Flushable} and allows  a caller
     * to force pending writes to be sent to the underlying resource without
     * ending the transaction. It may be called multiple times during the
     * transaction's lifetime.
     */
    void flush() throws TransactionalDomainException;
}
