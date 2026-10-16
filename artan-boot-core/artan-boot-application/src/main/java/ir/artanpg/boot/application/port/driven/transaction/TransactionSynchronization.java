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
 * Callback interface for participating in the lifecycle of a transaction.
 *
 * <p>Implementations can be registered with the transaction manager to
 * receive notifications before and after the transaction completes. The
 * hooks are invoked in the following order:
 * <ol>
 *   <li>{@link #beforeCommit(boolean)}</li>
 *   <li>{@link #beforeCompletion()}</li>
 *   <li>{@link #afterCommit()} on successful commit</li>
 *   <li>{@link #afterCompletion(int)} in all cases</li>
 * </ol>
 *
 * <p>Implementations must be thread-safe when shared across concurrent
 * transactions.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public interface TransactionSynchronization extends Flushable {

    /**
     * Returns the execution order of this synchronization.
     *
     * <p>Lower values indicate higher precedence: synchronizations with
     * smaller order values run first for "before" callbacks and last for
     * "after" callbacks. The default value is {@link Integer#MAX_VALUE},
     * meaning implementations that do not override this method run last
     * in "before" hooks and first in "after" hooks.
     *
     * @return the execution order; defaults to {@link Integer#MAX_VALUE}
     */
    default int order() {
        return Integer.MAX_VALUE;
    }

    /**
     * Invoked before the transaction is committed.
     *
     * <p>This callback allows a participant to flush state or perform
     * last-minute work before the commit is finalized.
     * A runtime exception thrown from this method causes the transaction
     * to roll back.
     *
     * @param readOnly {@code true} if the transaction is flagged as read-only, {@code false} otherwise
     */
    void beforeCommit(boolean readOnly);

    /**
     * Invoked after the transaction has been successfully committed.
     *
     * <p>This callback is only called when the commit succeeds. It
     * is not invoked when the transaction is rolled back.
     */
    void afterCommit();

    /**
     * Invoked before the transaction completes, regardless of the outcome.
     *
     * <p>This callback is called before both commit and rollback. It provides
     * a last opportunity to release resources that are independent of the
     * transaction's outcome.
     */
    void beforeCompletion();

    /**
     * Invoked after the transaction has completed.
     *
     * <p>This callback is called in all cases, whether the transaction was
     * committed or rolled back. The {@code status} argument indicates the
     * outcome.
     *
     * <p>One of the status codes defined by the transaction manager, such as
     * {@code STATUS_COMMITTED} or {@code STATUS_ROLLED_BACK}
     *
     * @param status the completion status
     * @see Status
     */
    void afterCompletion(int status);

    /**
     * Flushes any buffered state held by this synchronization.
     *
     * <p>This method is inherited from {@link Flushable} and is invoked by the
     * transaction manager to allow a participant to push buffered changes to
     * the underlying resource before the transaction proceeds further. It may
     * be called multiple times during a single transaction.
     */
    void flush() throws TransactionalDomainException;
}
