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

import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-bound registry for {@link TransactionSynchronization} callbacks.
 *
 * <p>This utility class maintains, for each thread, a flag indicating whether
 * transaction synchronization is currently active and an ordered list of the
 * synchronizations registered against the current transaction. The state is
 * stored in {@link ThreadLocal} fields and therefore is not shared across
 * threads.
 *
 * <p>The typical lifecycle is:
 * <ol>
 *   <li>{@link #activateSynchronization()} is called when a transaction
 *       starts.</li>
 *   <li>{@link #registerSynchronization(TransactionSynchronization)}  may be
 *       called zero or more times while the transaction is active.</li>
 *   <li>{@link #getSynchronizations()} is used by the transaction manager to
 *       drive the lifecycle callbacks.</li>
 *   <li>{@link #deactivateSynchronization()} is called when the transaction
 *       ends, releasing both the activation flag and the registered
 *       synchronizations.</li>
 * </ol>
 *
 * <p>All methods are static and thread-safe with respect to per-thread state.
 *
 * @author Mohammad Yazdian
 * @see TransactionSynchronization
 * @since 0.1.0
 */
public final class TransactionSynchronizationManager {

    /**
     * Thread-local list of synchronizations registered for the current
     * transaction.
     *
     * <p>The list is initialized lazily to a {@link CopyOnWriteArrayList},
     * so reads and writes during callback iteration are safe even if a
     * synchronization registers another synchronization reentrantly.
     */
    private static final ThreadLocal<List<TransactionSynchronization>> SYNCHRONIZATIONS =
            ThreadLocal.withInitial(CopyOnWriteArrayList::new);

    /**
     * Thread-local activation flag.
     *
     * <p>Defaults to {@code false} for every thread. The flag is set to
     * {@code true} by {@link #activateSynchronization()} and removed
     * (resetting it to {@code false}) by {@link #deactivateSynchronization()}.
     */
    private static final ThreadLocal<Boolean> SYNCHRONIZATION_ACTIVE = ThreadLocal.withInitial(() -> false);

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws UnsupportedOperationException if called via reflection (implicitly)
     */
    private TransactionSynchronizationManager() {
        throw new UnsupportedOperationException();
    }

    /**
     * Activates transaction synchronization for the current thread.
     *
     * <p>After this call, {@link #getSynchronizationActive()} returns
     * {@code true} and synchronizations may be registered through
     * {@link #registerSynchronization(TransactionSynchronization)}.
     */
    public static void activateSynchronization() {
        SYNCHRONIZATION_ACTIVE.set(true);
    }

    /**
     * Deactivates transaction synchronization for the current thread and
     * releases all associated state.
     *
     * <p>Both the activation flag and the registered synchronizations are
     * removed from the current thread. After this call,
     * {@link #getSynchronizationActive()} returns {@code false} and
     * {@link #getSynchronizations()} returns an empty list.
     */
    public static void deactivateSynchronization() {
        SYNCHRONIZATION_ACTIVE.remove();
        SYNCHRONIZATIONS.remove();
    }

    /**
     * Checks whether transaction synchronization is active on the current
     * thread.
     *
     * @return {@code true} if synchronization is active, {@code false} otherwise
     */
    public static boolean getSynchronizationActive() {
        return SYNCHRONIZATION_ACTIVE.get();
    }

    /**
     * Registers a synchronization for the current transaction.
     *
     * <p>The synchronization is appended to the current thread's list in
     * registration order. Invocation order is determined later by the
     * transaction manager using {@link TransactionSynchronization#getOrder()}.
     *
     * @param synchronization the synchronization to register
     * @throws TransactionalDomainException if synchronization is not currently active on the current thread
     */
    public static void registerSynchronization(@NonNull TransactionSynchronization synchronization) {
        if (!getSynchronizationActive()) {
            throw new TransactionalDomainException("Transaction synchronization not active");
        }
        SYNCHRONIZATIONS.get().add(synchronization);
    }

    /**
     * Returns a snapshot of the synchronizations registered for the current
     * thread.
     *
     * <p>The returned list is a defensive copy; later registrations or
     * removals do not affect it. The order matches registration order, not
     * {@link TransactionSynchronization#getOrder()}.
     *
     * @return a snapshot list of synchronizations, never {@code null}, possibly empty
     */
    public static List<TransactionSynchronization> getSynchronizations() {
        return new ArrayList<>(SYNCHRONIZATIONS.get());
    }

    /**
     * Removes all synchronizations registered for the current thread.
     *
     * <p>The activation flag is not affected by this call. Use
     * {@link #deactivateSynchronization()} to release both the flag  and the
     * list.
     */
    public static void clearSynchronizations() {
        SYNCHRONIZATIONS.get().clear();
    }
}
