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
import ir.artanpg.boot.application.port.driven.transaction.TransactionPropagation;
import ir.artanpg.boot.application.port.driven.transaction.TransactionStatus;
import ir.artanpg.boot.application.port.driven.transaction.TransactionSynchronization;
import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link AbstractTransactionManager}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("AbstractTransactionManager")
class TransactionManagerTests {

    private final TestableTransactionManager manager = new TestableTransactionManager();

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.deactivateSynchronization();
        TransactionContext.clear();

        suspendedResourcesStack().clear();
        suspendedResourcesThreadLocal().remove();
    }

    @SuppressWarnings("unchecked")
    private static ThreadLocal<Deque<Object>> suspendedResourcesThreadLocal() {
        try {
            Field field = AbstractTransactionManager.class.getDeclaredField("SUSPENDED_RESOURCES");
            field.setAccessible(true);
            return (ThreadLocal<Deque<Object>>) field.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access the suspended-resources ThreadLocal", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Deque<Object> suspendedResourcesStack() {
        try {
            Field field = AbstractTransactionManager.class.getDeclaredField("SUSPENDED_RESOURCES");
            field.setAccessible(true);
            ThreadLocal<Deque<Object>> threadLocal = (ThreadLocal<Deque<Object>>) field.get(null);
            return threadLocal.get();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to inspect the suspended-resources stack", ex);
        }
    }

    @Nested
    @DisplayName("executeInTransaction(TransactionCallback)")
    class ExecuteInTransactionWithDefaults {

        @Test
        @DisplayName("Commits an owned transaction using default REQUIRED propagation")
        void ExecuteInTransaction_ShouldCommit_WhenCallbackSucceedsWithDefaults() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("tx-default");

            // when
            String result = manager.executeInTransaction(_ -> "ok");

            // then
            then(result).as("callback result").isEqualTo("ok");
            then(manager.beginCalled).as("beginTransaction invoked").isTrue();
            then(manager.commitCalled).as("commit invoked").isTrue();
            then(manager.rollbackCalled).as("rollback not invoked").isFalse();
            then(TransactionContext.getCurrentStatus())
                    .as("context cleared after owned transaction")
                    .isNull();
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization deactivated after owned transaction")
                    .isFalse();
        }

        @Test
        @DisplayName("executeNonTransactional translates RuntimeException thrown by the non-transactional callback")
        void ExecuteInTransaction_ShouldPropagateRuntimeException_WhenNonTransactionalCallbackThrows() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.SUPPORTS)
                    .build();
            RuntimeException failure = new IllegalStateException("non-tx runtime failure");

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> sneakyThrow(failure), definition))
                    .as("RuntimeException passes through executeNonTransactional's catch untranslated")
                    .isSameAs(failure);
            then(manager.beginCalled).as("no transaction was begun").isFalse();
            then(manager.rollbackCalled).as("no rollback for a non-transactional failure").isFalse();
            then(manager.commitCalled).as("no commit for a non-transactional failure").isFalse();
        }

        @Test
        @DisplayName("executeNonTransactional wraps checked Exception from the callback")
        void ExecuteInTransaction_ShouldWrapCheckedException_WhenNonTransactionalCallbackThrowsChecked() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.NEVER)
                    .build();
            IOException checked = new IOException("non-tx io failure");

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> sneakyThrow(checked), definition))
                    .as("checked exception translated after leaving executeNonTransactional's try block")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Transaction execution failed")
                    .hasCauseInstanceOf(IOException.class);
            then(manager.beginCalled).as("begin never called for NEVER without existing").isFalse();
        }

        @Test
        @DisplayName("executeNonTransactional propagates DomainException unchanged from a suspended NOT_SUPPORTED call")
        void ExecuteInTransaction_ShouldPropagateDomainException_WhenNotSupportedCallbackThrows() {
            // given
            MutableTransactionStatus outer = new MutableTransactionStatus("nt-fail-outer");
            TransactionContext.setCurrentStatus(outer);
            TransactionSynchronizationManager.activateSynchronization();
            RecordingSynchronization outerSync = new RecordingSynchronization("nt-fail-outer-sync");
            TransactionSynchronizationManager.registerSynchronization(outerSync);

            DomainException domain = new DomainException("business rule violated outside any transaction");
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.NOT_SUPPORTED)
                    .build();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> sneakyThrow(domain), definition))
                    .as("DomainException propagated unchanged from executeNonTransactional's catch")
                    .isSameAs(domain);
            then(manager.beginCalled).as("NOT_SUPPORTED never begins a transaction").isFalse();
            then(TransactionContext.getCurrentStatus())
                    .as("outer status resumed in the finally block despite the translated failure")
                    .isSameAs(outer);
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("outer synchronization flag restored after the failure")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("outer synchronizations re-registered after the failure")
                    .containsExactly(outerSync);
            then(suspendedResourcesStack())
                    .as("suspension unwound cleanly even though the callback failed")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("executeInTransaction(TransactionCallback, TransactionDefinition)")
    class ExecuteInTransactionWithDefinition {

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws TransactionalDomainException when operation is null")
        void ExecuteInTransaction_ShouldThrowTransactionalDomainException_WhenOperationIsNull() {
            // given
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(null, definition))
                    .as("exception when operation is null")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("The operation cannot be null");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws TransactionalDomainException when definition is null")
        void ExecuteInTransaction_ShouldThrowTransactionalDomainException_WhenDefinitionIsNull() {
            // given
            TransactionCallback<String> operation = _ -> "x";

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(operation, null))
                    .as("exception when definition is null")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("The definition cannot be null");
        }

        @Test
        @DisplayName("REQUIRED joins an existing transaction without begin or commit")
        void ExecuteInTransaction_ShouldParticipate_WhenRequiredAndExistingTransaction() {
            // given
            MutableTransactionStatus existing = new MutableTransactionStatus("outer");
            TransactionContext.setCurrentStatus(existing);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRED)
                    .build();

            // when
            String result = manager.executeInTransaction(status -> {
                then(status).as("status passed to participant").isSameAs(existing);
                return "joined";
            }, definition);

            // then
            then(result).as("participant result").isEqualTo("joined");
            then(manager.beginCalled).as("begin not called when joining").isFalse();
            then(manager.commitCalled).as("commit not called when joining").isFalse();
            then(manager.rollbackCalled).as("rollback not called when joining succeeds").isFalse();
            then(TransactionContext.getCurrentStatus())
                    .as("outer status remains bound after participation")
                    .isSameAs(existing);
        }

        @Test
        @DisplayName("REQUIRED marks existing status rollback-only when participant fails")
        void ExecuteInTransaction_ShouldMarkRollbackOnly_WhenRequiredParticipantFails() {
            // given
            MutableTransactionStatus existing = new MutableTransactionStatus("outer-fail");
            TransactionContext.setCurrentStatus(existing);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRED)
                    .build();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                throw new RuntimeException("participant failed");
            }, definition))
                    .as("participant failure is rethrown")
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("participant failed");
            then(existing.isRollbackOnly())
                    .as("existing status marked rollback-only after participant failure")
                    .isTrue();
        }

        @Test
        @DisplayName("REQUIRED creates an owned transaction when none exists")
        void ExecuteInTransaction_ShouldCreateOwnedTransaction_WhenRequiredAndNoExisting() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("owned");
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRED)
                    .build();

            // when
            Integer result = manager.executeInTransaction(_ -> 1, definition);

            // then
            then(result).as("owned transaction result").isEqualTo(1);
            then(manager.beginCalled).as("begin called for owned REQUIRED").isTrue();
            then(manager.commitCalled).as("commit called for owned REQUIRED").isTrue();
        }

        @Test
        @DisplayName("REQUIRES_NEW suspends existing transaction and commits the inner one")
        void ExecuteInTransaction_ShouldSuspendAndResume_WhenRequiresNewWithExisting() {
            // given
            MutableTransactionStatus outer = new MutableTransactionStatus("outer-req-new");
            TransactionContext.setCurrentStatus(outer);
            TransactionSynchronizationManager.activateSynchronization();
            RecordingSynchronization outerSync = new RecordingSynchronization("outer-sync");
            TransactionSynchronizationManager.registerSynchronization(outerSync);

            manager.statusToReturn = new MutableTransactionStatus("inner");
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRES_NEW)
                    .build();

            // when
            String result = manager.executeInTransaction(_ -> {
                then(TransactionContext.getCurrentStatus())
                        .as("inner status bound during REQUIRES_NEW")
                        .isSameAs(manager.statusToReturn);
                return "inner-done";
            }, definition);

            // then
            then(result).as("REQUIRES_NEW result").isEqualTo("inner-done");
            then(manager.beginCalled).as("begin called for REQUIRES_NEW").isTrue();
            then(manager.commitCalled).as("commit called for inner transaction").isTrue();
            then(TransactionContext.getCurrentStatus())
                    .as("outer status resumed after REQUIRES_NEW")
                    .isSameAs(outer);
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("outer synchronization resumed")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("outer synchronizations restored")
                    .contains(outerSync);
        }

        @Test
        @DisplayName("REQUIRES_NEW skips suspension when no transaction is active")
        void ExecuteInTransaction_ShouldSkipSuspension_WhenRequiresNewWithoutActiveTransaction() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("inner-no-outer");
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRES_NEW)
                    .build();

            // when
            String result = manager.executeInTransaction(_ -> {
                then(TransactionContext.getCurrentStatus())
                        .as("new status bound during REQUIRES_NEW without outer tx")
                        .isSameAs(manager.statusToReturn);
                return "inner-without-outer";
            }, definition);

            // then
            then(result).as("REQUIRES_NEW result without existing transaction")
                    .isEqualTo("inner-without-outer");
            then(manager.beginCalled).as("begin called for REQUIRES_NEW").isTrue();
            then(manager.commitCalled).as("commit called for REQUIRES_NEW").isTrue();
            then(TransactionContext.getCurrentStatus())
                    .as("context empty after cleanup; nothing was suspended so nothing resumed")
                    .isNull();
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization deactivated after owned transaction")
                    .isFalse();

            // and when – a subsequent REQUIRES_NEW on the same thread must not resume
            // a phantom transaction from the previous call (verifies the suspended stack
            // stayed empty because suspension was skipped by the guard)
            manager.beginCalled = false;
            manager.commitCalled = false;
            manager.statusToReturn = new MutableTransactionStatus("second-inner");

            String secondResult = manager.executeInTransaction(_ -> "second", definition);

            then(secondResult).as("second REQUIRES_NEW result").isEqualTo("second");
            then(TransactionContext.getCurrentStatus())
                    .as("no stale suspended transaction resumed on second call")
                    .isNull();
        }

        @Test
        @DisplayName("REQUIRES_NEW suspends an active transaction and resumes it after failure of the inner one")
        void ExecuteInTransaction_ShouldSuspendAndResumeAfterFailure_WhenRequiresNewWithActiveTransaction() {
            // given
            // active transaction with synchronizations registered, so the guard condition
            // (suspendExisting && isTransactionActive()) evaluates to true and
            // suspendCurrentResources() runs
            MutableTransactionStatus outer = new MutableTransactionStatus("outer-req-new-fail");
            TransactionContext.setCurrentStatus(outer);
            TransactionSynchronizationManager.activateSynchronization();
            RecordingSynchronization outerSync = new RecordingSynchronization("outer-sync-fail");
            TransactionSynchronizationManager.registerSynchronization(outerSync);

            manager.statusToReturn = new MutableTransactionStatus("inner-fail");
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRES_NEW)
                    .build();

            // when
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                then(TransactionContext.getCurrentStatus())
                        .as("outer status suspended; inner status bound during REQUIRES_NEW")
                        .isSameAs(manager.statusToReturn);
                throw new RuntimeException("inner failed");
            }, definition))
                    .as("inner failure is propagated")
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("inner failed");

            // then
            then(manager.beginCalled).as("begin called for REQUIRES_NEW").isTrue();
            then(manager.rollbackCalled).as("inner transaction rolled back").isTrue();
            then(manager.commitCalled).as("inner transaction not committed").isFalse();
            then(TransactionContext.getCurrentStatus())
                    .as("outer status resumed even though inner transaction failed")
                    .isSameAs(outer);
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("outer synchronization resumed after inner failure")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("outer synchronizations restored after inner failure")
                    .containsExactly(outerSync);
            then(outer.isRollbackOnly())
                    .as("failure of the inner REQUIRES_NEW tx does not mark the outer rollback-only")
                    .isFalse();
        }

        @Test
        @DisplayName("MANDATORY throws when no existing transaction is present")
        void ExecuteInTransaction_ShouldThrowTransactionalDomainException_WhenMandatoryWithoutExisting() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.MANDATORY)
                    .build();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> "x", definition))
                    .as("exception for MANDATORY without existing transaction")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("No existing transaction found for propagation MANDATORY");
        }

        @Test
        @DisplayName("MANDATORY participates when an existing transaction is present")
        void ExecuteInTransaction_ShouldParticipate_WhenMandatoryWithExisting() {
            // given
            MutableTransactionStatus existing = new MutableTransactionStatus("mandatory-outer");
            TransactionContext.setCurrentStatus(existing);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.MANDATORY)
                    .build();

            // when
            String result = manager.executeInTransaction(_ -> "joined-mandatory", definition);

            // then
            then(result).as("MANDATORY result").isEqualTo("joined-mandatory");
            then(manager.beginCalled).as("begin not called for MANDATORY join").isFalse();
        }

        @Test
        @DisplayName("SUPPORTS runs non-transactionally when no existing transaction")
        void ExecuteInTransaction_ShouldRunNonTransactional_WhenSupportsWithoutExisting() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.SUPPORTS)
                    .build();
            AtomicReference<TransactionStatus> seen = new AtomicReference<>();

            // when
            String result = manager.executeInTransaction(status -> {
                seen.set(status);
                return "supports-none";
            }, definition);

            // then
            then(result).as("SUPPORTS non-transactional result").isEqualTo("supports-none");
            then(manager.beginCalled).as("begin not called for SUPPORTS without existing").isFalse();
            then(seen.get().isNewTransaction())
                    .as("non-transactional status reports isNewTransaction false")
                    .isFalse();
            then(seen.get().getTransactionName())
                    .as("non-transactional status name")
                    .isEqualTo("non-transactional");
        }

        @Test
        @DisplayName("SUPPORTS joins when an existing transaction is present")
        void ExecuteInTransaction_ShouldParticipate_WhenSupportsWithExisting() {
            // given
            MutableTransactionStatus existing = new MutableTransactionStatus("supports-outer");
            TransactionContext.setCurrentStatus(existing);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.SUPPORTS)
                    .build();

            // when
            String result = manager.executeInTransaction(_ -> "supports-join", definition);

            // then
            then(result).as("SUPPORTS join result").isEqualTo("supports-join");
            then(manager.beginCalled).as("begin not called for SUPPORTS join").isFalse();
        }

        @Test
        @DisplayName("NOT_SUPPORTED suspends existing transaction and runs non-transactionally")
        void ExecuteInTransaction_ShouldSuspend_WhenNotSupportedWithExisting() {
            // given
            MutableTransactionStatus outer = new MutableTransactionStatus("not-supported-outer");
            TransactionContext.setCurrentStatus(outer);
            manager.statusToReturn = new MutableTransactionStatus("unused");
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.NOT_SUPPORTED)
                    .build();
            AtomicReference<TransactionStatus> seen = new AtomicReference<>();

            // when
            String result = manager.executeInTransaction(status -> {
                seen.set(status);
                then(TransactionContext.getCurrentStatus())
                        .as("no managed status during NOT_SUPPORTED")
                        .isNull();
                return "not-supported";
            }, definition);

            // then
            then(result).as("NOT_SUPPORTED result").isEqualTo("not-supported");
            then(manager.beginCalled).as("begin not called for NOT_SUPPORTED").isFalse();
            then(seen.get().getTransactionName())
                    .as("callback receives non-transactional status")
                    .isEqualTo("non-transactional");
            then(TransactionContext.getCurrentStatus())
                    .as("outer status resumed after NOT_SUPPORTED")
                    .isSameAs(outer);
        }

        @Test
        @DisplayName("NOT_SUPPORTED runs non-transactionally when nothing is active")
        void ExecuteInTransaction_ShouldRunNonTransactional_WhenNotSupportedWithoutExisting() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.NOT_SUPPORTED)
                    .build();

            // when
            String result = manager.executeInTransaction(_ -> "plain", definition);

            // then
            then(result).as("NOT_SUPPORTED without existing").isEqualTo("plain");
            then(manager.beginCalled).as("begin not called").isFalse();
        }

        @Test
        @DisplayName("NEVER throws when an existing transaction is present")
        void ExecuteInTransaction_ShouldThrowTransactionalDomainException_WhenNeverWithExisting() {
            // given
            TransactionContext.setCurrentStatus(new MutableTransactionStatus("never-outer"));
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.NEVER)
                    .build();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> "x", definition))
                    .as("exception for NEVER with existing transaction")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Existing transaction found for propagation NEVER");
        }

        @Test
        @DisplayName("NEVER runs non-transactionally when no transaction is active")
        void ExecuteInTransaction_ShouldRunNonTransactional_WhenNeverWithoutExisting() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.NEVER)
                    .build();

            // when
            String result = manager.executeInTransaction(_ -> "never-ok", definition);

            // then
            then(result).as("NEVER without existing").isEqualTo("never-ok");
            then(manager.beginCalled).as("begin not called for NEVER").isFalse();
        }

        @Test
        @DisplayName("Rolls back when the owned status is marked rollback-only by the callback")
        void ExecuteInTransaction_ShouldRollback_WhenStatusMarkedRollbackOnly() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("rb-only");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            String result = manager.executeInTransaction(status -> {
                status.setRollbackOnly();
                return "rb";
            }, definition);

            // then
            then(result).as("result when rollback-only").isEqualTo("rb");
            then(manager.rollbackCalled).as("rollback invoked for rollback-only").isTrue();
            then(manager.commitCalled).as("commit not invoked for rollback-only").isFalse();
        }

        @Test
        @DisplayName("Rolls back when beforeCommit marks the status rollback-only")
        void ExecuteInTransaction_ShouldRollback_WhenBeforeCommitMarksRollbackOnly() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("before-commit-rb");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(
                        new BeforeCommitMarksRollbackOnlySynchronization());
                return "x";
            }, definition);

            // then
            then(manager.rollbackCalled)
                    .as("rollback after beforeCommit marks rollback-only")
                    .isTrue();
            then(manager.commitCalled)
                    .as("commit skipped after beforeCommit marks rollback-only")
                    .isFalse();
        }

        @Test
        @DisplayName("Propagates RuntimeException from beforeCommit and rolls back")
        void ExecuteInTransaction_ShouldThrowRuntimeException_WhenBeforeCommitThrows() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("before-commit-fail");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(
                        new BeforeCommitThrowsSynchronization());
                return "x";
            }, definition))
                    .as("RuntimeException from beforeCommit propagates")
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("beforeCommit failed");
            then(manager.rollbackCalled)
                    .as("rollback after beforeCommit failure")
                    .isTrue();
            then(manager.commitCalled)
                    .as("commit not called after beforeCommit failure")
                    .isFalse();
        }

        @Test
        @DisplayName("invokeBeforeCommit wraps checked Exception from beforeCommit as TransactionalDomainException")
        void ExecuteInTransaction_ShouldWrapCheckedException_WhenBeforeCommitThrowsChecked() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("before-commit-checked");
            IOException checked = new IOException("beforeCommit io failure");
            RecordingSynchronization surviving = new RecordingSynchronization("surviving-before-commit");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(
                        new BeforeCommitThrowsCheckedSynchronization(checked));
                TransactionSynchronizationManager.registerSynchronization(surviving);
                return "x";
            }, definition))
                    .as("checked exception from beforeCommit hits invokeBeforeCommit's catch (Exception) block "
                            + "and is wrapped as TransactionalDomainException")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("beforeCommit failed")
                    .cause().isSameAs(checked);
            then(surviving.beforeCommitCalled)
                    .as("loop aborted by the wrap; later synchronizations never see beforeCommit")
                    .isFalse();
            then(manager.commitCalled)
                    .as("commit skipped after beforeCommit failure")
                    .isFalse();
            then(manager.rollbackCalled)
                    .as("rollback invoked through handleTransactionException")
                    .isTrue();
        }

        @Test
        @DisplayName("Invokes full synchronization lifecycle on successful commit")
        void ExecuteInTransaction_ShouldInvokeFullSyncLifecycle_WhenCommitSucceeds() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("sync-ok");
            RecordingSynchronization sync = new RecordingSynchronization("s1");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(sync);
                return "done";
            }, definition);

            // then
            then(sync.beforeCommitCalled).as("beforeCommit invoked").isTrue();
            then(sync.beforeCompletionCalled).as("beforeCompletion invoked").isTrue();
            then(sync.afterCommitCalled).as("afterCommit invoked").isTrue();
            then(sync.afterCompletionStatus)
                    .as("afterCompletion status is COMMITTED")
                    .isEqualTo(Status.COMMITTED.getCode());
        }

        @Test
        @DisplayName("invokeBeforeCompletion logs beforeCompletion failure and still commits the transaction")
        void ExecuteInTransaction_ShouldLogBeforeCompletionError_WhenBeforeCompletionThrowsOnCommitPath() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("bc-commit");
            BeforeCompletionThrowsSynchronization failing =
                    new BeforeCompletionThrowsSynchronization(new IllegalStateException("beforeCompletion boom"));
            RecordingSynchronization surviving = new RecordingSynchronization("surviving-before-completion");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            String result = manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(failing);
                TransactionSynchronizationManager.registerSynchronization(surviving);
                return "ok";
            }, definition);

            // then
            then(result).as("commit path completes despite the swallowed failure").isEqualTo("ok");
            then(manager.syncErrors)
                    .as("invokeBeforeCompletion catch (Exception) routes through logSyncError(\"beforeCompletion\")")
                    .contains("beforeCompletion");
            then(surviving.beforeCompletionCalled)
                    .as("loop continues after the logged failure; later synchronizations still see beforeCompletion")
                    .isTrue();
            then(manager.commitCalled).as("commit not aborted by beforeCompletion failure").isTrue();
            then(manager.rollbackCalled).as("no rollback triggered by the swallowed exception").isFalse();
            then(surviving.afterCompletionStatus)
                    .as("lifecycle completes as COMMITTED")
                    .isEqualTo(Status.COMMITTED.getCode());
        }

        @Test
        @DisplayName("invokeBeforeCompletion logs checked beforeCompletion exception without aborting the commit")
        void ExecuteInTransaction_ShouldLogCheckedBeforeCompletionError_WhenBeforeCompletionThrowsChecked() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("bc-checked");
            IOException checked = new IOException("beforeCompletion io failure");
            BeforeCompletionThrowsSynchronization failing = new BeforeCompletionThrowsSynchronization(checked);
            RecordingSynchronization surviving = new RecordingSynchronization("surviving-checked-bc");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            String result = manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(failing);
                TransactionSynchronizationManager.registerSynchronization(surviving);
                return "ok";
            }, definition);

            // then
            then(result)
                    .as("checked exception is caught by the same catch (Exception) block, not wrapped")
                    .isEqualTo("ok");
            then(manager.syncErrors)
                    .as("checked exception also lands in logSyncError(\"beforeCompletion\")")
                    .contains("beforeCompletion");
            then(surviving.beforeCompletionCalled)
                    .as("checked failure does not break the loop")
                    .isTrue();
            then(manager.commitCalled)
                    .as("commit proceeds normally")
                    .isTrue();
            then(manager.rollbackCalled)
                    .as("rollback never invoked for a swallowed beforeCompletion error")
                    .isFalse();
        }

        @Test
        @DisplayName("invokeBeforeCompletion logs beforeCompletion failure on the rollback path too")
        void ExecuteInTransaction_ShouldLogBeforeCompletionError_WhenBeforeCompletionThrowsOnRollbackPath() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("bc-rollback");
            BeforeCompletionThrowsSynchronization failing =
                    new BeforeCompletionThrowsSynchronization(new RuntimeException("beforeCompletion on rollback"));
            RecordingSynchronization surviving = new RecordingSynchronization("surviving-rollback-bc");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(failing);
                TransactionSynchronizationManager.registerSynchronization(surviving);
                throw new RuntimeException("callback failed");
            }, definition))
                    .as("original callback failure still propagates")
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("callback failed");
            then(manager.syncErrors)
                    .as("rollbackAndTrack -> invokeBeforeCompletion swallows and logs the sync failure")
                    .contains("beforeCompletion");
            then(surviving.beforeCompletionCalled)
                    .as("remaining synchronizations still receive beforeCompletion on the rollback path")
                    .isTrue();
            then(manager.rollbackCalled).as("rollback executed after the logged failure").isTrue();
            then(surviving.afterCompletionStatus)
                    .as("lifecycle completes as ROLLED_BACK")
                    .isEqualTo(Status.ROLLED_BACK.getCode());
        }

        @Test
        @DisplayName("Invokes synchronizations with ROLLED_BACK status when callback fails")
        void ExecuteInTransaction_ShouldInvokeSyncWithRolledBack_WhenCallbackFails() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("sync-fail");
            RecordingSynchronization sync = new RecordingSynchronization("s-fail");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(sync);
                throw new RuntimeException("callback failed");
            }, definition))
                    .as("callback failure propagates")
                    .isInstanceOf(RuntimeException.class);
            then(sync.beforeCompletionCalled)
                    .as("beforeCompletion invoked on failure path")
                    .isTrue();
            then(sync.afterCommitCalled)
                    .as("afterCommit not invoked on failure path")
                    .isFalse();
            then(sync.afterCompletionStatus)
                    .as("afterCompletion status is ROLLED_BACK")
                    .isEqualTo(Status.ROLLED_BACK.getCode());
            then(manager.rollbackCalled).as("rollback on callback failure").isTrue();
        }

        @Test
        @DisplayName("Logs synchronization errors without stopping remaining callbacks")
        void ExecuteInTransaction_ShouldLogSyncErrors_WhenSyncCallbacksThrow() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("sync-errors");
            FailingAfterSync failing = new FailingAfterSync();
            RecordingSynchronization surviving = new RecordingSynchronization("surviving");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(failing);
                TransactionSynchronizationManager.registerSynchronization(surviving);
                return "ok";
            }, definition);

            // then
            then(manager.syncErrors)
                    .as("logSyncError records afterCommit and afterCompletion failures")
                    .contains("afterCommit", "afterCompletion");
            then(surviving.afterCommitCalled)
                    .as("surviving afterCommit still runs")
                    .isTrue();
            then(surviving.afterCompletionStatus)
                    .as("surviving afterCompletion still runs")
                    .isEqualTo(Status.COMMITTED.getCode());
        }

        @Test
        @DisplayName("Propagates DomainException unchanged after rollback")
        void ExecuteInTransaction_ShouldPropagateDomainException_WhenCallbackThrowsDomainException() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("domain");
            DomainException domain = new DomainException("business rule");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                throw domain;
            }, definition))
                    .as("DomainException propagated unchanged")
                    .isSameAs(domain);
            then(manager.rollbackCalled).as("rollback after domain failure").isTrue();
        }

        @Test
        @DisplayName("Wraps checked Exception as TransactionalDomainException")
        void ExecuteInTransaction_ShouldWrapCheckedException_WhenCallbackThrowsCheckedException() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("checked");
            TransactionDefinition definition = TransactionDefinition.withDefaults();
            TransactionCallback<String> callback = _ -> sneakyThrow(new IOException("checked"));

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(callback, definition))
                    .as("checked exception wrapped")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Transaction execution failed")
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("Logs rollback failure during exception handling without masking the original error")
        void ExecuteInTransaction_ShouldLogRollbackFailure_WhenRollbackFailsDuringHandling() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("rb-fail");
            manager.rollbackException = new RuntimeException("rollback failed");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                throw new RuntimeException("callback failed");
            }, definition))
                    .as("original callback failure still propagates")
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("callback failed");
            then(manager.syncErrors)
                    .as("logSyncError called for failed rollback")
                    .contains("handleTransactionException");
        }

        @Test
        @DisplayName("Skips synchronization completion when failure occurs after synchronization was deactivated")
        void ExecuteInTransaction_ShouldSkipSyncCompletion_WhenSynchronizationInactiveOnFailure() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("sync-inactive-fail");
            TransactionDefinition definition = TransactionDefinition.withDefaults();
            RecordingSynchronization sync = new RecordingSynchronization("never-called");

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(sync);
                TransactionSynchronizationManager.deactivateSynchronization();
                throw new RuntimeException("fail after deactivate");
            }, definition))
                    .as("callback failure still propagates")
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("fail after deactivate");

            then(manager.rollbackCalled)
                    .as("rollback still runs for the active status")
                    .isTrue();
            then(sync.beforeCompletionCalled)
                    .as("beforeCompletion skipped because synchronization is inactive")
                    .isFalse();
            then(sync.afterCompletionStatus)
                    .as("afterCompletion skipped because synchronization is inactive")
                    .isNull();
        }

        @Test
        @DisplayName("Logs postCommit and runs defensive afterCompletion when committed")
        void ExecuteInTransaction_ShouldLogPostCommitAndInvokeAfterCompletion_WhenCommittedWithoutAfterCompletion() {
            // given
            MutableTransactionStatus status = new MutableTransactionStatus("post-commit");
            RecordingSynchronization sync = new RecordingSynchronization("post-commit-sync");
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(sync);

            Object tracker = newCompletionTracker(false);

            // when
            invokeHandleExecutionFailure(status, new RuntimeException("post-commit noise"), tracker);

            // then
            then(manager.syncErrors)
                    .as("logSyncError called with postCommit phase")
                    .contains("postCommit");
            then(sync.afterCompletionStatus)
                    .as("defensive afterCompletion invoked with COMMITTED status")
                    .isEqualTo(Status.COMMITTED.getCode());
            then(readTrackerFlag(tracker))
                    .as("tracker marks afterCompletion as invoked")
                    .isTrue();
        }

        @Test
        @DisplayName("Logs postCommit without afterCompletion when synchronization is inactive after commit")
        void ExecuteInTransaction_ShouldLogPostCommitOnly_WhenCommittedAndSynchronizationInactive() {
            // given
            MutableTransactionStatus status = new MutableTransactionStatus("post-commit-inactive");
            Object tracker = newCompletionTracker(false);

            // when
            invokeHandleExecutionFailure(status, new RuntimeException("post-commit inactive"), tracker);

            // then
            then(manager.syncErrors)
                    .as("logSyncError called with postCommit phase")
                    .contains("postCommit");
            then(readTrackerFlag(tracker))
                    .as("afterCompletion remains false when synchronization is inactive")
                    .isFalse();
        }

        @Test
        @DisplayName("Logs postCommit without re-invoking afterCompletion when it already ran")
        void ExecuteInTransaction_ShouldLogPostCommitOnly_WhenCommittedAndAfterCompletionAlreadyInvoked() {
            // given
            MutableTransactionStatus status = new MutableTransactionStatus("post-commit-done");
            RecordingSynchronization sync = new RecordingSynchronization("already-done");
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(sync);

            Object tracker = newCompletionTracker(true);

            // when
            invokeHandleExecutionFailure(status, new RuntimeException("post-commit already done"), tracker);

            // then
            then(manager.syncErrors)
                    .as("logSyncError called with postCommit phase")
                    .contains("postCommit");
            then(sync.afterCompletionStatus)
                    .as("afterCompletion not invoked again when tracker already recorded it")
                    .isNull();
        }

        @Test
        @DisplayName("Invokes only afterCompletion when beforeCompletion already ran before rollback failed")
        void ExecuteInTransaction_ShouldInvokeOnlyAfterCompletion_WhenBeforeCompletionAlreadyRanAndRollbackFails() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("rb-partial");
            manager.rollbackException = new RuntimeException("rollback failed mid-completion");
            RecordingSynchronization sync = new RecordingSynchronization("partial");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(status -> {
                TransactionSynchronizationManager.registerSynchronization(sync);
                status.setRollbackOnly();
                return "rb";
            }, definition))
                    .as("rollback failure propagates from completeTransaction")
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("rollback failed mid-completion");

            then(sync.beforeCompletionCalled)
                    .as("beforeCompletion ran inside rollbackAndTrack before rollback failed")
                    .isTrue();
            then(sync.afterCompletionStatus)
                    .as("afterCompletion invoked by handleExecutionFailure with ROLLED_BACK")
                    .isEqualTo(Status.ROLLED_BACK.getCode());
        }

        @Test
        @DisplayName("Invokes both completion callbacks when neither phase ran before callback failure")
        void ExecuteInTransaction_ShouldInvokeBothCompletionPhases_WhenNeitherRanBeforeCallbackFails() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("both-phases");
            RecordingSynchronization sync = new RecordingSynchronization("both-phases-sync");
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(sync);
                throw new IllegalStateException("callback failed before completion phases");
            }, definition))
                    .as("original callback failure propagates unchanged")
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("callback failed before completion phases");

            then(manager.rollbackCalled)
                    .as("handleTransactionException rolled the active status back")
                    .isTrue();
            then(sync.beforeCompletionCalled)
                    .as("beforeCompletion branch (!tracker.beforeCompletionInvoked) executed")
                    .isTrue();
            then(sync.afterCompletionStatus)
                    .as("afterCompletion branch (!tracker.afterCompletionInvoked) executed with ROLLED_BACK")
                    .isEqualTo(Status.ROLLED_BACK.getCode());
        }

        @Test
        @DisplayName("Skips afterCompletion when it already ran but the callback still fails")
        void ExecuteInTransaction_ShouldSkipAfterCompletion_WhenAlreadyInvokedAndCallbackFails() {
            // given
            MutableTransactionStatus status = new MutableTransactionStatus("after-done-rollback");
            RecordingSynchronization sync = new RecordingSynchronization("after-done");
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(sync);

            Object tracker = newCompletionTracker(true);
            setTrackerFlag(tracker, "committed", false);
            setTrackerFlag(tracker, "beforeCompletionInvoked", true);

            // when
            invokeHandleExecutionFailure(status, new RuntimeException("late failure"), tracker);

            // then
            then(manager.rollbackCalled)
                    .as("handleTransactionException still rolls the active status back")
                    .isTrue();
            then(sync.beforeCompletionCalled)
                    .as("beforeCompletion not invoked again when tracker already recorded it")
                    .isFalse();
            then(sync.afterCompletionStatus)
                    .as("false branch of !tracker.afterCompletionInvoked: no re-invocation")
                    .isNull();
            then(readTrackerFlag(tracker))
                    .as("tracker flag stays untouched")
                    .isTrue();
        }

        @Test
        @DisplayName("Runs only afterCompletion when beforeCompletion already ran during a successful rollback path")
        void HandleExecutionFailure_ShouldRunOnlyAfterCompletion_WhenBeforeCompletionAlreadyTracked() {
            // given
            MutableTransactionStatus status = new MutableTransactionStatus("before-done");
            status.completed = true;
            RecordingSynchronization sync = new RecordingSynchronization("before-done-sync");
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(sync);

            Object tracker = newCompletionTracker(false);
            setTrackerFlag(tracker, "committed", false);
            setTrackerFlag(tracker, "beforeCompletionInvoked", true);

            // when
            invokeHandleExecutionFailure(status, new RuntimeException("failure after completion"), tracker);

            // then
            then(manager.rollbackCalled)
                    .as("no rollback attempted for an already completed status")
                    .isFalse();
            then(sync.beforeCompletionCalled)
                    .as("beforeCompletion skipped because the tracker already recorded it")
                    .isFalse();
            then(sync.afterCompletionStatus)
                    .as("only afterCompletion runs, with ROLLED_BACK status")
                    .isEqualTo(Status.ROLLED_BACK.getCode());
            then(readTrackerFlag(tracker))
                    .as("tracker marks afterCompletion as invoked")
                    .isTrue();
        }

        @Test
        @DisplayName("Orders before callbacks ascending and after callbacks descending by order()")
        void ExecuteInTransaction_ShouldOrderSynchronizations_ByOrderMethod() {
            // given
            manager.statusToReturn = new MutableTransactionStatus("order");
            List<String> beforeOrder = new ArrayList<>();
            List<String> afterOrder = new ArrayList<>();
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(
                        new OrderedSynchronization(20, "high", beforeOrder, afterOrder));
                TransactionSynchronizationManager.registerSynchronization(
                        new OrderedSynchronization(5, "low", beforeOrder, afterOrder));
                return "ordered";
            }, definition);

            // then
            then(beforeOrder)
                    .as("before callbacks run ascending by order()")
                    .containsExactly("low", "high");
            then(afterOrder)
                    .as("after callbacks run descending by order()")
                    .containsExactly("high", "low");
        }
    }

    @Nested
    @DisplayName("suspendCurrentResources() / resumeSuspendedResources()")
    class SuspendResumeResources {

        private final TransactionDefinition requiresNew = TransactionDefinition.builder()
                .propagation(TransactionPropagation.REQUIRES_NEW)
                .build();

        private final TransactionDefinition notSupported = TransactionDefinition.builder()
                .propagation(TransactionPropagation.NOT_SUPPORTED)
                .build();

        @Test
        @DisplayName("Removes the suspended stack when the last frame with a status is resumed")
        void Resume_ShouldRemoveThreadLocal_WhenLastFrameWithStatusPopped() {
            // given
            MutableTransactionStatus outer = new MutableTransactionStatus("last-frame-status");
            TransactionContext.setCurrentStatus(outer);
            TransactionSynchronizationManager.activateSynchronization();
            RecordingSynchronization outerSync = new RecordingSynchronization("last-frame-sync");
            TransactionSynchronizationManager.registerSynchronization(outerSync);

            manager.statusToReturn = new MutableTransactionStatus("inner-last");

            // when
            String result = manager.executeInTransaction(_ -> "inner", requiresNew);

            // then
            then(result).as("inner result").isEqualTo("inner");
            then(manager.commitCalled).as("inner transaction committed").isTrue();
            then(TransactionContext.getCurrentStatus())
                    .as("outer status rebound by the null-check branch")
                    .isSameAs(outer);
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("outer synchronization reactivated on resume")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("outer synchronizations re-registered on resume")
                    .containsExactly(outerSync);
            then(suspendedResourcesStack())
                    .as("stack emptied by pop; ThreadLocal removed, not kept as an empty deque")
                    .isEmpty();

            // and when – a second REQUIRES_NEW without any outer transaction must find
            // an empty stack (no stale frame left behind by the first call)
            TransactionContext.clear();
            TransactionSynchronizationManager.deactivateSynchronization();
            manager.beginCalled = false;
            manager.commitCalled = false;
            manager.rollbackCalled = false;
            manager.statusToReturn = new MutableTransactionStatus("second-inner");

            String second = manager.executeInTransaction(_ -> "second", requiresNew);

            // then
            then(second).as("second inner result").isEqualTo("second");
            then(manager.commitCalled).as("second inner transaction committed").isTrue();
            then(suspendedResourcesStack())
                    .as("still no leaked frames after the second resume")
                    .isEmpty();
            then(TransactionContext.getCurrentStatus())
                    .as("context cleared by cleanup; nothing was resumed")
                    .isNull();
        }

        @Test
        @DisplayName("Leaves pending frames untouched when a nested call suspends nothing")
        void Resume_ShouldKeepPendingFrames_WhenNestedCallHasNothingToSuspendOrResume() {
            // given
            MutableTransactionStatus t0 = new MutableTransactionStatus("keep-frames-outer");
            RecordingSynchronization t0Sync = new RecordingSynchronization("keep-frames-outer-sync");
            TransactionContext.setCurrentStatus(t0);
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(t0Sync);

            manager.statusToReturn = new MutableTransactionStatus("t1");
            AtomicReference<Integer> depthAtT1Start = new AtomicReference<>();
            AtomicReference<Integer> depthAfterNested = new AtomicReference<>();
            AtomicReference<Boolean> contextEmptyInsideNested = new AtomicReference<>();
            AtomicReference<Boolean> syncActiveInsideNested = new AtomicReference<>();

            // when
            String result = manager.executeInTransaction(t1 -> {
                depthAtT1Start.set(suspendedResourcesStack().size());
                TransactionContext.clear();
                TransactionSynchronizationManager.deactivateSynchronization();

                TransactionDefinition definition = TransactionDefinition.builder()
                        .propagation(TransactionPropagation.NOT_SUPPORTED)
                        .build();
                String inner = manager.executeInTransaction(nsStatus -> {
                    contextEmptyInsideNested.set(!TransactionContext.isTransactionActive());
                    syncActiveInsideNested.set(TransactionSynchronizationManager.getSynchronizationActive());
                    then(nsStatus.getTransactionName())
                            .as("nested call runs non-transactionally")
                            .isEqualTo("non-transactional");
                    return "ns-inner";
                }, definition);

                depthAfterNested.set(suspendedResourcesStack().size());
                then(inner).as("nested non-transactional result").isEqualTo("ns-inner");

                // rebind T1 before returning so the owned completion path behaves normally
                TransactionContext.setCurrentStatus(t1);
                TransactionSynchronizationManager.activateSynchronization();
                return "t1:" + inner;
            }, requiresNew);

            // then
            then(result).as("full nested flow result").isEqualTo("t1:ns-inner");
            then(depthAtT1Start.get())
                    .as("exactly one frame (T0) was suspended when T1's callback started")
                    .isEqualTo(1);
            then(contextEmptyInsideNested.get())
                    .as("no status bound during the nested non-transactional call")
                    .isEqualTo(Boolean.TRUE);
            then(syncActiveInsideNested.get())
                    .as("synchronization stayed inactive because nothing was resumed for the nested call")
                    .isFalse();
            then(depthAfterNested.get())
                    .as("the empty-stack early return left T0's pending frame untouched")
                    .isEqualTo(1);
            then(TransactionContext.getCurrentStatus())
                    .as("T0 rebound by the final resume (status != null branch)")
                    .isSameAs(t0);
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("T0 synchronizations restored once the last frame was popped")
                    .containsExactly(t0Sync);
            then(suspendedResourcesStack())
                    .as("stack fully unwound; ThreadLocal removed after the last pop")
                    .isEmpty();
        }

        @Test
        @DisplayName("Keeps the ThreadLocal when an outer frame remains below the resumed one")
        void Resume_ShouldKeepThreadLocal_WhenFramesRemainBelowPoppedFrame() {
            // given
            MutableTransactionStatus t0 = new MutableTransactionStatus("t0");
            RecordingSynchronization t0Sync = new RecordingSynchronization("t0-sync");
            TransactionContext.setCurrentStatus(t0);
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(t0Sync);

            AtomicReference<TransactionStatus> t1SeenByInnerCall = new AtomicReference<>();
            AtomicReference<Boolean> stackSizeDuringInner = new AtomicReference<>();

            // when
            String result = manager.executeInTransaction(t1 -> {
                t1SeenByInnerCall.set(t1);
                manager.statusToReturn = new MutableTransactionStatus("t2");
                String inner = manager.executeInTransaction(_ -> {
                    stackSizeDuringInner.set(suspendedResourcesStack().size() >= 2);
                    return "t2-done";
                }, requiresNew);
                return "t1:" + inner;
            }, requiresNew);

            // then
            then(result).as("nested REQUIRES_NEW results").isEqualTo("t1:t2-done");
            then(stackSizeDuringInner.get())
                    .as("at least two frames were suspended during the innermost call")
                    .isTrue();
            then(t1SeenByInnerCall.get())
                    .as("T1 status rebound after T2's frame was resumed")
                    .isNotNull();
            then(TransactionContext.getCurrentStatus())
                    .as("T0 status rebound after T1's frame was resumed")
                    .isSameAs(t0);
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("T0 synchronization active again")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("T0 synchronizations restored exactly once")
                    .containsExactly(t0Sync);
            then(suspendedResourcesStack())
                    .as("after the last frame is popped the ThreadLocal is removed")
                    .isEmpty();
        }

        @Test
        @DisplayName("Resumes a frame whose captured synchronization state was inactive")
        void Resume_ShouldSkipReactivation_WhenFrameCapturedInactiveSynchronization() {
            // given
            MutableTransactionStatus t0 = new MutableTransactionStatus("null-sync-outer");
            RecordingSynchronization t0Sync = new RecordingSynchronization("null-sync-outer-sync");
            TransactionContext.setCurrentStatus(t0);
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(t0Sync);

            manager.statusToReturn = new MutableTransactionStatus("t1");
            AtomicReference<Integer> depthInsideNested = new AtomicReference<>();
            AtomicReference<Boolean> syncSkippedAfterNestedResume = new AtomicReference<>();

            String result = manager.executeInTransaction(t1 -> {
                TransactionSynchronizationManager.deactivateSynchronization();

                TransactionDefinition definition = TransactionDefinition.builder()
                        .propagation(TransactionPropagation.NOT_SUPPORTED)
                        .build();
                String inner = manager.executeInTransaction(nonTxStatus -> {
                    // frames pending now: [T1-inactive-sync (top), T0-active-sync]
                    depthInsideNested.set(suspendedResourcesStack().size());
                    then(nonTxStatus.getTransactionName())
                            .as("nested call runs non-transactionally")
                            .isEqualTo("non-transactional");
                    return "ns";
                }, definition);

                // resumeSuspendedResources popped the top frame: its captured flag was
                // false, so neither activateSynchronization nor re-registration ran,
                // even though the status itself was rebound by the null-check branch
                syncSkippedAfterNestedResume.set(
                        !TransactionSynchronizationManager.getSynchronizationActive()
                                && TransactionSynchronizationManager.getSynchronizations().isEmpty()
                                && TransactionContext.getCurrentStatus() == t1);
                then(inner).as("nested non-transactional result").isEqualTo("ns");
                return "t1-done";
            }, requiresNew);

            // then
            then(result).as("outer flow completed").isEqualTo("t1-done");
            then(depthInsideNested.get())
                    .as("two frames were pending inside the nested call")
                    .isEqualTo(2);
            then(syncSkippedAfterNestedResume.get())
                    .as("inactive-sync frame resumed without restoring synchronization")
                    .isEqualTo(Boolean.TRUE);
            then(TransactionContext.getCurrentStatus())
                    .as("T0 rebound only by the final resume of its own frame")
                    .isSameAs(t0);
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("T0 synchronization flag restored from its frame")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("T0 sync re-registered from its frame")
                    .containsExactly(t0Sync);
            then(suspendedResourcesStack())
                    .as("stack fully unwound; ThreadLocal removed after the last pop")
                    .isEmpty();
        }

        @Test
        @DisplayName("Rebinds the captured status when the resumed frame holds one")
        void Resume_ShouldRebindStatus_WhenFrameCapturedNonNullStatus() {
            // given
            MutableTransactionStatus captured = new MutableTransactionStatus("frame-status");
            RecordingSynchronization capturedSync = new RecordingSynchronization("frame-sync");
            pushSuspendedFrame(captured, List.of(capturedSync));
            then(suspendedResourcesStack()).as("precondition: one pending frame").hasSize(1);

            TransactionContext.setCurrentStatus(new MutableTransactionStatus("dangling"));
            TransactionSynchronizationManager.activateSynchronization();

            // when
            manager.cleanupAfterTransaction(true);

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("captured status rebound by the taken null-check branch")
                    .isSameAs(captured);
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("frame's active synchronization flag restored")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("frame's synchronizations re-registered")
                    .containsExactly(capturedSync);
            then(suspendedResourcesStack())
                    .as("last frame popped, ThreadLocal released")
                    .isEmpty();
        }

        @Test
        @DisplayName("Skips rebinding when the resumed frame captured no status")
        void Resume_ShouldSkipRebinding_WhenFrameCapturedNullStatus() {
            // given
            pushSuspendedFrame(null, List.of());
            then(suspendedResourcesStack()).as("precondition: one pending frame").hasSize(1);

            MutableTransactionStatus inner = new MutableTransactionStatus("inner-still-bound");
            TransactionContext.setCurrentStatus(inner);
            TransactionSynchronizationManager.activateSynchronization();

            // when
            manager.cleanupAfterTransaction(true);

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("no status rebound because the frame captured none")
                    .isNull();
            then(TransactionContext.isTransactionActive())
                    .as("thread reports no active transaction after the null-status resume")
                    .isFalse();
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization flag still restored independently of the status branch")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("nothing to re-register from an empty captured list")
                    .isEmpty();
            then(suspendedResourcesStack())
                    .as("last frame popped, ThreadLocal released")
                    .isEmpty();
        }

        @Test
        @DisplayName("Ignores a resume request when the suspended stack is empty")
        void Resume_ShouldDoNothing_WhenStackIsEmpty() {
            // given
            then(suspendedResourcesStack()).as("precondition: empty stack").isEmpty();
            MutableTransactionStatus dangling = new MutableTransactionStatus("dangling");
            TransactionContext.setCurrentStatus(dangling);
            TransactionSynchronizationManager.activateSynchronization();

            // when
            manager.cleanupAfterTransaction(true);

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("context cleared by cleanup; early return resumed nothing")
                    .isNull();
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization deactivated; early return reactivated nothing")
                    .isFalse();
            then(suspendedResourcesStack())
                    .as("early return left the untouched empty stack in place")
                    .isEmpty();
        }

        @Test
        @DisplayName("Restores synchronization state of each frame across mixed active/inactive suspensions")
        void Resume_ShouldRestoreSyncStatePerFrame_WhenFramesDifferInSynchronizationActivity() {
            // given
            MutableTransactionStatus outer = new MutableTransactionStatus("mixed-outer");
            RecordingSynchronization outerSync = new RecordingSynchronization("mixed-outer-sync");
            TransactionContext.setCurrentStatus(outer);
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(outerSync);

            AtomicReference<Boolean> syncActiveInsideInner = new AtomicReference<>();

            // when
            String result = manager.executeInTransaction(_ -> {
                syncActiveInsideInner.set(TransactionSynchronizationManager.getSynchronizationActive());
                TransactionSynchronizationManager.registerSynchronization(
                        new RecordingSynchronization("inner-sync"));
                return "mixed";
            }, requiresNew);

            // then
            then(result).as("inner result").isEqualTo("mixed");
            then(syncActiveInsideInner.get())
                    .as("synchronization activated for the inner transaction")
                    .isTrue();
            then(TransactionContext.getCurrentStatus())
                    .as("outer status restored")
                    .isSameAs(outer);
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("outer synchronization-active flag restored from the frame")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("only the outer sync survived; the inner one was dropped by cleanup")
                    .containsExactly(outerSync);
            then(outerSync.afterCompletionStatus)
                    .as("outer sync did not participate in the inner transaction lifecycle")
                    .isNull();
            then(suspendedResourcesStack())
                    .as("balanced suspend/resume leaves no frame behind")
                    .isEmpty();
        }

        @Test
        @DisplayName("NOT_SUPPORTED suspends and resumes the surrounding transaction resources")
        void Resume_ShouldRestoreOuterTransaction_WhenNotSupportedCompletesNonTransactionalWork() {
            // given
            MutableTransactionStatus outer = new MutableTransactionStatus("ns-outer");
            RecordingSynchronization outerSync = new RecordingSynchronization("ns-outer-sync");
            TransactionContext.setCurrentStatus(outer);
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(outerSync);

            AtomicReference<Boolean> syncActiveDuringNonTransactional = new AtomicReference<>();

            // when
            String result = manager.executeInTransaction(status -> {
                syncActiveDuringNonTransactional.set(
                        TransactionSynchronizationManager.getSynchronizationActive());
                then(status.getTransactionName())
                        .as("callback receives the non-transactional status")
                        .isEqualTo("non-transactional");
                return "ns";
            }, notSupported);

            // then
            then(result).as("NOT_SUPPORTED result").isEqualTo("ns");
            then(syncActiveDuringNonTransactional.get())
                    .as("synchronization deactivated while running non-transactionally")
                    .isFalse();
            then(TransactionContext.getCurrentStatus())
                    .as("outer status resumed by the finally block")
                    .isSameAs(outer);
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("outer synchronization flag resumed")
                    .isTrue();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("outer sync re-registered on resume")
                    .containsExactly(outerSync);
            then(suspendedResourcesStack())
                    .as("last frame popped -> ThreadLocal removed")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("registerSynchronization(TransactionSynchronization)")
    class RegisterSynchronization {

        @Test
        @DisplayName("Registers when synchronization is active")
        void RegisterSynchronization_ShouldRegister_WhenActive() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronization sync = new RecordingSynchronization("reg");

            // when
            manager.registerSynchronization(sync);

            // then
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("registered synchronization")
                    .containsExactly(sync);
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws TransactionalDomainException when synchronization is null")
        void RegisterSynchronization_ShouldThrowTransactionalDomainException_WhenNull() {
            // given

            // when & then
            thenThrownBy(() -> manager.registerSynchronization(null))
                    .as("exception when synchronization is null")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("The synchronization cannot be null");
        }

        @Test
        @DisplayName("Throws TransactionalDomainException when synchronization is not active")
        void RegisterSynchronization_ShouldThrowTransactionalDomainException_WhenNotActive() {
            // given
            TransactionSynchronization sync = new RecordingSynchronization("inactive");

            // when & then
            thenThrownBy(() -> manager.registerSynchronization(sync))
                    .as("exception when registering while inactive")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Transaction synchronization not active");
        }
    }

    @Nested
    @DisplayName("handleTransactionException(TransactionStatus, Exception)")
    class HandleTransactionException {

        @Test
        @DisplayName("Does nothing when status is null")
        void HandleTransactionException_ShouldDoNothing_WhenStatusIsNull() {
            // given
            // when
            manager.handleTransactionException(null, new RuntimeException("x"));

            // then
            then(manager.rollbackCalled).as("rollback not called for null status").isFalse();
        }

        @Test
        @DisplayName("Does nothing when status is already completed")
        void HandleTransactionException_ShouldDoNothing_WhenStatusIsCompleted() {
            // given
            MutableTransactionStatus status = new MutableTransactionStatus("done");
            status.completed = true;

            // when
            manager.handleTransactionException(status, new RuntimeException("x"));

            // then
            then(manager.rollbackCalled).as("rollback not called for completed status").isFalse();
        }

        @Test
        @DisplayName("Rolls back when status is active")
        void HandleTransactionException_ShouldRollback_WhenStatusIsActive() {
            // given
            MutableTransactionStatus status = new MutableTransactionStatus("active");

            // when
            manager.handleTransactionException(status, new RuntimeException("x"));

            // then
            then(manager.rollbackCalled).as("rollback called for active status").isTrue();
        }
    }

    @Nested
    @DisplayName("cleanupAfterTransaction(boolean)")
    class CleanupAfterTransaction {

        @Test
        @DisplayName("Deactivates synchronization and clears context")
        void CleanupAfterTransaction_ShouldReleaseThreadState_WhenCalled() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionContext.setCurrentStatus(new MutableTransactionStatus("ctx"));

            // when
            manager.cleanupAfterTransaction(false);

            // then
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization deactivated")
                    .isFalse();
            then(TransactionContext.getCurrentStatus())
                    .as("context cleared")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("translateAfterRollback(Exception)")
    class TranslateAfterRollback {

        @Test
        @DisplayName("Returns TransactionalDomainException unchanged")
        void TranslateAfterRollback_ShouldReturnSame_WhenTransactionalDomainException() {
            // given
            TransactionalDomainException original = new TransactionalDomainException("tx");

            // when
            RuntimeException result = manager.translateAfterRollback(original);

            // then
            then(result).as("same TransactionalDomainException").isSameAs(original);
        }

        @Test
        @DisplayName("Returns DomainException unchanged")
        void TranslateAfterRollback_ShouldReturnSame_WhenDomainException() {
            // given
            DomainException original = new DomainException("domain");

            // when
            RuntimeException result = manager.translateAfterRollback(original);

            // then
            then(result).as("same DomainException").isSameAs(original);
        }

        @Test
        @DisplayName("Returns RuntimeException unchanged")
        void TranslateAfterRollback_ShouldReturnSame_WhenRuntimeException() {
            // given
            RuntimeException original = new IllegalStateException("runtime");

            // when
            RuntimeException result = manager.translateAfterRollback(original);

            // then
            then(result).as("same RuntimeException").isSameAs(original);
        }

        @Test
        @DisplayName("Wraps checked Exception as TransactionalDomainException")
        void TranslateAfterRollback_ShouldWrap_WhenCheckedException() {
            // given
            Exception checked = new IOException("io");

            // when
            RuntimeException result = manager.translateAfterRollback(checked);

            // then
            then(result)
                    .as("wrapped type")
                    .isInstanceOf(TransactionalDomainException.class);
            then(result.getMessage())
                    .as("wrapped message")
                    .isEqualTo("Transaction execution failed");
            then(result.getCause())
                    .as("wrapped cause")
                    .isSameAs(checked);
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods and test doubles (after all Nested classes)
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T sneakyThrow(Throwable throwable) throws E {
        throw (E) throwable;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @SneakyThrows
    private void invokeHandleExecutionFailure(TransactionStatus status,
                                              Exception ex,
                                              Object tracker) {
        Method method = AbstractTransactionManager.class.getDeclaredMethod(
                "handleExecutionFailure",
                TransactionStatus.class,
                Exception.class,
                tracker.getClass());
        method.setAccessible(true);
        method.invoke(manager, status, ex, tracker);
    }

    @SneakyThrows
    private static Object newCompletionTracker(boolean afterCompletionInvoked) {
        Class<?> trackerType = Class.forName(
                "ir.artanpg.boot.application.port.driven.transaction.support."
                        + "AbstractTransactionManager$CompletionTracker");
        Constructor<?> ctor = trackerType.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object tracker = ctor.newInstance();
        setTrackerFlag(tracker, "committed", true);
        setTrackerFlag(tracker, "beforeCompletionInvoked", true);
        setTrackerFlag(tracker, "afterCompletionInvoked", afterCompletionInvoked);
        return tracker;
    }

    @SneakyThrows
    private static void setTrackerFlag(Object tracker, String fieldName, boolean value) {
        Field field = tracker.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(tracker, value);
    }

    @SneakyThrows
    private static boolean readTrackerFlag(Object tracker) {
        Field field = tracker.getClass().getDeclaredField("afterCompletionInvoked");
        field.setAccessible(true);
        return field.getBoolean(tracker);
    }

    private static void pushSuspendedFrame(TransactionStatus status,
                                           List<TransactionSynchronization> synchronizations) {
        try {
            String className =
                    "ir.artanpg.boot.application.port.driven.transaction.support." +
                            "AbstractTransactionManager$SuspendedResources";
            Class<?> frameType = Class.forName(className);
            Constructor<?> ctor = frameType.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            Object frame = ctor.newInstance(status, true, synchronizations);

            Field field = AbstractTransactionManager.class.getDeclaredField("SUSPENDED_RESOURCES");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ThreadLocal<Deque<Object>> threadLocal = (ThreadLocal<Deque<Object>>) field.get(null);
            threadLocal.get().push(frame);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to push a suspended-resources frame", ex);
        }
    }

    private static class TestableTransactionManager extends AbstractTransactionManager {

        private TransactionStatus statusToReturn = new MutableTransactionStatus("default");
        private boolean beginCalled;
        private boolean commitCalled;
        private boolean rollbackCalled;
        private RuntimeException rollbackException;
        private final List<String> syncErrors = new ArrayList<>();

        @Override
        protected TransactionStatus beginTransaction(TransactionDefinition definition) {
            beginCalled = true;
            return statusToReturn;
        }

        @Override
        protected void commit(TransactionStatus status) {
            commitCalled = true;
        }

        @Override
        protected void rollback(TransactionStatus status) {
            rollbackCalled = true;
            if (rollbackException != null) {
                throw rollbackException;
            }
        }

        @Override
        protected void logSyncError(String phase, Exception ex) {
            syncErrors.add(phase);
        }

        @Override
        public TransactionStatus getStatus() {
            return null;
        }
    }

    private static final class MutableTransactionStatus implements TransactionStatus {

        private final String name;
        private boolean rollbackOnly;
        private boolean completed;
        private boolean readOnly;

        private MutableTransactionStatus(String name) {
            this.name = name;
        }

        @Override
        public String getTransactionName() {
            return name;
        }

        @Override
        public boolean isNewTransaction() {
            return true;
        }

        @Override
        public boolean isReadOnly() {
            return readOnly;
        }

        @Override
        public void setRollbackOnly() {
            rollbackOnly = true;
        }

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        @Override
        public void flush() {
            // no-op
        }
    }

    private static final class RecordingSynchronization implements TransactionSynchronization {

        private final String name;
        private boolean beforeCommitCalled;
        private boolean beforeCompletionCalled;
        private boolean afterCommitCalled;
        private Integer afterCompletionStatus;

        private RecordingSynchronization(String name) {
            this.name = name;
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            beforeCommitCalled = true;
        }

        @Override
        public void afterCommit() {
            afterCommitCalled = true;
        }

        @Override
        public void beforeCompletion() {
            beforeCompletionCalled = true;
        }

        @Override
        public void afterCompletion(int status) {
            afterCompletionStatus = status;
        }

        @Override
        public void flush() {
            // no-op
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final class BeforeCommitMarksRollbackOnlySynchronization
            implements TransactionSynchronization {

        @Override
        public void beforeCommit(boolean readOnly) {
            TransactionStatus status = TransactionContext.getCurrentStatus();
            if (status != null) {
                status.setRollbackOnly();
            }
        }

        @Override
        public void afterCommit() {
            // no-op
        }

        @Override
        public void beforeCompletion() {
            // no-op
        }

        @Override
        public void afterCompletion(int status) {
            // no-op
        }

        @Override
        public void flush() {
            // no-op
        }
    }

    private record BeforeCommitThrowsCheckedSynchronization(Exception checked) implements TransactionSynchronization {

        @Override
        public void beforeCommit(boolean readOnly) {
            sneakyThrow(checked);
        }

        @Override
        public void afterCommit() {
            // no-op
        }

        @Override
        public void beforeCompletion() {
            // no-op
        }

        @Override
        public void afterCompletion(int status) {
            // no-op
        }

        @Override
        public void flush() {
            // no-op
        }
    }

    private static final class BeforeCommitThrowsSynchronization implements TransactionSynchronization {

        @Override
        public void beforeCommit(boolean readOnly) {
            throw new RuntimeException("beforeCommit failed");
        }

        @Override
        public void afterCommit() {
            // no-op
        }

        @Override
        public void beforeCompletion() {
            // no-op
        }

        @Override
        public void afterCompletion(int status) {
            // no-op
        }

        @Override
        public void flush() {
            // no-op
        }
    }

    private record BeforeCompletionThrowsSynchronization(Exception failure) implements TransactionSynchronization {

        @Override
        public void beforeCommit(boolean readOnly) {
            // no-op
        }

        @Override
        public void afterCommit() {
            // no-op
        }

        @Override
        public void beforeCompletion() {
            sneakyThrow(failure);
        }

        @Override
        public void afterCompletion(int status) {
            // no-op
        }

        @Override
        public void flush() {
            // no-op
        }
    }

    private static final class FailingAfterSync implements TransactionSynchronization {

        @Override
        public void beforeCommit(boolean readOnly) {
            // no-op
        }

        @Override
        public void afterCommit() {
            throw new RuntimeException("afterCommit failed");
        }

        @Override
        public void beforeCompletion() {
            // no-op
        }

        @Override
        public void afterCompletion(int status) {
            throw new RuntimeException("afterCompletion failed");
        }

        @Override
        public void flush() {
            // no-op
        }
    }

    private record OrderedSynchronization(int order,
                                          String label,
                                          List<String> beforeOrder,
                                          List<String> afterOrder) implements TransactionSynchronization {

        @Override
        public void beforeCommit(boolean readOnly) {
            beforeOrder.add(label);
        }

        @Override
        public void afterCommit() {
            afterOrder.add(label);
        }

        @Override
        public void beforeCompletion() {
            // no-op
        }

        @Override
        public void afterCompletion(int status) {
            // no-op
        }

        @Override
        public void flush() {
            // no-op
        }
    }
}
