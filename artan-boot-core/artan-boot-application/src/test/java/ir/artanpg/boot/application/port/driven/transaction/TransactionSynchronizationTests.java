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

package ir.artanpg.boot.application.port.driven.transaction;

import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link TransactionSynchronization}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("TransactionSynchronization")
class TransactionSynchronizationTests {

    @Nested
    @DisplayName("getOrder()")
    class GetOrder {

        @Test
        @DisplayName("Returns Integer.MAX_VALUE when the default implementation is used")
        void GetOrder_ShouldReturnMaxValue_WhenDefaultImplementationIsUsed() {
            // given
            TransactionSynchronization synchronization = new DefaultOrderSynchronization();

            // when
            int order = synchronization.order();

            // then
            then(order)
                    .as("default getOrder value")
                    .isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("Returns the overridden order when the implementation customizes it")
        void GetOrder_ShouldReturnCustomValue_WhenImplementationOverridesDefault() {
            // given
            TransactionSynchronization synchronization = new CustomOrderSynchronization(10);

            // when
            int order = synchronization.order();

            // then
            then(order)
                    .as("custom getOrder value from overriding implementation")
                    .isEqualTo(10);
        }

        @Test
        @DisplayName("Allows zero as a custom order value")
        void GetOrder_ShouldReturnZero_WhenImplementationUsesZero() {
            // given
            TransactionSynchronization synchronization = new CustomOrderSynchronization(0);

            // when
            int order = synchronization.order();

            // then
            then(order)
                    .as("getOrder when implementation returns zero")
                    .isZero();
        }

        @Test
        @DisplayName("Allows negative order values for higher precedence")
        void GetOrder_ShouldReturnNegativeValue_WhenImplementationUsesNegativeOrder() {
            // given
            TransactionSynchronization synchronization = new CustomOrderSynchronization(-5);

            // when
            int order = synchronization.order();

            // then
            then(order)
                    .as("getOrder when implementation returns a negative precedence value")
                    .isEqualTo(-5);
        }
    }

    // -------------------------------------------------------------------------
    // Helper methods and test doubles (after all Nested classes)
    // -------------------------------------------------------------------------

    /**
     * Implementation that relies on the default {@link TransactionSynchronization#order()}.
     */
    private static final class DefaultOrderSynchronization implements TransactionSynchronization {

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
            // no-op
        }

        @Override
        public void afterCompletion(int status) {
            // no-op
        }

        @Override
        public void flush() throws TransactionalDomainException {
            // no-op
        }
    }

    /**
     * Implementation that overrides {@link TransactionSynchronization#order ()}.
     */
    private record CustomOrderSynchronization(int order) implements TransactionSynchronization {

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
            // no-op
        }

        @Override
        public void afterCompletion(int status) {
            // no-op
        }

        @Override
        public void flush() throws TransactionalDomainException {
            // no-op
        }
    }
}
