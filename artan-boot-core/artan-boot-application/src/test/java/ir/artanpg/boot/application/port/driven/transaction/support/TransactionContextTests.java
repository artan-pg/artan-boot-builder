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

import ir.artanpg.boot.application.port.driven.transaction.TransactionStatus;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link TransactionContext}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("TransactionContext")
class TransactionContextTests {

    @AfterEach
    void tearDown() {
        TransactionContext.clear();
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @SneakyThrows
        @Test
        @DisplayName("Throws UnsupportedOperationException when instantiated via reflection")
        void Constructor_ShouldThrowUnsupportedOperationException_WhenInstantiatedViaReflection() {
            // given
            Constructor<TransactionContext> constructor = TransactionContext.class.getDeclaredConstructor();
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
    @DisplayName("setCurrentStatus(TransactionStatus)")
    class SetCurrentStatus {

        @Test
        @DisplayName("Binds the given status to the current thread")
        void SetCurrentStatus_ShouldBindStatus_WhenStatusIsNonNull() {
            // given
            TransactionStatus status = new TestTransactionStatus("tx-1");

            // when
            TransactionContext.setCurrentStatus(status);

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("status bound to the current thread after setCurrentStatus")
                    .isSameAs(status);
            then(TransactionContext.isTransactionActive())
                    .as("transaction marked active after binding a non-null status")
                    .isTrue();
        }

        @Test
        @DisplayName("Replaces any previously bound status")
        void SetCurrentStatus_ShouldReplacePreviousStatus_WhenCalledAgainWithDifferentStatus() {
            // given
            TransactionStatus first = new TestTransactionStatus("tx-first");
            TransactionStatus second = new TestTransactionStatus("tx-second");
            TransactionContext.setCurrentStatus(first);

            // when
            TransactionContext.setCurrentStatus(second);

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("status after replacing the previously bound value")
                    .isSameAs(second);
            then(TransactionContext.getCurrentStatus())
                    .as("previously bound status must no longer be visible")
                    .isNotSameAs(first);
        }

        @Test
        @DisplayName("Clears the binding when status is null")
        void SetCurrentStatus_ShouldClearBinding_WhenStatusIsNull() {
            // given
            TransactionContext.setCurrentStatus(new TestTransactionStatus("tx-clear"));

            // when
            TransactionContext.setCurrentStatus(null);

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("status after setCurrentStatus(null)")
                    .isNull();
            then(TransactionContext.isTransactionActive())
                    .as("transaction must not be active after clearing via null status")
                    .isFalse();
        }

        @Test
        @DisplayName("Leaves the thread unbound when status is null and nothing was bound")
        void SetCurrentStatus_ShouldLeaveUnbound_WhenStatusIsNullAndNothingWasBound() {
            // given
            TransactionContext.clear();

            // when
            TransactionContext.setCurrentStatus(null);

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("status when setCurrentStatus(null) is called on an unbound thread")
                    .isNull();
            then(TransactionContext.isTransactionActive())
                    .as("transaction must remain inactive when clearing an already unbound thread")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("getCurrentStatus()")
    class GetCurrentStatus {

        @Test
        @DisplayName("Returns the status bound to the current thread")
        void GetCurrentStatus_ShouldReturnBoundStatus_WhenStatusIsBound() {
            // given
            TransactionStatus status = new TestTransactionStatus("tx-get");
            TransactionContext.setCurrentStatus(status);

            // when
            TransactionStatus result = TransactionContext.getCurrentStatus();

            // then
            then(result)
                    .as("status returned by getCurrentStatus when a value is bound")
                    .isSameAs(status);
        }

        @Test
        @DisplayName("Returns null when no status is bound")
        void GetCurrentStatus_ShouldReturnNull_WhenNoStatusIsBound() {
            // given
            TransactionContext.clear();

            // when
            TransactionStatus result = TransactionContext.getCurrentStatus();

            // then
            then(result)
                    .as("status returned by getCurrentStatus when the thread is unbound")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("Removes the status bound to the current thread")
        void Clear_ShouldRemoveBoundStatus_WhenStatusIsBound() {
            // given
            TransactionContext.setCurrentStatus(new TestTransactionStatus("tx-to-clear"));

            // when
            TransactionContext.clear();

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("status after clear when a value was previously bound")
                    .isNull();
            then(TransactionContext.isTransactionActive())
                    .as("transaction must not be active after clear")
                    .isFalse();
        }

        @Test
        @DisplayName("Is safe to call when no status is bound")
        void Clear_ShouldLeaveUnbound_WhenNoStatusIsBound() {
            // given
            TransactionContext.clear();

            // when
            TransactionContext.clear();

            // then
            then(TransactionContext.getCurrentStatus())
                    .as("status after clear on an already unbound thread")
                    .isNull();
            then(TransactionContext.isTransactionActive())
                    .as("transaction must remain inactive after redundant clear")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("isTransactionActive()")
    class IsTransactionActive {

        @Test
        @DisplayName("Returns true when a status is bound")
        void IsTransactionActive_ShouldReturnTrue_WhenStatusIsBound() {
            // given
            TransactionContext.setCurrentStatus(new TestTransactionStatus("tx-active"));

            // when
            boolean active = TransactionContext.isTransactionActive();

            // then
            then(active)
                    .as("isTransactionActive when a non-null status is bound")
                    .isTrue();
        }

        @Test
        @DisplayName("Returns false when no status is bound")
        void IsTransactionActive_ShouldReturnFalse_WhenNoStatusIsBound() {
            // given
            TransactionContext.clear();

            // when
            boolean active = TransactionContext.isTransactionActive();

            // then
            then(active)
                    .as("isTransactionActive when the thread is unbound")
                    .isFalse();
        }

        @Test
        @DisplayName("Returns false after the bound status is cleared")
        void IsTransactionActive_ShouldReturnFalse_WhenStatusWasCleared() {
            // given
            TransactionContext.setCurrentStatus(new TestTransactionStatus("tx-then-cleared"));
            TransactionContext.clear();

            // when
            boolean active = TransactionContext.isTransactionActive();

            // then
            then(active)
                    .as("isTransactionActive after clear removes the previously bound status")
                    .isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods and test doubles (after all Nested classes)
    // -------------------------------------------------------------------------

    /**
     * Minimal {@link TransactionStatus} test double used only to bind a
     * non-null value into {@link TransactionContext}.
     */
    private static final class TestTransactionStatus implements TransactionStatus {

        private final String name;
        private boolean rollbackOnly;

        private TestTransactionStatus(String name) {
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
            return false;
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
            return false;
        }

        @Override
        public void flush() throws TransactionalDomainException {
            // no-op for tests
        }
    }
}
