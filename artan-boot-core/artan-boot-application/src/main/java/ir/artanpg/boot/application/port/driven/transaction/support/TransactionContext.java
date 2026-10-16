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

package ir.artanpg.boot.application.port.driven.transaction.support;

import ir.artanpg.boot.application.port.driven.transaction.TransactionStatus;
import org.jspecify.annotations.Nullable;

/**
 * Thread-bound holder for the current {@link TransactionStatus}.
 *
 * <p>This utility class associates a {@code TransactionStatus} with
 * the calling thread so that components without direct access to the
 * transaction manager can still inspect the active transaction's
 * state. The association is stored in a {@link ThreadLocal} and
 * therefore is not visible to other threads.
 *
 * <p>Callers are responsible for clearing the context when a
 * transaction ends. The recommended pattern is to call
 * {@link #setCurrentStatus(TransactionStatus)} when a transaction
 * begins and {@link #clear()} in a {@code finally} block when it
 * ends, to avoid leaking state across logical transactions that
 * happen to reuse the same thread.
 *
 * <p>This class is a stateless utility holder; all methods are
 * static and thread-safe with respect to per-thread state.
 *
 * @author Mohammad Yazdian
 * @see TransactionStatus
 * @since 0.1.0
 */
public final class TransactionContext {

    /**
     * Thread-local holding the current transaction status.
     *
     * <p>Never {@code null}, the contained value may be {@code null} when no
     * transaction is active on the current thread. The value is managed
     * through the static methods of this class rather than accessed directly.
     */
    private static final ThreadLocal<TransactionStatus> CURRENT_STATUS = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws UnsupportedOperationException if called via reflection (implicitly)
     */
    private TransactionContext() {
        throw new UnsupportedOperationException();
    }

    /**
     * Binds the given status to the current thread.
     *
     * <p>When {@code status} is {@code null}, any previously bound status is
     * removed from the current thread, leaving the thread with no active
     * transaction context. When {@code status} is non-null, it replaces any
     * previously bound status.
     *
     * @param status the transaction status to bind, or {@code null} to clear the current thread's binding
     */
    public static void setCurrentStatus(@Nullable TransactionStatus status) {
        if (status != null) {
            CURRENT_STATUS.set(status);
        } else {
            CURRENT_STATUS.remove();
        }
    }

    /**
     * Returns the transaction status bound to the current thread.
     *
     * @return the current transaction status, or {@code null} when no transaction is active on the current thread
     */
    @Nullable
    public static TransactionStatus getCurrentStatus() {
        return CURRENT_STATUS.get();
    }

    /**
     * Clears the transaction status bound to the current thread.
     *
     * <p>After this call, {@link #getCurrentStatus()} returns {@code null} and
     * {@link #isTransactionActive()} returns {@code false} until a new status
     * is bound via {@link #setCurrentStatus(TransactionStatus)}.
     */
    public static void clear() {
        CURRENT_STATUS.remove();
    }

    /**
     * Checks whether a transaction is active on the current thread.
     *
     * @return {@code true} if a transaction status is bound to the current thread, {@code false} otherwise
     */
    public static boolean isTransactionActive() {
        return CURRENT_STATUS.get() != null;
    }
}
