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
 * Unit tests for {@link AbstractResourceTransactionManager}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("AbstractResourceTransactionManager")
class ResourceTransactionManagerTests {

    private final TestableResourceTransactionManager manager = new TestableResourceTransactionManager();

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.deactivateSynchronization();
        TransactionContext.clear();
        manager.reset();
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
            RestrictiveResourceTransactionManager restrictive = new RestrictiveResourceTransactionManager();
            TransactionDefinition definition = TransactionDefinition.builder()
                    .name("unsupported-isolation")
                    .isolation(TransactionIsolation.READ_UNCOMMITTED)
                    .build();

            // when & then
            thenThrownBy(() -> restrictive.executeInTransaction(_ -> "never", definition))
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
            AtomicReference<TransactionStatus> held =
                    new AtomicReference<>();
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

    @Nested
    @DisplayName("onCommitted(SimpleTransactionStatus)")
    class OnCommitted {

        @Test
        @DisplayName("should record the transaction name when invoked")
        void onCommitted_ShouldRecordTransactionName_WhenInvoked() {
            // given
            RecordingResourceTransactionManager manager = new RecordingResourceTransactionManager();
            SimpleTransactionStatus status = new SimpleTransactionStatus(
                    TransactionDefinition.builder().name("commit-tx").build(), true, null);

            // when
            manager.exposeOnCommitted(status);

            // then
            then(manager.getCommittedTransactions())
                    .as("onCommitted should record the transaction name")
                    .containsExactly("commit-tx");
            then(manager.getOnCommittedCallCount())
                    .as("onCommitted should be invoked exactly once")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should accumulate every invocation in registration order")
        void onCommitted_ShouldAccumulateInvocations_WhenCalledMultipleTimes() {
            // given
            RecordingResourceTransactionManager manager = new RecordingResourceTransactionManager();
            SimpleTransactionStatus first = new SimpleTransactionStatus(
                    TransactionDefinition.builder().name("tx-1").build(), true, null);
            SimpleTransactionStatus second = new SimpleTransactionStatus(
                    TransactionDefinition.builder().name("tx-2").build(), true, null);

            // when
            manager.exposeOnCommitted(first);
            manager.exposeOnCommitted(second);

            // then
            then(manager.getCommittedTransactions())
                    .as("onCommitted should accumulate names in invocation order")
                    .containsExactly("tx-1", "tx-2");
            then(manager.getOnCommittedCallCount())
                    .as("onCommitted should be invoked twice")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("should leave rolled-back records untouched when invoked")
        void onCommitted_ShouldLeaveRolledBackRecordsUntouched_WhenInvoked() {
            // given
            RecordingResourceTransactionManager manager = new RecordingResourceTransactionManager();
            SimpleTransactionStatus status = new SimpleTransactionStatus(
                    TransactionDefinition.builder().name("commit-only-tx").build(), true, null);

            // when
            manager.exposeOnCommitted(status);

            // then
            then(manager.getRolledBackTransactions())
                    .as("onCommitted must not record anything as rolled back")
                    .isEmpty();
            then(manager.getOnRolledBackCallCount())
                    .as("onRolledBack must not be triggered by onCommitted")
                    .isZero();
        }
    }

    @Nested
    @DisplayName("onRolledBack(SimpleTransactionStatus)")
    class OnRolledBack {

        @Test
        @DisplayName("should record the transaction name when invoked")
        void onRolledBack_ShouldRecordTransactionName_WhenInvoked() {
            // given
            RecordingResourceTransactionManager manager = new RecordingResourceTransactionManager();
            SimpleTransactionStatus status = new SimpleTransactionStatus(
                    TransactionDefinition.builder().name("rollback-tx").build(), true, null);

            // when
            manager.exposeOnRolledBack(status);

            // then
            then(manager.getRolledBackTransactions())
                    .as("onRolledBack should record the transaction name")
                    .containsExactly("rollback-tx");
            then(manager.getOnRolledBackCallCount())
                    .as("onRolledBack should be invoked exactly once")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should accumulate every invocation in registration order")
        void onRolledBack_ShouldAccumulateInvocations_WhenCalledMultipleTimes() {
            // given
            RecordingResourceTransactionManager manager = new RecordingResourceTransactionManager();
            SimpleTransactionStatus first = new SimpleTransactionStatus(
                    TransactionDefinition.builder().name("tx-1").build(), true, null);
            SimpleTransactionStatus second = new SimpleTransactionStatus(
                    TransactionDefinition.builder().name("tx-2").build(), true, null);

            // when
            manager.exposeOnRolledBack(first);
            manager.exposeOnRolledBack(second);

            // then
            then(manager.getRolledBackTransactions())
                    .as("onRolledBack should accumulate names in invocation order")
                    .containsExactly("tx-1", "tx-2");
            then(manager.getOnRolledBackCallCount())
                    .as("onRolledBack should be invoked twice")
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("should leave committed records untouched when invoked")
        void onRolledBack_ShouldLeaveCommittedRecordsUntouched_WhenInvoked() {
            // given
            RecordingResourceTransactionManager manager = new RecordingResourceTransactionManager();
            SimpleTransactionStatus status = new SimpleTransactionStatus(
                    TransactionDefinition.builder().name("rollback-only-tx").build(), true, null);

            // when
            manager.exposeOnRolledBack(status);

            // then
            then(manager.getCommittedTransactions())
                    .as("onRolledBack must not record anything as committed")
                    .isEmpty();
            then(manager.getOnCommittedCallCount())
                    .as("onCommitted must not be triggered by onRolledBack")
                    .isZero();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers and test doubles
    // -------------------------------------------------------------------------

    private static final class TestableResourceTransactionManager extends AbstractResourceTransactionManager {

        private final AtomicInteger beginCount = new AtomicInteger();
        private final AtomicInteger flushCount = new AtomicInteger();
        private final List<String> committedNames = new ArrayList<>();
        private final List<String> rolledBackNames = new ArrayList<>();
        private boolean failOnBegin;

        @Override
        protected void doBegin(TransactionDefinition definition, SimpleTransactionStatus status) {
            if (failOnBegin) {
                throw new IllegalStateException("resource unavailable");
            }
            beginCount.incrementAndGet();
        }

        @Override
        protected void doFlush(SimpleTransactionStatus status) {
            flushCount.incrementAndGet();
        }

        @Override
        protected void onCommitted(SimpleTransactionStatus status) {
            committedNames.add(status.getTransactionName());
        }

        @Override
        protected void onRolledBack(SimpleTransactionStatus status) {
            rolledBackNames.add(status.getTransactionName());
        }

        @Override
        protected void logSyncError(String phase, Exception ex) {
            // no-op for unit tests
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

        private void reset() {
            beginCount.set(0);
            flushCount.set(0);
            committedNames.clear();
            rolledBackNames.clear();
            failOnBegin = false;
        }
    }

    private static final class RestrictiveResourceTransactionManager extends AbstractResourceTransactionManager {

        @Override
        protected void doBegin(TransactionDefinition definition, SimpleTransactionStatus status) {
            // no-op
        }

        @Override
        protected void doFlush(SimpleTransactionStatus status) {
            // no-op
        }

        @Override
        protected void validateIsolationLevel(TransactionIsolation isolation) {
            super.validateIsolationLevel(isolation);
            if (isolation == TransactionIsolation.READ_UNCOMMITTED) {
                throw new TransactionalDomainException(
                        "Isolation level READ_UNCOMMITTED is not supported");
            }
        }

        @Override
        protected void logSyncError(String phase, Exception ex) {
            // no-op
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

    private static final class RecordingResourceTransactionManager extends AbstractResourceTransactionManager {

        private final List<String> committedTransactions = new ArrayList<>();
        private final List<String> rolledBackTransactions = new ArrayList<>();
        private final AtomicInteger onCommittedCallCount = new AtomicInteger();
        private final AtomicInteger onRolledBackCallCount = new AtomicInteger();

        @Override
        protected void doBegin(TransactionDefinition definition, SimpleTransactionStatus status) {
            // no-op for testing
        }

        @Override
        protected void doFlush(SimpleTransactionStatus status) {
            // no-op for testing
        }

        @Override
        protected void onCommitted(SimpleTransactionStatus status) {
            onCommittedCallCount.incrementAndGet();
            committedTransactions.add(status.getTransactionName());
        }

        @Override
        protected void onRolledBack(SimpleTransactionStatus status) {
            onRolledBackCallCount.incrementAndGet();
            rolledBackTransactions.add(status.getTransactionName());
        }

        @Override
        protected void logSyncError(String phase, Exception ex) {
            // no-op for testing
        }

        public void exposeOnCommitted(SimpleTransactionStatus status) {
            onCommitted(status);
        }

        public void exposeOnRolledBack(SimpleTransactionStatus status) {
            onRolledBack(status);
        }

        public List<String> getCommittedTransactions() {
            return new ArrayList<>(committedTransactions);
        }

        public List<String> getRolledBackTransactions() {
            return new ArrayList<>(rolledBackTransactions);
        }

        public int getOnCommittedCallCount() {
            return onCommittedCallCount.get();
        }

        public int getOnRolledBackCallCount() {
            return onRolledBackCallCount.get();
        }

        public void reset() {
            committedTransactions.clear();
            rolledBackTransactions.clear();
            onCommittedCallCount.set(0);
            onRolledBackCallCount.set(0);
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
