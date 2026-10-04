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

/**
 * Callback interface for participating in transaction lifecycle events.
 *
 * <p>Implementations can be registered with a {@link TransactionManager}. The
 * manager will invoke the appropriate methods at well-defined points during
 * the transaction lifecycle.
 *
 * <p>The constants {@link #STATUS_COMMITTED}, {@link #STATUS_ROLLED_BACK} and
 * {@link #STATUS_UNKNOWN} are used as the argument to
 * {@link #afterCompletion(int)} to indicate the final outcome of the
 * transaction.
 *
 * <p>All methods have empty default implementations so that clients only need
 * to override the callbacks they are interested in. Typical use cases include:
 * <ul>
 *   <li>flushing caches or publishing domain events after a successful commit
 *       </li>
 *   <li>cleaning up resources after completion regardless of outcome</li>
 *   <li>reacting to suspension / resumption of the transaction (for example
 *       when {@code REQUIRES_NEW} propagation is used)</li>
 * </ul>
 *
 * <p>Invocation order:
 * <ol>
 *   <li>{@code beforeCommit} – called just before the transaction is committed
 *       (only if the transaction is not marked rollback-only)</li>
 *   <li>{@code beforeCompletion} – called before completion, whether commit or
 *       rollback</li>
 *   <li>{@code afterCommit} – called after a successful commit</li>
 *   <li>{@code afterCompletion} – called after completion with the final
 *       status</li>
 *   <li>{@code afterSuspend} / {@code afterResume} – called when the
 *       transaction is suspended or resumed (propagation-related)</li>
 * </ol>
 *
 * @author Mohammad Yazdian
 * @see TransactionManager
 * @see TransactionStatus
 * @since 0.1.0
 */
public interface TransactionSynchronization {

    /**
     * Completion status indicating that the transaction completed successfully
     * (committed).
     */
    int STATUS_COMMITTED = 0;

    /**
     * Completion status indicating that the transaction was rolled back.
     */
    int STATUS_ROLLED_BACK = 1;

    /**
     * Completion status indicating that the final outcome of the transaction
     * could not be determined (for example after a heuristic mixed outcome).
     */
    int STATUS_UNKNOWN = 2;

    /**
     * Invoked before transaction commit (only if the transaction is not marked
     * rollback-only).
     *
     * <p>This is the last opportunity to perform work that must still
     * participate in the same transactional context (for example flushing a
     * session or writing additional records).
     *
     * @param readOnly whether the transaction was started as read-only
     */
    default void beforeCommit(boolean readOnly) {
    }

    /**
     * Invoked before transaction completion, regardless of the outcome (commit
     * or rollback).
     *
     * <p>Useful for releasing resources that should not survive the end of the
     * transaction.
     */
    default void beforeCompletion() {
    }

    /**
     * Invoked after a successful commit.
     *
     * <p>At this point the transaction has already been committed; any work
     * performed here runs outside the original transactional context. Typical
     * usage is publishing domain events or updating secondary caches.
     */
    default void afterCommit() {
    }

    /**
     * Invoked after transaction completion with the final status.
     *
     * <p>This method is always called, whether the transaction committed,
     * rolled back, or ended with an unknown status.
     *
     * @param status one of {@link #STATUS_COMMITTED}, {@link #STATUS_ROLLED_BACK} or {@link #STATUS_UNKNOWN}
     */
    default void afterCompletion(int status) {
    }

    /**
     * Invoked after the current transaction has been suspended.
     *
     * <p>This occurs when a new transaction is started with
     * {@code REQUIRES_NEW} (or similar) propagation while an outer transaction
     * is still active.
     */
    default void afterSuspend() {
    }

    /**
     * Invoked after a previously suspended transaction has been resumed.
     *
     * <p>This occurs when the inner transaction started with
     * {@code REQUIRES_NEW} (or similar) completes and the outer transaction
     * becomes active again.
     */
    default void afterResume() {
    }
}
