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
import ir.artanpg.boot.application.port.driven.transaction.support.TransactionContext;
import ir.artanpg.boot.application.port.driven.transaction.support.TransactionSynchronization;
import ir.artanpg.boot.application.port.driven.transaction.support.TransactionSynchronizationManager;
import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link InMemoryTransactionManager}.
 *
 * <p>All scenarios run against a single shared manager instance: the
 * {@link RecordingInMemoryTransactionManager} test double, which extends the
 * production {@link InMemoryTransactionManager} only to expose the protected
 * workflow hooks (commit / rollback / isolation validation) and to count
 * resource-level callbacks.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("InMemoryTransactionManager")
class InMemoryTransactionManagerTests {

    private final RecordingInMemoryTransactionManager manager = new RecordingInMemoryTransactionManager();

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.deactivateSynchronization();
        TransactionContext.clear();
        manager.reset();
    }

    @Nested
    @DisplayName("executeInTransaction(TransactionCallback)")
    class ExecuteInTransaction {

        @Test
        @DisplayName("Records begin and commit lifecycle events on success")
        void executeInTransaction_ShouldRecordBeginAndCommit_WhenCallbackSucceeds() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder().name("create-order").build();

            // when
            String result = manager.executeInTransaction(_ -> "created", definition);

            // then
            then(result)
                    .as("callback result should be returned")
                    .isEqualTo("created");
            then(manager.getLifecycleEvents())
                    .as("success path should journal begin then commit")
                    .containsExactly("begin:create-order", "commit:create-order");
        }

        @Test
        @DisplayName("Records begin and rollback when status is rollback-only")
        void executeInTransaction_ShouldRecordRollback_WhenStatusIsRollbackOnly() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder().name("cancel-order").build();

            // when
            manager.executeInTransaction(status -> {
                status.setRollbackOnly();
                return null;
            }, definition);

            // then
            then(manager.getLifecycleEvents())
                    .as("rollback-only path should journal begin then rollback")
                    .containsExactly("begin:cancel-order", "rollback:cancel-order");
        }

        @Test
        @DisplayName("Records rollback and propagates DomainException")
        void executeInTransaction_ShouldRecordRollback_WhenCallbackThrowsDomainException() {
            // given
            DomainException failure = new DomainException("invariant violated");
            TransactionDefinition definition = TransactionDefinition.builder()
                    .name("pay-order")
                    .build();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                throw failure;
            }, definition))
                    .as("DomainException must propagate unchanged")
                    .isSameAs(failure);
            then(manager.getLifecycleEvents())
                    .as("failure path should journal begin then rollback")
                    .containsExactly("begin:pay-order", "rollback:pay-order");
        }

        @Test
        @DisplayName("Clears thread context after completion")
        void executeInTransaction_ShouldClearThreadContext_WhenFinished() {
            // given

            // when
            manager.executeInTransaction(_ -> "ok");

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("TransactionContext must be cleared after completion")
                    .isNull();
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization must be deactivated after completion")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("executeNamed(String, TransactionCallback, boolean)")
    class ExecuteNamed {

        @Test
        @DisplayName("Uses the operation name as the transaction name")
        void executeNamed_ShouldUseOperationName_WhenInvoked() {
            // given

            // when
            manager.executeNamed("ship-order", _ -> "shipped", false);

            // then
            then(manager.getInvokedOperations())
                    .as("operation name should be journaled")
                    .containsExactly("ship-order");
            then(manager.getLifecycleEvents())
                    .as("lifecycle events should use the operation name")
                    .contains("begin:ship-order", "commit:ship-order");
        }

        @Test
        @DisplayName("Marks the transaction read-only when requested")
        void executeNamed_ShouldMarkReadOnly_WhenReadOnlyFlagIsTrue() {
            // given

            // when
            Boolean readOnly = manager.executeNamed("query-order", TransactionStatus::isReadOnly, true);

            // then
            then(readOnly)
                    .as("transaction should be flagged read-only")
                    .isTrue();
        }

        @Test
        @DisplayName("Throws when operation is null")
        void executeNamed_ShouldThrowTransactionalDomainException_WhenOperationIsNull() {
            // given

            // when & then
            thenThrownBy(() -> manager.executeNamed("op", null, false))
                    .as("null operation must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("operation");
        }

        @Test
        @DisplayName("Throws when operation name is null")
        void executeNamed_ShouldThrowTransactionalDomainException_WhenNameIsNull() {
            // given

            // when & then
            thenThrownBy(() -> manager.executeNamed(null, _ -> "x", false))
                    .as("null operation name must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("operation name");
        }

        @Test
        @DisplayName("Throws when operation name is blank")
        void executeNamed_ShouldThrowTransactionalDomainException_WhenNameIsBlank() {
            // given

            // when & then
            thenThrownBy(() -> manager.executeNamed("   ", _ -> "x", false))
                    .as("blank operation name must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("operation name");
        }

        @Test
        @DisplayName("Throws when operation name is empty")
        void executeNamed_ShouldThrowTransactionalDomainException_WhenNameIsEmpty() {
            // given

            // when & then
            thenThrownBy(() -> manager.executeNamed("", _ -> "x", false))
                    .as("empty operation name must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("operation name");
        }
    }

    @Nested
    @DisplayName("flush journaling")
    class FlushJournaling {

        @Test
        @DisplayName("Records flush events when status.flush is called")
        void flush_ShouldRecordFlushEvent_WhenStatusFlushIsCalled() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder().name("flush-demo").build();

            // when
            manager.executeInTransaction(status -> {
                status.flush();
                return null;
            }, definition);

            // then
            then(manager.getLifecycleEvents())
                    .as("flush should appear between begin and commit")
                    .containsExactly("begin:flush-demo", "flush:flush-demo", "commit:flush-demo");
            then(manager.flushCount.get())
                    .as("doFlush should have been invoked once")
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("synchronization error journaling")
    class SyncErrorJournaling {

        @Test
        @DisplayName("Records error phase when a synchronization callback fails")
        void executeInTransaction_ShouldRecordErrorPhase_WhenSynchronizationThrows() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder().name("sync-error").build();
            TransactionSynchronization failing = new TransactionSynchronization() {
                @Override
                public void beforeCompletion() {
                    throw new RuntimeException("beforeCompletion failed");
                }
            };

            // when
            manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(failing);
                return null;
            }, definition);

            // then
            then(manager.getLifecycleEvents())
                    .as("sync failure should be journaled as error:<phase>")
                    .contains("error:beforeCompletion");
            then(manager.getLifecycleEvents())
                    .as("transaction should still commit after a logged sync error in beforeCompletion")
                    .contains("commit:sync-error");
        }
    }

    @Nested
    @DisplayName("getStatus()")
    class GetStatus {

        @Test
        @DisplayName("Returns NO_TRANSACTION placeholder when no transaction is active")
        void getStatus_ShouldReturnNoTransactionPlaceholder_WhenNoActiveTransaction() {
            // given

            // when
            TransactionStatus status = manager.getStatus();

            // then
            then(status)
                    .as("getStatus must never return null")
                    .isNotNull();
            then(status)
                    .as("placeholder must be a SimpleTransactionStatus")
                    .isInstanceOf(SimpleTransactionStatus.class);
            then(((SimpleTransactionStatus) status).getStatus())
                    .as("placeholder lifecycle state should be NO_TRANSACTION")
                    .isEqualTo(Status.NO_TRANSACTION);
            then(status.isCompleted())
                    .as("NO_TRANSACTION placeholder should report completed")
                    .isTrue();
            then(status.isNewTransaction())
                    .as("NO_TRANSACTION placeholder is not a new transaction")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns the bound status while a transaction is active")
        void getStatus_ShouldReturnBoundStatus_WhenTransactionIsActive() {
            // given

            // when
            TransactionStatus seen = manager.executeInTransaction(_ -> manager.getStatus());

            // then
            then(seen)
                    .as("active status should be visible through getStatus")
                    .isInstanceOf(SimpleTransactionStatus.class);
            then(seen.isNewTransaction())
                    .as("owned transaction should be marked new")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("reset()")
    class Reset {

        @Test
        @DisplayName("Clears lifecycle events and invoked operations")
        void reset_ShouldClearJournals_WhenCalled() {
            // given
            manager.executeNamed("op-1", _ -> null, false);
            then(manager.getLifecycleEvents()).as("precondition: journal not empty").isNotEmpty();
            then(manager.getInvokedOperations()).as("precondition: operations not empty").isNotEmpty();

            // when
            manager.reset();

            // then
            then(manager.getLifecycleEvents())
                    .as("lifecycle journal should be empty after reset")
                    .isEmpty();
            then(manager.getInvokedOperations())
                    .as("operations journal should be empty after reset")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("begin / commit / rollback guards")
    class BeginCommitRollback {

        @Test
        @DisplayName("Begins with ACTIVE status and invokes doBegin")
        void executeInTransaction_ShouldBeginWithActiveStatus_WhenCallbackRuns() {
            // given

            // when
            manager.executeInTransaction(status -> {
                then(status)
                        .as("status must be SimpleTransactionStatus")
                        .isInstanceOf(SimpleTransactionStatus.class);
                then(((SimpleTransactionStatus) status).getStatus())
                        .as("status should be ACTIVE during the callback")
                        .isEqualTo(Status.ACTIVE);
                then(manager.beginCount.get())
                        .as("doBegin should have been invoked")
                        .isEqualTo(1);
                return null;
            });

            // then
            then(manager.beginCount.get())
                    .as("doBegin runs once per owned transaction")
                    .isEqualTo(1);
            then(manager.committedNames)
                    .as("successful path should record a commit")
                    .hasSize(1);
        }

        @Test
        @DisplayName("Commits and marks status COMMITTED on success")
        void executeInTransaction_ShouldCommit_WhenCallbackSucceeds() {
            // given

            // when
            String result = manager.executeInTransaction(_ -> "ok");

            // then
            then(result)
                    .as("callback result should be returned")
                    .isEqualTo("ok");
            then(manager.committedNames)
                    .as("onCommitted should run for the success path")
                    .isNotEmpty();
            then(manager.rolledBackNames)
                    .as("rollback must not run on the success path")
                    .isEmpty();
        }

        @Test
        @DisplayName("Rolls back when the status is marked rollback-only")
        void executeInTransaction_ShouldRollback_WhenStatusIsRollbackOnly() {
            // given

            // when
            manager.executeInTransaction(status -> {
                status.setRollbackOnly();
                return null;
            });

            // then
            then(manager.rolledBackNames)
                    .as("rollback-only must end with rollback")
                    .isNotEmpty();
            then(manager.committedNames)
                    .as("commit must not run for rollback-only")
                    .isEmpty();
        }

        @Test
        @DisplayName("Rolls back and propagates DomainException unchanged")
        void executeInTransaction_ShouldPropagateDomainException_WhenCallbackFails() {
            // given
            DomainException failure = new DomainException("business rule");

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                throw failure;
            }))
                    .as("DomainException must not be wrapped")
                    .isSameAs(failure);
            then(manager.rolledBackNames)
                    .as("callback failure should trigger rollback")
                    .isNotEmpty();
        }

        @Test
        @DisplayName("Rejects commit when status is already completed")
        void commit_ShouldThrowTransactionalDomainException_WhenAlreadyCompleted() {
            // given
            CallSetStatusSimpleTransactionStatus status = new CallSetStatusSimpleTransactionStatus(
                    TransactionDefinition.withDefaults(), true, null);
            status.setStatus(Status.COMMITTED);

            // when & then
            thenThrownBy(() -> manager.exposeCommit(status))
                    .as("second commit must fail")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        @DisplayName("Rejects commit when status is rollback-only")
        void commit_ShouldThrowTransactionalDomainException_WhenRollbackOnly() {
            // given
            SimpleTransactionStatus status = new SimpleTransactionStatus(
                    TransactionDefinition.withDefaults(), true, null);
            status.setRollbackOnly();

            // when & then
            thenThrownBy(() -> manager.exposeCommit(status))
                    .as("commit of rollback-only status must fail")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("rollback-only");
        }

        @Test
        @DisplayName("Rejects rollback when status is already completed")
        void rollback_ShouldThrowTransactionalDomainException_WhenAlreadyCompleted() {
            // given
            CallSetStatusSimpleTransactionStatus status = new CallSetStatusSimpleTransactionStatus(
                    TransactionDefinition.withDefaults(), true, null);
            status.setStatus(Status.ROLLED_BACK);

            // when & then
            thenThrownBy(() -> manager.exposeRollback(status))
                    .as("second rollback must fail")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        @DisplayName("Rejects foreign TransactionStatus types")
        void commit_ShouldThrowTransactionalDomainException_WhenStatusIsForeign() {
            // given
            TransactionStatus foreign = new ForeignTransactionStatus();

            // when & then
            thenThrownBy(() -> manager.exposeCommit(foreign))
                    .as("foreign status types must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("not managed");
        }
    }

    @Nested
    @DisplayName("validateIsolationLevel(TransactionIsolation)")
    class ValidateIsolationLevel {

        @Test
        @DisplayName("Accepts a non-null isolation level")
        void validateIsolationLevel_ShouldAccept_WhenIsolationIsNonNull() {
            // given

            // when & then
            manager.exposeValidateIsolationLevel(TransactionIsolation.READ_COMMITTED);
            manager.exposeValidateIsolationLevel(TransactionIsolation.DEFAULT);
        }

        @Test
        @DisplayName("Throws when isolation level is null")
        void validateIsolationLevel_ShouldThrowTransactionalDomainException_WhenIsolationIsNull() {
            // given

            // when & then
            thenThrownBy(() -> manager.exposeValidateIsolationLevel(null))
                    .as("null isolation must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("Isolation");
        }

        @Test
        @DisplayName("Is invoked during begin so a subclass can reject unsupported levels")
        void validateIsolationLevel_ShouldRejectUnsupportedLevel_WhenSubclassOverrides() {
            // given
            manager.rejectedIsolation = TransactionIsolation.READ_UNCOMMITTED;
            TransactionDefinition definition = TransactionDefinition.builder()
                    .name("unsupported-isolation")
                    .isolation(TransactionIsolation.READ_UNCOMMITTED)
                    .build();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> "never", definition))
                    .as("subclass validation failure must surface on begin")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("READ_UNCOMMITTED");
        }
    }

    @Nested
    @DisplayName("flushManaged(SimpleTransactionStatus)")
    class FlushManaged {

        @Test
        @DisplayName("Invokes doFlush when status.flush is called on an active transaction")
        void flush_ShouldInvokeDoFlush_WhenStatusIsActive() {
            // given

            // when
            manager.executeInTransaction(status -> {
                status.flush();
                return null;
            });

            // then
            then(manager.flushCount.get())
                    .as("status.flush should reach doFlush via flushManaged")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Invokes doFlush once per status.flush call")
        void flush_ShouldInvokeDoFlushOncePerCall_WhenFlushedMultipleTimes() {
            // given

            // when
            manager.executeInTransaction(status -> {
                status.flush();
                status.flush();
                status.flush();
                return null;
            });

            // then
            then(manager.flushCount.get())
                    .as("each status.flush should reach doFlush exactly once")
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("Throws when status.flush is called after the transaction has committed")
        void flush_ShouldThrowTransactionalDomainException_WhenTransactionAlreadyCommitted() {
            // given
            AtomicReference<TransactionStatus> held = new AtomicReference<>();
            manager.executeInTransaction(status -> {
                held.set(status);
                return null;
            });

            // when & then
            then(held.get().isCompleted())
                    .as("precondition: status must be completed after commit")
                    .isTrue();
            thenThrownBy(() -> held.get().flush())
                    .as("flush after commit must fail")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("Cannot flush");
            then(manager.flushCount.get())
                    .as("doFlush must not run after the transaction completed")
                    .isZero();
        }

        @Test
        @DisplayName("Throws when status.flush is called after the transaction has rolled back")
        void flush_ShouldThrowTransactionalDomainException_WhenTransactionAlreadyRolledBack() {
            // given
            AtomicReference<TransactionStatus> held = new AtomicReference<>();
            manager.executeInTransaction(status -> {
                held.set(status);
                status.setRollbackOnly();
                return null;
            });

            // when & then
            then(held.get().isCompleted())
                    .as("precondition: status must be completed after rollback")
                    .isTrue();
            thenThrownBy(() -> held.get().flush())
                    .as("flush after rollback must fail")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("Cannot flush");
            then(manager.flushCount.get())
                    .as("doFlush must not run after the transaction completed")
                    .isZero();
        }
    }

    @Nested
    @DisplayName("begin failure")
    class BeginFailure {

        @Test
        @DisplayName("Wraps RuntimeException from doBegin")
        void executeInTransaction_ShouldWrap_WhenDoBeginThrows() {
            // given
            manager.failOnBegin = true;

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> "never"))
                    .as("doBegin failure should become TransactionalDomainException")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("Unable to begin")
                    .hasCauseInstanceOf(IllegalStateException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers and test doubles
    // -------------------------------------------------------------------------

    /**
     * The single manager used by every scenario in this class. It extends the
     * production {@link InMemoryTransactionManager} without changing its
     * behavior, adding only:
     * <ul>
     *   <li>counters and name journals for the resource hooks
     *       ({@code doBegin}, {@code doFlush}, {@code onCommitted},
     *       {@code onRolledBack})</li>
     *   <li>switches to simulate failures ({@code failOnBegin},
     *       {@code rejectedIsolation})</li>
     *   <li>exposure of the protected {@code commit} / {@code rollback} /
     *       {@code validateIsolationLevel} entry points</li>
     * </ul>
     */
    private static final class RecordingInMemoryTransactionManager extends InMemoryTransactionManager {

        private final AtomicInteger beginCount = new AtomicInteger();
        private final AtomicInteger flushCount = new AtomicInteger();
        private final List<String> committedNames = new ArrayList<>();
        private final List<String> rolledBackNames = new ArrayList<>();
        private boolean failOnBegin;
        private TransactionIsolation rejectedIsolation;

        @Override
        protected void doBegin(TransactionDefinition definition, SimpleTransactionStatus status) {
            if (failOnBegin) throw new IllegalStateException("resource unavailable");
            beginCount.incrementAndGet();
        }

        @Override
        protected void doFlush(SimpleTransactionStatus status) {
            super.doFlush(status);
            flushCount.incrementAndGet();
        }

        @Override
        protected void onCommitted(SimpleTransactionStatus status) {
            super.onCommitted(status);
            committedNames.add(status.getTransactionName());
        }

        @Override
        protected void onRolledBack(SimpleTransactionStatus status) {
            super.onRolledBack(status);
            rolledBackNames.add(status.getTransactionName());
        }

        @Override
        protected void validateIsolationLevel(TransactionIsolation isolation) {
            super.validateIsolationLevel(isolation);
            if (rejectedIsolation != null && rejectedIsolation == isolation) {
                throw new TransactionalDomainException("Isolation level " + isolation + " is not supported");
            }
        }

        private void exposeCommit(TransactionStatus status) {
            commit(status);
        }

        private void exposeRollback(TransactionStatus status) {
            rollback(status);
        }

        private void exposeValidateIsolationLevel(TransactionIsolation isolation) {
            validateIsolationLevel(isolation);
        }

        @Override
        public void reset() {
            super.reset();
            beginCount.set(0);
            flushCount.set(0);
            committedNames.clear();
            rolledBackNames.clear();
            failOnBegin = false;
            rejectedIsolation = null;
        }
    }

    private static final class ForeignTransactionStatus implements TransactionStatus {

        @Override
        public String getTransactionName() {
            return "foreign";
        }

        @Override
        public boolean isNewTransaction() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public void setRollbackOnly() {
            // no-op
        }

        @Override
        public boolean isRollbackOnly() {
            return false;
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

    private static final class CallSetStatusSimpleTransactionStatus extends SimpleTransactionStatus {

        public CallSetStatusSimpleTransactionStatus(@NonNull TransactionDefinition definition,
                                                    boolean newTransaction,
                                                    @Nullable Runnable flushAction) {
            super(definition, newTransaction, flushAction);
        }

        @Override
        protected synchronized void setStatus(Status newStatus) {
            super.setStatus(newStatus);
        }
    }
}
