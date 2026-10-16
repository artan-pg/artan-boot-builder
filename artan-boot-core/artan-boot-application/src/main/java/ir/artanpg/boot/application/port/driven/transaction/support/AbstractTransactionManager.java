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

import ir.artanpg.boot.application.port.driven.transaction.Status;
import ir.artanpg.boot.application.port.driven.transaction.TransactionCallback;
import ir.artanpg.boot.application.port.driven.transaction.TransactionDefinition;
import ir.artanpg.boot.application.port.driven.transaction.TransactionManager;
import ir.artanpg.boot.application.port.driven.transaction.TransactionPropagation;
import ir.artanpg.boot.application.port.driven.transaction.TransactionStatus;
import ir.artanpg.boot.application.port.driven.transaction.TransactionSynchronization;
import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Skeletal {@link TransactionManager} implementation that defines the common
 * transactional workflow and delegates backend-specific operations to
 * subclasses.
 *
 * <p>Lifecycle for a transaction that this manager <em>owns</em>
 * ({@code isNewTransaction() == true}):
 * <ol>
 *   <li>{@code beginTransaction}</li>
 *   <li>Bind {@link TransactionContext} and activate synchronizations</li>
 *   <li>Invoke the user callback</li>
 *   <li>{@code beforeCommit} (commit path only)</li>
 *   <li>{@code beforeCompletion}</li>
 *   <li>{@code commit} or {@code rollback}</li>
 *   <li>{@code afterCommit} (successful commit only)</li>
 *   <li>{@code afterCompletion} with an explicit completion status</li>
 *   <li>Clear thread-local state (and resume a suspended outer transaction if
 *       any)</li>
 * </ol>
 *
 * <p>Propagation handling (thread-local / context level):
 * <ul>
 *   <li>{@link TransactionPropagation#REQUIRED} – join existing or create
 *       new</li>
 *   <li>{@link TransactionPropagation#REQUIRES_NEW} – suspend existing
 *       (if any), create new</li>
 *   <li>{@link TransactionPropagation#MANDATORY} – require existing</li>
 *   <li>{@link TransactionPropagation#SUPPORTS} – join existing or run
 *       without a managed TX</li>
 *   <li>{@link TransactionPropagation#NOT_SUPPORTED} – suspend existing
 *       and run without a managed TX</li>
 *   <li>{@link TransactionPropagation#NEVER} – fail if a transaction is
 *       active</li>
 * </ul>
 * Resource-manager suspend/resume (JDBC connection, JTA, …) remains the
 * responsibility of concrete subclasses inside {@link #beginTransaction}.
 *
 * @author Mohammad Yazdian
 * @see TransactionManager
 * @see TransactionCallback
 * @see TransactionSynchronization
 * @since 0.1.0
 */
public abstract class AbstractTransactionManager implements TransactionManager {

    private static final ThreadLocal<Deque<SuspendedResources>> SUSPENDED_RESOURCES =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Constructs a new {@code AbstractTransactionManager}.
     */
    protected AbstractTransactionManager() {
        super();
    }

    @Override
    public <T> T executeInTransaction(@NonNull TransactionCallback<T> operation) throws TransactionalDomainException {
        return executeInTransaction(operation, TransactionDefinition.withDefaults());
    }

    @Override
    public <T> T executeInTransaction(@NonNull TransactionCallback<T> operation,
                                      @NonNull TransactionDefinition definition) throws TransactionalDomainException {
        if (operation == null) throw new TransactionalDomainException("The operation cannot be null");
        if (definition == null) throw new TransactionalDomainException("The definition cannot be null");

        TransactionPropagation propagation = definition.getPropagation();
        TransactionStatus existing = TransactionContext.getCurrentStatus();

        return switch (propagation) {
            case REQUIRED -> existing != null ?
                    participateInExistingTransaction(operation, existing) :
                    executeOwnedTransaction(operation, definition, false);

            case REQUIRES_NEW -> executeOwnedTransaction(operation, definition, true);

            case MANDATORY -> {
                if (existing == null) {
                    throw new TransactionalDomainException("No existing transaction found for propagation MANDATORY");
                }
                yield participateInExistingTransaction(operation, existing);
            }

            case SUPPORTS -> existing != null ?
                    participateInExistingTransaction(operation, existing) :
                    executeNonTransactional(operation);

            case NOT_SUPPORTED -> executeNotSupported(operation, existing != null);

            case NEVER -> {
                if (existing != null) {
                    throw new TransactionalDomainException("Existing transaction found for propagation NEVER");
                }
                yield executeNonTransactional(operation);
            }
        };
    }

    @Override
    public void registerSynchronization(@NonNull TransactionSynchronization synchronization) {
        if (synchronization == null) throw new TransactionalDomainException("The synchronization cannot be null");
        TransactionSynchronizationManager.registerSynchronization(synchronization);
    }

    /**
     * Starts a new transaction using the given definition.
     *
     * @param definition the transaction configuration
     * @return the status of the newly started transaction
     * @throws TransactionalDomainException if the transaction cannot be started
     */
    protected abstract TransactionStatus beginTransaction(@NonNull TransactionDefinition definition);

    /**
     * Commits the transaction associated with the given status.
     *
     * @param status the transaction status to commit
     * @throws TransactionalDomainException if the commit fails
     */
    protected abstract void commit(@NonNull TransactionStatus status);

    /**
     * Rolls back the transaction associated with the given status.
     *
     * @param status the transaction status to roll back
     * @throws TransactionalDomainException if the rollback fails
     */
    protected abstract void rollback(@NonNull TransactionStatus status);

    /**
     * Logs an error that occurred while invoking a synchronization callback.
     *
     * @param phase the lifecycle phase name
     * @param ex    the error that was caught
     */
    protected abstract void logSyncError(String phase, Exception ex);

    /**
     * Handles an exception by rolling back an active, incomplete transaction.
     *
     * @param status the transaction status, or {@code null} when never started
     * @param ex     the original exception
     */
    protected void handleTransactionException(@Nullable TransactionStatus status, @NonNull Exception ex) {
        if (status != null && !status.isCompleted()) {
            try {
                rollback(status);
            } catch (Exception exception) {
                logSyncError("handleTransactionException", exception);
            }
        }
    }

    /**
     * Releases thread-local transaction state after an owned transaction ends.
     *
     * <p>Does not invoke synchronizations; those are driven explicitly during
     * the commit/rollback path so that {@code beforeCompletion} runs
     * <em>before</em> the resource commit.
     *
     * @param resumeSuspended {@code true} to resume a previously suspended outer transaction
     */
    protected void cleanupAfterTransaction(boolean resumeSuspended) {
        TransactionSynchronizationManager.deactivateSynchronization();
        TransactionContext.clear();

        if (resumeSuspended) resumeSuspendedResources();
    }

    /**
     * Translates an exception after rollback (or non-transactional failure).
     *
     * <p>Application and domain failures are propagated unchanged so callers can
     * still catch concrete business exception types. Only unexpected checked
     * failures are wrapped as {@link TransactionalDomainException}.
     *
     * <ul>
     *   <li>{@link TransactionalDomainException} – returned as-is</li>
     *   <li>{@link DomainException} (and subclasses) – returned as-is</li>
     *   <li>other {@link RuntimeException} – returned as-is</li>
     *   <li>checked {@link Exception} – wrapped with the original as cause</li>
     * </ul>
     *
     * @param ex the exception raised during transactional (or non-transactional) work
     * @return a runtime exception to propagate to the caller
     */
    protected RuntimeException translateAfterRollback(Exception ex) {
        if (ex instanceof TransactionalDomainException transactional) return transactional;
        if (ex instanceof DomainException domain) return domain;
        if (ex instanceof RuntimeException runtime) return runtime;

        return new TransactionalDomainException("Transaction execution failed", ex);
    }

    // -------------------------------------------------------------------------
    // Owned / participating / non-transactional execution
    // -------------------------------------------------------------------------

    private <T> T participateInExistingTransaction(TransactionCallback<T> operation, TransactionStatus existing) {
        try {
            return operation.doInTransaction(existing);
        } catch (Exception ex) {
            existing.setRollbackOnly();
            throw translateAfterRollback(ex);
        }
    }

    private <T> T executeNonTransactional(TransactionCallback<T> operation) {
        try {
            // No managed transaction is bound; callers under SUPPORTS/NEVER without a TX
            // must not rely on transactional resource semantics.
            return operation.doInTransaction(NonTransactionalStatus.INSTANCE);
        } catch (Exception ex) {
            throw translateAfterRollback(ex);
        }
    }

    private <T> T executeNotSupported(TransactionCallback<T> operation, boolean suspendExisting) {
        if (suspendExisting) suspendCurrentResources();

        try {
            return executeNonTransactional(operation);
        } finally {
            if (suspendExisting) resumeSuspendedResources();
        }
    }

    private <T> T executeOwnedTransaction(TransactionCallback<T> operation,
                                          TransactionDefinition definition,
                                          boolean suspendExisting) {
        if (suspendExisting && TransactionContext.isTransactionActive()) suspendCurrentResources();

        CompletionTracker tracker = new CompletionTracker();
        TransactionStatus status = null;

        try {
            status = beginTransaction(definition);
            TransactionContext.setCurrentStatus(status);
            TransactionSynchronizationManager.activateSynchronization();

            T result = operation.doInTransaction(status);
            completeTransaction(status, tracker);
            return result;
        } catch (Exception ex) {
            handleExecutionFailure(status, ex, tracker);
            throw translateAfterRollback(ex);
        } finally {
            cleanupAfterTransaction(suspendExisting);
        }
    }

    /**
     * Drives the post-callback completion flow.
     *
     * <p>The transaction is rolled back when it has already been marked
     * rollback-only, or when {@code beforeCommit} marks it rollback-only as a
     * side effect. Otherwise, the commit path is taken.
     *
     * @param status  the current transaction status
     * @param tracker mutable tracker recording which phases have run
     */
    private void completeTransaction(TransactionStatus status, CompletionTracker tracker) {
        if (status.isRollbackOnly()) {
            rollbackAndTrack(status, tracker);
            return;
        }

        invokeBeforeCommit(status.isReadOnly());
        // beforeCommit may mark rollback-only
        if (status.isRollbackOnly()) {
            rollbackAndTrack(status, tracker);
            return;
        }

        commitAndTrack(status, tracker);
    }

    /**
     * Runs the rollback side of the completion flow, recording invoked phases.
     *
     * @param status  the current transaction status
     * @param tracker mutable tracker recording which phases have run
     */
    private void rollbackAndTrack(TransactionStatus status, CompletionTracker tracker) {
        invokeBeforeCompletion();
        tracker.beforeCompletionInvoked = true;
        rollback(status);
        invokeAfterCompletion(Status.ROLLED_BACK.getCode());
        tracker.afterCompletionInvoked = true;
    }

    /**
     * Runs the commit side of the completion flow, recording invoked phases.
     *
     * <p>The {@code committed} flag is set immediately after a successful commit
     * so that failures in {@code afterCommit}/{@code afterCompletion} are treated
     * as post-commit errors rather than triggering a rollback.
     *
     * @param status  the current transaction status
     * @param tracker mutable tracker recording which phases have run
     */
    private void commitAndTrack(TransactionStatus status, CompletionTracker tracker) {
        invokeBeforeCompletion();
        tracker.beforeCompletionInvoked = true;
        commit(status);
        tracker.committed = true;
        try {
            invokeAfterCommit();
        } finally {
            // Always run after a successful commit, even if afterCommit fails
            invokeAfterCompletion(Status.COMMITTED.getCode());
            tracker.afterCompletionInvoked = true;
        }
    }

    /**
     * Handles any exception thrown during the owned-transaction workflow.
     *
     * <p>When the commit has already succeeded, the failure is reported as a
     * post-commit synchronization error. Otherwise, the transaction is rolled back
     * and any synchronization phases that have not yet run are invoked.
     *
     * @param status  the transaction status, or {@code null} when never started
     * @param ex      the original exception
     * @param tracker tracker recording which phases have already run
     */
    private void handleExecutionFailure(@Nullable TransactionStatus status,
                                        Exception ex,
                                        CompletionTracker tracker) {
        if (tracker.committed) {
            logSyncError("postCommit", ex);
            // Defensive: if afterCompletion did not run (should be rare with the finally above)
            if (TransactionSynchronizationManager.getSynchronizationActive() && !tracker.afterCompletionInvoked) {
                invokeAfterCompletion(Status.COMMITTED.getCode());
                tracker.afterCompletionInvoked = true;
            }
            return;
        }

        handleTransactionException(status, ex);

        if (!TransactionSynchronizationManager.getSynchronizationActive()) return;

        if (!tracker.beforeCompletionInvoked) {
            invokeBeforeCompletion();
            tracker.beforeCompletionInvoked = true;
        }
        if (!tracker.afterCompletionInvoked) {
            invokeAfterCompletion(Status.ROLLED_BACK.getCode());
            tracker.afterCompletionInvoked = true;
        }
    }

    // -------------------------------------------------------------------------
    // Synchronization invocation (ordered)
    // -------------------------------------------------------------------------

    private void invokeBeforeCommit(boolean readOnly) {
        for (TransactionSynchronization sync : synchronizationsForBefore()) {
            try {
                sync.beforeCommit(readOnly);
            } catch (RuntimeException ex) {
                // beforeCommit failure must abort commit path — propagate
                throw ex;
            } catch (Exception ex) {
                throw new TransactionalDomainException("beforeCommit failed", ex);
            }
        }
    }

    private void invokeBeforeCompletion() {
        for (TransactionSynchronization sync : synchronizationsForBefore()) {
            try {
                sync.beforeCompletion();
            } catch (Exception ex) {
                logSyncError("beforeCompletion", ex);
            }
        }
    }

    private void invokeAfterCommit() {
        for (TransactionSynchronization sync : synchronizationsForAfter()) {
            try {
                sync.afterCommit();
            } catch (Exception ex) {
                logSyncError("afterCommit", ex);
            }
        }
    }

    private void invokeAfterCompletion(int completionStatus) {
        for (TransactionSynchronization sync : synchronizationsForAfter()) {
            try {
                sync.afterCompletion(completionStatus);
            } catch (Exception ex) {
                logSyncError("afterCompletion", ex);
            }
        }
    }

    private List<TransactionSynchronization> synchronizationsForBefore() {
        List<TransactionSynchronization> list =
                new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
        list.sort(Comparator.comparingInt(TransactionSynchronization::order));
        return list;
    }

    private List<TransactionSynchronization> synchronizationsForAfter() {
        List<TransactionSynchronization> list =
                new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
        list.sort(Comparator.comparingInt(TransactionSynchronization::order).reversed());
        return list;
    }

    // -------------------------------------------------------------------------
    // Suspend / resume (thread-local context only)
    // -------------------------------------------------------------------------

    private void suspendCurrentResources() {
        TransactionStatus status = TransactionContext.getCurrentStatus();
        boolean syncActive = TransactionSynchronizationManager.getSynchronizationActive();
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();

        SUSPENDED_RESOURCES.get().push(new SuspendedResources(status, syncActive, syncs));

        TransactionSynchronizationManager.deactivateSynchronization();
        TransactionContext.clear();
    }

    private void resumeSuspendedResources() {
        Deque<SuspendedResources> stack = SUSPENDED_RESOURCES.get();
        if (stack.isEmpty()) return;

        SuspendedResources suspended = stack.pop();
        if (stack.isEmpty()) SUSPENDED_RESOURCES.remove();
        if (suspended.status() != null) TransactionContext.setCurrentStatus(suspended.status());

        if (suspended.synchronizationActive()) {
            TransactionSynchronizationManager.activateSynchronization();
            for (TransactionSynchronization sync : suspended.synchronizations()) {
                TransactionSynchronizationManager.registerSynchronization(sync);
            }
        }
    }

    /**
     * Mutable state holder shared across the owned-transaction workflow. It lets
     * the completion flow and the failure handler communicate which phases have
     * already been executed without relying on deeply nested {@code try/catch}.
     */
    private static final class CompletionTracker {
        private boolean committed;
        private boolean beforeCompletionInvoked;
        private boolean afterCompletionInvoked;
    }

    private record SuspendedResources(@Nullable TransactionStatus status,
                                      boolean synchronizationActive,
                                      List<TransactionSynchronization> synchronizations) {
    }

    private static final class NonTransactionalStatus implements TransactionStatus {

        private static final NonTransactionalStatus INSTANCE = new NonTransactionalStatus();

        private boolean rollbackOnly;

        @Override
        public String getTransactionName() {
            return "non-transactional";
        }

        @Override
        public boolean isNewTransaction() {
            return false;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public void setRollbackOnly() {
            this.rollbackOnly = true;
        }

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public boolean isCompleted() {
            return false;
        }

        @Override
        public void flush() {
            // no-op
        }
    }
}
