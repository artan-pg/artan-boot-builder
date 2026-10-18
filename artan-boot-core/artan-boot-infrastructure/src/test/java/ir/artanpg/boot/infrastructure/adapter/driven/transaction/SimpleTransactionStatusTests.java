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
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link SimpleTransactionStatus}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("SimpleTransactionStatus")
class SimpleTransactionStatusTests {

    @Nested
    @DisplayName("construction and accessors")
    class ConstructionAndAccessors {

        @Test
        @DisplayName("Exposes definition-backed name, readOnly and isolation")
        void construction_ShouldExposeDefinitionAttributes_WhenCreated() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .name("place-order")
                    .readOnly(true)
                    .isolation(TransactionIsolation.READ_COMMITTED)
                    .build();

            // when
            SimpleTransactionStatus status = new SimpleTransactionStatus(definition, true, null);

            // then
            then(status.getTransactionName())
                    .as("transaction name comes from the definition")
                    .isEqualTo("place-order");
            then(status.isReadOnly())
                    .as("readOnly comes from the definition")
                    .isTrue();
            then(status.getIsolationLevel())
                    .as("isolation comes from the definition")
                    .isEqualTo(TransactionIsolation.READ_COMMITTED);
            then(status.isNewTransaction())
                    .as("newTransaction flag is preserved")
                    .isTrue();
            then(status.getStatus())
                    .as("initial lifecycle state is ACTIVE")
                    .isEqualTo(Status.ACTIVE);
            then(status.isCompleted())
                    .as("ACTIVE status is not completed")
                    .isFalse();
            then(status.isRollbackOnly())
                    .as("ACTIVE status is not rollback-only")
                    .isFalse();
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws when definition is null")
        void construction_ShouldThrowTransactionalDomainException_WhenDefinitionIsNull() {
            // when / then
            thenThrownBy(() -> new SimpleTransactionStatus(null, true, null))
                    .as("null definition must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("definition");
        }
    }

    @Nested
    @DisplayName("setRollbackOnly()")
    class SetRollbackOnly {

        @Test
        @DisplayName("Transitions ACTIVE to MARKED_ROLLBACK")
        void setRollbackOnly_ShouldMarkRollbackOnly_WhenStatusIsActive() {
            // given
            SimpleTransactionStatus status = activeStatus();

            // when
            status.setRollbackOnly();

            // then
            then(status.isRollbackOnly())
                    .as("status should be rollback-only after setRollbackOnly")
                    .isTrue();
            then(status.getStatus())
                    .as("lifecycle state should be MARKED_ROLLBACK")
                    .isEqualTo(Status.MARKED_ROLLBACK);
            then(status.isCompleted())
                    .as("MARKED_ROLLBACK is not a completed state")
                    .isFalse();
        }

        @Test
        @DisplayName("Is idempotent when already MARKED_ROLLBACK")
        void setRollbackOnly_ShouldRemainMarked_WhenAlreadyRollbackOnly() {
            // given
            SimpleTransactionStatus status = activeStatus();
            status.setRollbackOnly();

            // when
            status.setRollbackOnly();

            // then
            then(status.getStatus())
                    .as("second setRollbackOnly must not change terminal semantics")
                    .isEqualTo(Status.MARKED_ROLLBACK);
        }

        @Test
        @DisplayName("Does not change a completed status")
        void setRollbackOnly_ShouldNotChangeState_WhenAlreadyCommitted() {
            // given
            CallSetStatusSimpleTransactionStatus status = activeStatus();
            status.setStatus(Status.COMMITTED);

            // when
            status.setRollbackOnly();

            // then
            then(status.getStatus())
                    .as("setRollbackOnly must not alter a completed transaction")
                    .isEqualTo(Status.COMMITTED);
            then(status.isRollbackOnly())
                    .as("committed status is not rollback-only")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("setStatus(Status)")
    class SetStatus {

        @Test
        @DisplayName("Moves to COMMITTED and reports completed")
        void setStatus_ShouldMarkCompleted_WhenCommitted() {
            // given
            CallSetStatusSimpleTransactionStatus status = activeStatus();

            // when
            status.setStatus(Status.COMMITTED);

            // then
            then(status.getStatus())
                    .as("lifecycle state should be COMMITTED")
                    .isEqualTo(Status.COMMITTED);
            then(status.isCompleted())
                    .as("COMMITTED is a completed state")
                    .isTrue();
        }

        @Test
        @DisplayName("Moves to ROLLED_BACK and reports completed")
        void setStatus_ShouldMarkCompleted_WhenRolledBack() {
            // given
            CallSetStatusSimpleTransactionStatus status = activeStatus();

            // when
            status.setStatus(Status.ROLLED_BACK);

            // then
            then(status.getStatus())
                    .as("lifecycle state should be ROLLED_BACK")
                    .isEqualTo(Status.ROLLED_BACK);
            then(status.isCompleted())
                    .as("ROLLED_BACK is a completed state")
                    .isTrue();
        }

        @Test
        @DisplayName("Throws when new status is null")
        void setStatus_ShouldThrowTransactionalDomainException_WhenNull() {
            // given
            CallSetStatusSimpleTransactionStatus status = activeStatus();

            // when / then
            thenThrownBy(() -> status.setStatus(null))
                    .as("null status transition must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("Status");
        }
    }

    @Nested
    @DisplayName("bindFlushAction(Runnable)")
    class BindFlushAction {

        @Test
        @DisplayName("Binds the action so a later flush runs it")
        void bindFlushAction_ShouldBindAction_WhenNonNull() {
            // given
            AtomicInteger calls = new AtomicInteger();
            SimpleTransactionStatus status = activeStatus();

            // when
            status.bindFlushAction(calls::incrementAndGet);
            status.flush();

            // then
            then(calls.get())
                    .as("bound flush action should run on flush")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Replaces a previously bound action")
        void bindFlushAction_ShouldReplacePreviousAction_WhenCalledAgain() {
            // given
            AtomicBoolean first = new AtomicBoolean();
            AtomicBoolean second = new AtomicBoolean();
            SimpleTransactionStatus status = activeStatus();
            status.bindFlushAction(() -> first.set(true));

            // when
            status.bindFlushAction(() -> second.set(true));
            status.flush();

            // then
            then(first.get())
                    .as("previous flush action must not run after rebind")
                    .isFalse();
            then(second.get())
                    .as("latest bound flush action should run")
                    .isTrue();
        }

        @Test
        @DisplayName("Replaces a constructor-supplied action")
        void bindFlushAction_ShouldReplaceConstructorAction_WhenRebound() {
            // given
            AtomicBoolean constructorAction = new AtomicBoolean();
            AtomicBoolean reboundAction = new AtomicBoolean();
            SimpleTransactionStatus status = new SimpleTransactionStatus(
                    TransactionDefinition.withDefaults(),
                    true,
                    () -> constructorAction.set(true));

            // when
            status.bindFlushAction(() -> reboundAction.set(true));
            status.flush();

            // then
            then(constructorAction.get())
                    .as("constructor flush action must not run after rebind")
                    .isFalse();
            then(reboundAction.get())
                    .as("rebound flush action should run")
                    .isTrue();
        }

        @Test
        @DisplayName("Ignores null and keeps the previous action")
        void bindFlushAction_ShouldKeepPreviousAction_WhenNull() {
            // given
            AtomicInteger calls = new AtomicInteger();
            SimpleTransactionStatus status = activeStatus();
            status.bindFlushAction(calls::incrementAndGet);

            // when
            status.bindFlushAction(null);
            status.flush();

            // then
            then(calls.get())
                    .as("null bind must not clear the previously bound action")
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("flush()")
    class Flush {

        @Test
        @DisplayName("Runs the bound flush action when active")
        void flush_ShouldRunBoundAction_WhenStatusIsActive() {
            // given
            AtomicInteger calls = new AtomicInteger();
            SimpleTransactionStatus status = new SimpleTransactionStatus(
                    TransactionDefinition.withDefaults(), true, null);
            status.bindFlushAction(calls::incrementAndGet);

            // when
            status.flush();

            // then
            then(calls.get())
                    .as("bound flush action should run once")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Replaces the flush action when bindFlushAction is called again")
        void flush_ShouldReplaceAction_WhenCalledAgain() {
            // given
            AtomicBoolean first = new AtomicBoolean();
            AtomicBoolean second = new AtomicBoolean();
            SimpleTransactionStatus status = new SimpleTransactionStatus(
                    TransactionDefinition.withDefaults(), true, () -> first.set(false));
            status.bindFlushAction(() -> second.set(true));

            // when
            status.flush();

            // then
            then(first.get())
                    .as("previous flush action must not run after rebind")
                    .isFalse();
            then(second.get())
                    .as("latest bound flush action should run")
                    .isTrue();
        }

        @Test
        @DisplayName("Does nothing when no flush action is bound")
        void flush_ShouldDoNothing_WhenNoActionBound() {
            // given
            SimpleTransactionStatus status = activeStatus();

            // when / then
            status.flush();
            then(status.isCompleted())
                    .as("flush without action must not complete the transaction")
                    .isFalse();
        }

        @Test
        @DisplayName("Throws when the transaction is already completed")
        void flush_ShouldThrowTransactionalDomainException_WhenCompleted() {
            // given
            CallSetStatusSimpleTransactionStatus status = activeStatus();
            status.setStatus(Status.COMMITTED);

            // when / then
            thenThrownBy(status::flush)
                    .as("flush on a completed transaction must fail")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("Cannot flush");
        }

        @Test
        @DisplayName("Still allows flush when status is MARKED_ROLLBACK")
        void flush_ShouldRunAction_WhenMarkedRollbackOnly() {
            // given
            AtomicBoolean ran = new AtomicBoolean();
            SimpleTransactionStatus status = new SimpleTransactionStatus(
                    TransactionDefinition.withDefaults(), true, () -> ran.set(true));
            status.setRollbackOnly();

            // when
            status.flush();

            // then
            then(ran.get())
                    .as("flush should still be allowed before terminal completion")
                    .isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static CallSetStatusSimpleTransactionStatus activeStatus() {
        return new CallSetStatusSimpleTransactionStatus(TransactionDefinition.withDefaults(), true, null);
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
