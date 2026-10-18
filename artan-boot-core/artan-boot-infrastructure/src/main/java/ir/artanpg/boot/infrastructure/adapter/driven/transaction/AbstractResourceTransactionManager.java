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
import ir.artanpg.boot.application.port.driven.transaction.TransactionManager;
import ir.artanpg.boot.application.port.driven.transaction.TransactionStatus;
import ir.artanpg.boot.application.port.driven.transaction.support.AbstractTransactionManager;
import ir.artanpg.boot.application.port.driven.transaction.support.TransactionContext;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.jspecify.annotations.NonNull;

/**
 * Base class for resource-aware {@link TransactionManager} adapters.
 *
 * <p>Builds on {@link AbstractTransactionManager} for propagation,
 * synchronization lifecycle, and exception translation. Subclasses bind a
 * concrete resource in {@link #doBegin} / {@link #doFlush} while this class
 * owns {@link SimpleTransactionStatus} bookkeeping and commit/rollback guards.
 *
 * @author Mohammad Yazdian
 * @see InMemoryTransactionManager
 * @see SimpleTransactionStatus
 * @since 0.1.0
 */
public abstract class AbstractResourceTransactionManager extends AbstractTransactionManager {

    private static final String TRANSACTION_COMPLETED_EXCEPTION_MSG = "Transaction '%s' is already completed";

    /**
     * Constructs a new resource transaction manager.
     */
    protected AbstractResourceTransactionManager() {
        super();
    }

    /**
     * Returns the {@link TransactionStatus} bound to the calling thread.
     *
     * <p>When no transaction is active, a completed placeholder describing
     * {@link Status#NO_TRANSACTION} is returned instead of {@code null}.
     *
     * @return the current transaction status
     */
    @Override
    public TransactionStatus getStatus() {
        TransactionStatus current = TransactionContext.getCurrentStatus();
        return current != null ? current : noneStatus();
    }

    /**
     * Begins a new transaction described by the given definition.
     *
     * @param definition the transaction configuration
     * @return the status of the newly started transaction
     * @throws TransactionalDomainException if the transaction cannot be started
     */
    @Override
    protected TransactionStatus beginTransaction(@NonNull TransactionDefinition definition) {
        validateIsolationLevel(definition.getIsolation());

        SimpleTransactionStatus status = new SimpleTransactionStatus(definition, true, null);
        status.bindFlushAction(() -> flushManaged(status));

        try {
            doBegin(definition, status);
        } catch (RuntimeException ex) {
            throw new TransactionalDomainException(
                    String.format("Unable to begin transaction '%s'", definition.getName()), ex);
        }

        status.setStatus(Status.ACTIVE);
        onBegun(status);
        return status;
    }

    /**
     * Commits the transaction described by the given status.
     *
     * @param status the transaction status to commit
     * @throws TransactionalDomainException if the status is foreign, completed, or rollback-only
     */
    @Override
    protected void commit(@NonNull TransactionStatus status) {
        SimpleTransactionStatus simpleStatus = requireManagedStatus(status);

        if (simpleStatus.isCompleted()) {
            throw new TransactionalDomainException(
                    String.format(TRANSACTION_COMPLETED_EXCEPTION_MSG, status.getTransactionName()));
        }
        if (simpleStatus.isRollbackOnly()) {
            throw new TransactionalDomainException(
                    String.format("Cannot commit rollback-only transaction '%s'", status.getTransactionName()));
        }

        simpleStatus.setStatus(Status.COMMITTED);
        onCommitted(simpleStatus);
    }

    /**
     * Rolls back the transaction described by the given status.
     *
     * @param status the transaction status to roll back
     * @throws TransactionalDomainException if the status is foreign or already completed
     */
    @Override
    protected void rollback(@NonNull TransactionStatus status) {
        SimpleTransactionStatus simpleStatus = requireManagedStatus(status);

        if (simpleStatus.isCompleted()) {
            throw new TransactionalDomainException(
                    String.format(TRANSACTION_COMPLETED_EXCEPTION_MSG, status.getTransactionName()));
        }

        simpleStatus.setStatus(Status.ROLLED_BACK);
        onRolledBack(simpleStatus);
    }

    // -------------------------------------------------------------------------
    // Resource extension points
    // -------------------------------------------------------------------------

    /**
     * Binds the underlying resource when a transaction begins.
     *
     * @param definition the transaction configuration
     * @param status     the status describing the transaction being begun
     */
    protected abstract void doBegin(TransactionDefinition definition, SimpleTransactionStatus status);

    /**
     * Pushes buffered changes of a transaction to the underlying resource
     * without ending the transaction.
     *
     * <p>Invoked only through {@link SimpleTransactionStatus#flush()} so there is a
     * single flush path for both application code and resource adapters.
     *
     * @param status the status of the transaction to flush
     */
    protected abstract void doFlush(SimpleTransactionStatus status);

    /**
     * Validates that the isolation level can be honored.
     *
     * <p>The baseline accepts every non-null {@link TransactionIsolation} value.
     * Subclasses override this method to reject levels their resource cannot support.
     *
     * @param isolation the isolation level to validate
     * @throws TransactionalDomainException if the isolation level is null or unsupported
     */
    protected void validateIsolationLevel(TransactionIsolation isolation) {
        if (isolation == null) throw new TransactionalDomainException("Isolation level must not be null");
    }

    /**
     * Called after a transaction has been begun and marked {@link Status#ACTIVE}.
     *
     * @param status the new transaction status
     */
    protected void onBegun(SimpleTransactionStatus status) {
        // default no-op
    }

    /**
     * Called after a successful commit transition.
     *
     * @param status the committed status
     */
    protected void onCommitted(SimpleTransactionStatus status) {
        // default no-op
    }

    /**
     * Called after a successful rollback transition.
     *
     * @param status the rolled-back status
     */
    protected void onRolledBack(SimpleTransactionStatus status) {
        // default no-op
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Narrows a generic status to the concrete type managed by this family.
     *
     * @param status the status to cast
     * @return the same status as a {@link SimpleTransactionStatus}
     * @throws TransactionalDomainException if the status was not produced by this manager family
     */
    protected final SimpleTransactionStatus requireManagedStatus(TransactionStatus status) {
        if (status instanceof SimpleTransactionStatus simpleStatus) return simpleStatus;

        throw new TransactionalDomainException(
                String.format("Status %s is not managed by this transaction manager", status.getClass().getName()));
    }

    private void flushManaged(SimpleTransactionStatus status) {
        if (status.isCompleted()) {
            throw new TransactionalDomainException(
                    "Cannot flush a completed transaction: " + status.getTransactionName());
        }
        doFlush(status);
    }

    private static TransactionStatus noneStatus() {
        SimpleTransactionStatus none = new SimpleTransactionStatus(TransactionDefinition.withDefaults(), false, null);
        none.setStatus(Status.NO_TRANSACTION);
        return none;
    }
}
