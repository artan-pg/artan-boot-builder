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

package ir.artanpg.boot.infrastructure.adapter.driven.transaction;

import ir.artanpg.boot.application.port.driven.transaction.Status;
import ir.artanpg.boot.application.port.driven.transaction.TransactionDefinition;
import ir.artanpg.boot.application.port.driven.transaction.TransactionIsolation;
import ir.artanpg.boot.application.port.driven.transaction.TransactionStatus;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple implementation of {@link TransactionStatus}.
 *
 * <p>An instance starts in {@link Status#ACTIVE} state, tracks the
 * rollback-only flag through {@link #setRollbackOnly()}, and moves to a
 * terminal state ({@link Status#COMMITTED}, {@link Status#ROLLED_BACK}, or
 * {@link Status#NO_TRANSACTION}) when the manager completes the transaction.
 * All state transitions are synchronized so that the status can be inspected
 * safely from other threads, for example by monitoring code.
 *
 * <p>Instances are created by the transaction manager; callers should treat
 * them as read-mostly views over the current transaction and must not cache
 * them across transaction boundaries.
 *
 * @author Mohammad Yazdian
 * @see TransactionStatus
 * @since 0.1.0
 */
public class SimpleTransactionStatus implements TransactionStatus {

    private final TransactionDefinition definition;
    private final boolean newTransaction;
    private Status status = Status.ACTIVE;
    private final AtomicReference<Runnable> flushAction = new AtomicReference<>();

    /**
     * Constructs a status for a transaction started with the given definition.
     *
     * @param definition     the configuration the transaction was started with
     * @param newTransaction {@code true} when the transaction is brand new rather than joined
     * @param flushAction    the flush hook supplied by the owning manager, or {@code null} when not yet bound
     */
    public SimpleTransactionStatus(@NonNull TransactionDefinition definition,
                                   boolean newTransaction,
                                   @Nullable Runnable flushAction) {
        if (definition == null) throw new TransactionalDomainException("Transaction definition cannot be null");

        this.definition = definition;
        this.newTransaction = newTransaction;
        this.flushAction.set(flushAction);
    }

    /**
     * Binds the flush hook after the status instance exists.
     *
     * <p>Used by {@link AbstractResourceTransactionManager} so {@code doFlush}
     * can close over the real status without a temporary placeholder.
     *
     * @param flushAction the flush hook; ignored when {@code null}
     */
    public void bindFlushAction(@Nullable Runnable flushAction) {
        if (flushAction != null) this.flushAction.set(flushAction);
    }

    @Override
    public String getTransactionName() {
        return definition.getName();
    }

    @Override
    public boolean isNewTransaction() {
        return newTransaction;
    }

    @Override
    public boolean isReadOnly() {
        return definition.isReadOnly();
    }

    /**
     * Returns the isolation level the transaction was started with.
     *
     * @return the configured isolation level
     */
    public TransactionIsolation getIsolationLevel() {
        return definition.getIsolation();
    }

    @Override
    public synchronized void setRollbackOnly() {
        if (status == Status.ACTIVE) status = Status.MARKED_ROLLBACK;
    }

    @Override
    public synchronized boolean isRollbackOnly() {
        return status == Status.MARKED_ROLLBACK;
    }

    @Override
    public synchronized boolean isCompleted() {
        return status != Status.ACTIVE && status != Status.MARKED_ROLLBACK;
    }

    @Override
    public void flush() {
        Runnable action = flushAction.get();
        if (action != null) {
            action.run();
            return;
        }
        if (isCompleted()) {
            throw new TransactionalDomainException("Cannot flush a completed transaction: " + definition.getName());
        }
    }

    /**
     * Returns the current lifecycle state of the described transaction.
     *
     * @return the current status
     */
    public synchronized Status getStatus() {
        return status;
    }

    /**
     * Moves the transaction into the given lifecycle state.
     *
     * <p>Called by the owning manager during begin, commit, and rollback.
     * Visibility is {@code protected} so subclasses of
     * {@link AbstractResourceTransactionManager} in other packages can reuse it.
     *
     * @param newStatus the state to transition into
     */
    protected synchronized void setStatus(Status newStatus) {
        if (newStatus == null) throw new TransactionalDomainException("Status cannot be null");
        this.status = newStatus;
    }
}
