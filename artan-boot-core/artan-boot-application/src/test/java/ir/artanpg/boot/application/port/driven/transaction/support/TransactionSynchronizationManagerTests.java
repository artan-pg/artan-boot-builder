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

import ir.artanpg.boot.application.port.driven.transaction.TransactionSynchronization;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link TransactionSynchronizationManager}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("TransactionSynchronizationManager")
class TransactionSynchronizationManagerTests {

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.deactivateSynchronization();
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @SneakyThrows
        @Test
        @DisplayName("Throws UnsupportedOperationException when instantiated via reflection")
        void Constructor_ShouldThrowUnsupportedOperationException_WhenInstantiatedViaReflection() {
            // given
            Constructor<TransactionSynchronizationManager> constructor =
                    TransactionSynchronizationManager.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            // when / then
            thenThrownBy(() -> {
                try {
                    constructor.newInstance();
                } catch (InvocationTargetException ex) {
                    throw ex.getCause();
                }
            })
                    .as("exception thrown when the private constructor is invoked via reflection")
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("activateSynchronization()")
    class ActivateSynchronization {

        @Test
        @DisplayName("Marks synchronization as active on the current thread")
        void ActivateSynchronization_ShouldMarkActive_WhenCalled() {
            // given
            // when
            TransactionSynchronizationManager.activateSynchronization();

            // then
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization active flag after activateSynchronization")
                    .isTrue();
        }

        @Test
        @DisplayName("Is idempotent when already active")
        void ActivateSynchronization_ShouldRemainActive_WhenCalledAgain() {
            // given
            TransactionSynchronizationManager.activateSynchronization();

            // when
            TransactionSynchronizationManager.activateSynchronization();

            // then
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization active flag after a second activateSynchronization call")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("deactivateSynchronization()")
    class DeactivateSynchronization {

        @Test
        @DisplayName("Clears the active flag and registered synchronizations")
        void DeactivateSynchronization_ShouldClearState_WhenSynchronizationWasActive() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(new TestSynchronization("s1"));

            // when
            TransactionSynchronizationManager.deactivateSynchronization();

            // then
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization active flag after deactivateSynchronization")
                    .isFalse();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("registered synchronizations after deactivateSynchronization")
                    .isEmpty();
        }

        @Test
        @DisplayName("Is safe to call when synchronization was never activated")
        void DeactivateSynchronization_ShouldLeaveInactive_WhenNeverActivated() {
            // given
            // when
            TransactionSynchronizationManager.deactivateSynchronization();

            // then
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("synchronization active flag after deactivate on a never-activated thread")
                    .isFalse();
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("synchronizations list after deactivate on a never-activated thread")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("getSynchronizationActive()")
    class GetSynchronizationActive {

        @Test
        @DisplayName("Returns false when synchronization has not been activated")
        void GetSynchronizationActive_ShouldReturnFalse_WhenNotActivated() {
            // given
            // when
            boolean active = TransactionSynchronizationManager.getSynchronizationActive();

            // then
            then(active)
                    .as("getSynchronizationActive on a thread that never activated synchronization")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns true when synchronization has been activated")
        void GetSynchronizationActive_ShouldReturnTrue_WhenActivated() {
            // given
            TransactionSynchronizationManager.activateSynchronization();

            // when
            boolean active = TransactionSynchronizationManager.getSynchronizationActive();

            // then
            then(active)
                    .as("getSynchronizationActive after activateSynchronization")
                    .isTrue();
        }

        @Test
        @DisplayName("Returns false after synchronization has been deactivated")
        void GetSynchronizationActive_ShouldReturnFalse_WhenDeactivated() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.deactivateSynchronization();

            // when
            boolean active = TransactionSynchronizationManager.getSynchronizationActive();

            // then
            then(active)
                    .as("getSynchronizationActive after deactivateSynchronization")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("registerSynchronization(TransactionSynchronization)")
    class RegisterSynchronization {

        @Test
        @DisplayName("Appends the synchronization when active")
        void RegisterSynchronization_ShouldAppendSynchronization_WhenActive() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronization sync = new TestSynchronization("s1");

            // when
            TransactionSynchronizationManager.registerSynchronization(sync);

            // then
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("synchronizations list after registering one callback")
                    .containsExactly(sync);
        }

        @Test
        @DisplayName("Preserves registration order for multiple synchronizations")
        void RegisterSynchronization_ShouldPreserveRegistrationOrder_WhenMultipleRegistered() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronization first = new TestSynchronization("first");
            TransactionSynchronization second = new TestSynchronization("second");

            // when
            TransactionSynchronizationManager.registerSynchronization(first);
            TransactionSynchronizationManager.registerSynchronization(second);

            // then
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("synchronizations list order after registering two callbacks")
                    .containsExactly(first, second);
        }

        @Test
        @DisplayName("Throws TransactionalDomainException when synchronization is not active")
        void RegisterSynchronization_ShouldThrowTransactionalDomainException_WhenNotActive() {
            // given
            TransactionSynchronization sync = new TestSynchronization("inactive");

            // when / then
            thenThrownBy(() -> TransactionSynchronizationManager.registerSynchronization(sync))
                    .as("exception thrown when registering while synchronization is not active")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Transaction synchronization not active");
        }

        @Test
        @DisplayName("Throws TransactionalDomainException after synchronization was deactivated")
        void RegisterSynchronization_ShouldThrowTransactionalDomainException_WhenDeactivated() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.deactivateSynchronization();
            TransactionSynchronization sync = new TestSynchronization("after-deactivate");

            // when / then
            thenThrownBy(() -> TransactionSynchronizationManager.registerSynchronization(sync))
                    .as("exception thrown when registering after deactivateSynchronization")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Transaction synchronization not active");
        }
    }

    @Nested
    @DisplayName("getSynchronizations()")
    class GetSynchronizations {

        @Test
        @DisplayName("Returns empty list when nothing is registered")
        void GetSynchronizations_ShouldReturnEmptyList_WhenNothingRegistered() {
            // given
            // when
            List<TransactionSynchronization> result =
                    TransactionSynchronizationManager.getSynchronizations();

            // then
            then(result)
                    .as("synchronizations snapshot when nothing has been registered")
                    .isEmpty();
        }

        @Test
        @DisplayName("Returns a snapshot that is not affected by later registrations")
        void GetSynchronizations_ShouldReturnDefensiveCopy_WhenLaterRegistrationsOccur() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronization first = new TestSynchronization("first");
            TransactionSynchronizationManager.registerSynchronization(first);

            // when
            List<TransactionSynchronization> snapshot =
                    TransactionSynchronizationManager.getSynchronizations();
            TransactionSynchronization second = new TestSynchronization("second");
            TransactionSynchronizationManager.registerSynchronization(second);

            // then
            then(snapshot)
                    .as("snapshot taken before the second registration must stay unchanged")
                    .containsExactly(first);
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("fresh snapshot after the second registration")
                    .containsExactly(first, second);
        }

        @Test
        @DisplayName("Returns empty list after deactivateSynchronization")
        void GetSynchronizations_ShouldReturnEmptyList_WhenDeactivated() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(new TestSynchronization("s1"));
            TransactionSynchronizationManager.deactivateSynchronization();

            // when
            List<TransactionSynchronization> result =
                    TransactionSynchronizationManager.getSynchronizations();

            // then
            then(result)
                    .as("synchronizations after deactivateSynchronization")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("clearSynchronizations()")
    class ClearSynchronizations {

        @Test
        @DisplayName("Removes registered synchronizations without deactivating")
        void ClearSynchronizations_ShouldRemoveRegistrations_WhenActive() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(new TestSynchronization("s1"));
            TransactionSynchronizationManager.registerSynchronization(new TestSynchronization("s2"));

            // when
            TransactionSynchronizationManager.clearSynchronizations();

            // then
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("synchronizations after clearSynchronizations")
                    .isEmpty();
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("activation flag must remain true after clearSynchronizations")
                    .isTrue();
        }

        @Test
        @DisplayName("Is safe to call when the list is already empty")
        void ClearSynchronizations_ShouldLeaveEmpty_WhenNothingRegistered() {
            // given
            TransactionSynchronizationManager.activateSynchronization();

            // when
            TransactionSynchronizationManager.clearSynchronizations();

            // then
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("synchronizations after clearSynchronizations on an empty list")
                    .isEmpty();
            then(TransactionSynchronizationManager.getSynchronizationActive())
                    .as("activation flag must remain true after clearing an empty list")
                    .isTrue();
        }

        @Test
        @DisplayName("Allows new registrations after clearing")
        void ClearSynchronizations_ShouldAllowNewRegistrations_WhenCalledWhileActive() {
            // given
            TransactionSynchronizationManager.activateSynchronization();
            TransactionSynchronizationManager.registerSynchronization(new TestSynchronization("old"));
            TransactionSynchronizationManager.clearSynchronizations();
            TransactionSynchronization fresh = new TestSynchronization("fresh");

            // when
            TransactionSynchronizationManager.registerSynchronization(fresh);

            // then
            then(TransactionSynchronizationManager.getSynchronizations())
                    .as("synchronizations after clear and a new registration")
                    .containsExactly(fresh);
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods and test doubles (after all Nested classes)
    // -------------------------------------------------------------------------

    /**
     * Minimal {@link TransactionSynchronization} test double identified by a name.
     */
    private record TestSynchronization(String name) implements TransactionSynchronization {

        @Override
        public void beforeCommit(boolean readOnly) {
            // no-op for tests
        }

        @Override
        public void afterCommit() {
            // no-op for tests
        }

        @Override
        public void beforeCompletion() {
            // no-op for tests
        }

        @Override
        public void afterCompletion(int status) {
            // no-op for tests
        }

        @Override
        public void flush() throws TransactionalDomainException {
            // no-op for tests
        }

        @Override
        public String toString() {
            return "TestSynchronization[" + name + "]";
        }
    }
}
