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
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Flushable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenCode;

/**
 * Unit tests for the default methods of {@link TransactionSynchronization}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("TransactionSynchronization")
class TransactionSynchronizationTests {

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("Extends Flushable so implementations are flushable")
        void Interface_ShouldExtendFlushable_WhenInspected() {
            // given
            TransactionSynchronization sync = new PlainSynchronization();

            // when / then
            then(sync)
                    .as("a TransactionSynchronization must also be a Flushable")
                    .isInstanceOf(Flushable.class);
        }

        @Test
        @DisplayName("Can be implemented as a lambda-free anonymous class with no overrides")
        void Interface_ShouldBeImplementable_WhenNoMethodOverridden() {
            // given
            TransactionSynchronization sync = new TransactionSynchronization() {
            };

            // when / then
            then(sync)
                    .as("bare implementation of all default methods must instantiate cleanly")
                    .isNotNull();
            thenCode(() -> invokeAllHooks(sync))
                    .as("all default hooks on a bare implementation must be safe no-ops")
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getOrder()")
    class GetOrder {

        @Test
        @DisplayName("Returns Integer.MAX_VALUE by default")
        void GetOrder_ShouldReturnMaxValue_WhenNotOverridden() {
            // given
            TransactionSynchronization sync = new PlainSynchronization();

            // when
            int order = sync.getOrder();

            // then
            then(order)
                    .as("default order of a synchronization that does not override getOrder")
                    .isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("Honors the value returned by an overriding implementation")
        void GetOrder_ShouldReturnOverriddenValue_WhenImplementationOverrides() {
            // given
            TransactionSynchronization sync = new OrderedSynchronization(7);

            // when
            int order = sync.getOrder();

            // then
            then(order)
                    .as("order reported by an implementation overriding getOrder")
                    .isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("beforeCommit(boolean)")
    class BeforeCommit {

        @Test
        @DisplayName("Is a no-op by default for a read-write transaction")
        void BeforeCommit_ShouldDoNothing_WhenDefaultAndReadWrite() {
            // given
            DefaultOnlySynchronization sync = new DefaultOnlySynchronization();

            // when / then
            thenCode(() -> sync.beforeCommit(false))
                    .as("default beforeCommit with readOnly=false must be a safe no-op")
                    .doesNotThrowAnyException();
            then(sync.invokedHooks())
                    .as("only the explicitly invoked beforeCommit hook may run")
                    .containsOnly("beforeCommit");
        }

        @Test
        @DisplayName("Is a no-op by default for a read-only transaction")
        void BeforeCommit_ShouldDoNothing_WhenDefaultAndReadOnly() {
            // given
            DefaultOnlySynchronization sync = new DefaultOnlySynchronization();

            // when / then
            thenCode(() -> sync.beforeCommit(true))
                    .as("default beforeCommit with readOnly=true must be a safe no-op")
                    .doesNotThrowAnyException();
            then(sync.invokedHooks())
                    .as("only the explicitly invoked beforeCommit hook may run")
                    .containsOnly("beforeCommit");
        }

        @Test
        @DisplayName("Receives the readOnly flag from a custom implementation")
        void BeforeCommit_ShouldReceiveReadOnlyFlag_WhenImplementationOverrides() {
            // given
            RecordingSynchronization sync = new RecordingSynchronization();

            // when
            sync.beforeCommit(true);

            // then
            then(sync.invocations())
                    .as("overridden beforeCommit must record the invocation with its argument")
                    .containsExactly("beforeCommit(true)");
        }
    }

    @Nested
    @DisplayName("afterCommit()")
    class AfterCommit {

        @Test
        @DisplayName("Is a no-op by default")
        void AfterCommit_ShouldDoNothing_WhenDefault() {
            // given
            DefaultOnlySynchronization sync = new DefaultOnlySynchronization();

            // when / then
            thenCode(sync::afterCommit)
                    .as("default afterCommit must be a safe no-op")
                    .doesNotThrowAnyException();
            then(sync.invokedHooks())
                    .as("only the explicitly invoked afterCommit hook may run")
                    .containsOnly("afterCommit");
        }

        @Test
        @DisplayName("Records the invocation in a custom implementation")
        void AfterCommit_ShouldRecordInvocation_WhenImplementationOverrides() {
            // given
            RecordingSynchronization sync = new RecordingSynchronization();

            // when
            sync.afterCommit();

            // then
            then(sync.invocations())
                    .as("overridden afterCommit must record the invocation")
                    .containsExactly("afterCommit()");
        }
    }

    @Nested
    @DisplayName("beforeCompletion()")
    class BeforeCompletion {

        @Test
        @DisplayName("Is a no-op by default")
        void BeforeCompletion_ShouldDoNothing_WhenDefault() {
            // given
            DefaultOnlySynchronization sync = new DefaultOnlySynchronization();

            // when / then
            thenCode(sync::beforeCompletion)
                    .as("default beforeCompletion must be a safe no-op")
                    .doesNotThrowAnyException();
            then(sync.invokedHooks())
                    .as("only the explicitly invoked beforeCompletion hook may run")
                    .containsOnly("beforeCompletion");
        }

        @Test
        @DisplayName("Records the invocation in a custom implementation")
        void BeforeCompletion_ShouldRecordInvocation_WhenImplementationOverrides() {
            // given
            RecordingSynchronization sync = new RecordingSynchronization();

            // when
            sync.beforeCompletion();

            // then
            then(sync.invocations())
                    .as("overridden beforeCompletion must record the invocation")
                    .containsExactly("beforeCompletion()");
        }
    }

    @Nested
    @DisplayName("afterCompletion(int)")
    class AfterCompletion {

        @Test
        @DisplayName("Is a no-op by default")
        void AfterCompletion_ShouldDoNothing_WhenDefault() {
            // given
            DefaultOnlySynchronization sync = new DefaultOnlySynchronization();

            // when / then
            thenCode(() -> sync.afterCompletion(Status.COMMITTED.getCode()))
                    .as("default afterCompletion must be a safe no-op")
                    .doesNotThrowAnyException();
            then(sync.invokedHooks())
                    .as("only the explicitly invoked afterCompletion hook may run")
                    .containsOnly("afterCompletion");
        }

        @Test
        @DisplayName("Receives the committed status code from a custom implementation")
        void AfterCompletion_ShouldReceiveCommittedStatus_WhenImplementationOverrides() {
            // given
            RecordingSynchronization sync = new RecordingSynchronization();

            // when
            sync.afterCompletion(Status.COMMITTED.getCode());

            // then
            then(sync.invocations())
                    .as("overridden afterCompletion must record the invocation with its status")
                    .containsExactly("afterCompletion(" + Status.COMMITTED.getCode() + ")");
        }

        @Test
        @DisplayName("Receives the rolled-back status code from a custom implementation")
        void AfterCompletion_ShouldReceiveRolledBackStatus_WhenImplementationOverrides() {
            // given
            RecordingSynchronization sync = new RecordingSynchronization();

            // when
            sync.afterCompletion(Status.ROLLED_BACK.getCode());

            // then
            then(sync.invocations())
                    .as("overridden afterCompletion must record the rollback status")
                    .containsExactly("afterCompletion(" + Status.ROLLED_BACK.getCode() + ")");
        }
    }

    @Nested
    @DisplayName("flush()")
    class Flush {

        @Test
        @DisplayName("Is a no-op by default")
        void Flush_ShouldDoNothing_WhenDefault() {
            // given
            DefaultOnlySynchronization sync = new DefaultOnlySynchronization();

            // when / then
            thenCode(sync::flush)
                    .as("default flush must be a safe no-op")
                    .doesNotThrowAnyException();
            then(sync.invokedHooks())
                    .as("only the explicitly invoked flush hook may run")
                    .containsOnly("flush");
        }

        @Test
        @DisplayName("Does not throw TransactionalDomainException by default")
        void Flush_ShouldNotThrowTransactionalDomainException_WhenDefault() {
            // given
            TransactionSynchronization sync = new PlainSynchronization();

            // when / then
            thenCode(sync::flush)
                    .as("default flush declares TransactionalDomainException but never throws it")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Records the invocation in a custom implementation")
        void Flush_ShouldRecordInvocation_WhenImplementationOverrides() {
            // given
            RecordingSynchronization sync = new RecordingSynchronization();

            // when
            sync.flush();

            // then
            then(sync.invocations())
                    .as("overridden flush must record the invocation")
                    .containsExactly("flush()");
        }

        @Test
        @DisplayName("Propagates TransactionalDomainException thrown by a failing implementation")
        void Flush_ShouldPropagateException_WhenImplementationThrows() {
            // given
            TransactionSynchronization sync = new FailingFlushSynchronization();

            // when / then
            thenCode(sync::flush)
                    .as("failing flush implementation must propagate its exception")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Flush failed");
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods and test doubles (after all Nested classes)
    // -------------------------------------------------------------------------

    /**
     * Invokes every lifecycle hook of the given synchronization in the
     * documented order: beforeCommit, beforeCompletion, afterCommit,
     * afterCompletion and flush.
     *
     * @param sync the synchronization to drive through the full lifecycle
     */
    private static void invokeAllHooks(TransactionSynchronization sync) {
        sync.beforeCommit(false);
        sync.beforeCompletion();
        sync.afterCommit();
        sync.afterCompletion(Status.COMMITTED.getCode());
        sync.flush();
    }

    /**
     * Bare {@link TransactionSynchronization} that relies entirely on the
     * default implementations of every method.
     */
    private static final class PlainSynchronization implements TransactionSynchronization {
    }

    /**
     * {@link TransactionSynchronization} test double that tracks which hook
     * implementations were invoked, so tests can assert the default
     * implementations act as isolated no-ops without cascading into other
     * callbacks.
     */
    private static final class DefaultOnlySynchronization implements TransactionSynchronization {

        private final Set<String> invokedHooks = new HashSet<>();

        Set<String> invokedHooks() {
            return Set.copyOf(invokedHooks);
        }

        @Override
        public int getOrder() {
            invokedHooks.add("getOrder");
            return TransactionSynchronization.super.getOrder();
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            invokedHooks.add("beforeCommit");
            TransactionSynchronization.super.beforeCommit(readOnly);
        }

        @Override
        public void afterCommit() {
            invokedHooks.add("afterCommit");
            TransactionSynchronization.super.afterCommit();
        }

        @Override
        public void beforeCompletion() {
            invokedHooks.add("beforeCompletion");
            TransactionSynchronization.super.beforeCompletion();
        }

        @Override
        public void afterCompletion(int status) {
            invokedHooks.add("afterCompletion");
            TransactionSynchronization.super.afterCompletion(status);
        }

        @Override
        public void flush() throws TransactionalDomainException {
            invokedHooks.add("flush");
            TransactionSynchronization.super.flush();
        }
    }

    /**
     * {@link TransactionSynchronization} that overrides only the execution
     * order.
     */
    private record OrderedSynchronization(int order) implements TransactionSynchronization {

        @Override
        public int getOrder() {
            return order;
        }
    }

    /**
     * {@link TransactionSynchronization} test double that records every hook
     * invocation together with its arguments, while keeping the default
     * behavior for the methods it does not override.
     */
    private static final class RecordingSynchronization implements TransactionSynchronization {

        private final List<String> invocations = new ArrayList<>();

        List<String> invocations() {
            return List.copyOf(invocations);
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            invocations.add("beforeCommit(" + readOnly + ")");
        }

        @Override
        public void afterCommit() {
            invocations.add("afterCommit()");
        }

        @Override
        public void beforeCompletion() {
            invocations.add("beforeCompletion()");
        }

        @Override
        public void afterCompletion(int status) {
            invocations.add("afterCompletion(" + status + ")");
        }

        @Override
        public void flush() throws TransactionalDomainException {
            invocations.add("flush()");
        }
    }

    /**
     * {@link TransactionSynchronization} whose {@code flush} always fails with
     * a {@link TransactionalDomainException}.
     */
    private static final class FailingFlushSynchronization implements TransactionSynchronization {

        @Override
        public void flush() throws TransactionalDomainException {
            throw new TransactionalDomainException("Flush failed");
        }
    }
}
