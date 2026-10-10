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

package ir.artanpg.boot.domain.model;

import ir.artanpg.boot.domain.exception.BusinessValidationException;
import ir.artanpg.boot.domain.exception.DomainException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serial;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link AbstractSoftDeletableAggregateRoot}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("AbstractSoftDeletableAggregateRoot")
class SoftDeletableAggregateRootTests {

    private static final String AGGREGATE_ID = "aggregate-1";
    private static final String ERROR_NOT_RESTORABLE = "Aggregate is not restorable";
    private static final String ERROR_ID_NULL = "The identifier cannot be null";

    @Nested
    @DisplayName("Constructor with Identifier")
    class ConstructorWithIdentifier {

        @Test
        @DisplayName("should initialize deleted as false when created")
        void constructor_ShouldInitializeDeletedAsFalse_WhenCreated() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);

            // when
            TestSoftDeletableAggregate aggregate = new TestSoftDeletableAggregate(id);

            // then
            then(aggregate.isDeleted())
                    .as("Aggregate should not be deleted initially")
                    .isFalse();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when id is null")
        void constructor_ShouldThrowDomainException_WhenIdIsNull() {
            // given
            TestIdentifier nullId = null;

            // when & then
            thenThrownBy(() -> new TestSoftDeletableAggregate(nullId))
                    .as("Should throw DomainException when id is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_ID_NULL);
        }
    }

    @Nested
    @DisplayName("Constructor with Builder")
    class ConstructorWithBuilder {

        @Test
        @DisplayName("should set deleted from builder when provided")
        void constructor_ShouldSetDeletedFromBuilder_WhenProvided() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            TestSoftDeletableAggregate.TestBuilder builder =
                    new TestSoftDeletableAggregate.TestBuilder()
                            .identifier(id)
                            .deleted(true);

            // when
            TestSoftDeletableAggregate aggregate = new TestSoftDeletableAggregate(builder);

            // then
            then(aggregate.isDeleted())
                    .as("Deleted state should match builder value")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("isDeleted")
    class IsDeleted {

        @Test
        @DisplayName("should return false when not deleted")
        void isDeleted_ShouldReturnFalse_WhenNotDeleted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            boolean result = aggregate.isDeleted();

            // then
            then(result)
                    .as("Should return false when not deleted")
                    .isFalse();
        }

        @Test
        @DisplayName("should return true after markAsDeleted")
        void isDeleted_ShouldReturnTrue_AfterMarkAsDeleted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();

            // then
            then(aggregate.isDeleted())
                    .as("Should return true after deletion")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("markAsDeleted")
    class MarkAsDeleted {

        @Test
        @DisplayName("should set deleted to true when not deleted")
        void markAsDeleted_ShouldSetDeletedToTrue_WhenNotDeleted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();

            // then
            then(aggregate.isDeleted())
                    .as("Should be deleted after markAsDeleted")
                    .isTrue();
        }

        @Test
        @DisplayName("should be idempotent when already deleted")
        void markAsDeleted_ShouldBeIdempotent_WhenAlreadyDeleted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();

            // when
            aggregate.markAsDeleted();
            aggregate.markAsDeleted();

            // then
            then(aggregate.isDeleted())
                    .as("Should remain deleted after multiple calls")
                    .isTrue();
            then(aggregate.getHookInvocationCount())
                    .as("onMarkedAsDeleted hook must fire exactly once (idempotency guard)")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should invoke onMarkedAsDeleted hook once on state transition")
        void markAsDeleted_ShouldInvokeHook_WhenTransitioningToDeleted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();

            // then
            then(aggregate.getHookInvocationCount())
                    .as("Deletion hook should be invoked on transition")
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("isRestorable")
    class IsRestorable {

        @Test
        @DisplayName("should return false when not deleted")
        void isRestorable_ShouldReturnFalse_WhenNotDeleted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            boolean result = aggregate.isRestorable();

            // then
            then(result)
                    .as("Should not be restorable when not deleted")
                    .isFalse();
        }

        @Test
        @DisplayName("should return true when deleted")
        void isRestorable_ShouldReturnTrue_WhenDeleted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();

            // when
            boolean result = aggregate.isRestorable();

            // then
            then(result)
                    .as("Should be restorable when deleted")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("restore")
    class Restore {

        @Test
        @DisplayName("should set deleted to false when restorable")
        void restore_ShouldSetDeletedToFalse_WhenRestorable() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();

            // when
            aggregate.restore();

            // then
            then(aggregate.isDeleted())
                    .as("Should not be deleted after restore")
                    .isFalse();
            then(aggregate.getRestoredHookCount())
                    .as("onRestored hook should fire once on restoration")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("should throw BusinessValidationException when not restorable")
        void restore_ShouldThrowException_WhenNotRestorable() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when & then
            thenThrownBy(aggregate::restore)
                    .as("Should throw when not restorable")
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessage(ERROR_NOT_RESTORABLE);
        }
    }

    @Nested
    @DisplayName("onMarkedAsDeleted")
    class OnMarkedAsDeletedTests {

        @Test
        void onMarkedAsDeleted_ShouldBeInvokedOnce_WhenMarkAsDeletedCalled() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();

            // then
            then(aggregate.getHookInvocationCount())
                    .as("onMarkedAsDeleted hook should be invoked exactly once")
                    .isEqualTo(1);
        }

        @Test
        void onMarkedAsDeleted_ShouldNotBeInvokedAgain_WhenAlreadyDeleted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();

            // when
            aggregate.markAsDeleted();
            aggregate.markAsDeleted();

            // then
            then(aggregate.getHookInvocationCount())
                    .as("onMarkedAsDeleted hook must fire exactly once due to idempotency guard")
                    .isEqualTo(1);
        }

        @Test
        void onMarkedAsDeleted_ShouldBeInvokedAgain_AfterRestoreAndReDelete() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();
            aggregate.restore();

            // when
            aggregate.markAsDeleted();

            // then
            then(aggregate.getHookInvocationCount())
                    .as("onMarkedAsDeleted hook should fire again after restore cycle")
                    .isEqualTo(2);
        }

        @Test
        void onMarkedAsDeleted_ShouldBeInvokedForEachCycle_WhenMultipleDeleteRestoreCycles() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();
            aggregate.restore();
            aggregate.markAsDeleted();
            aggregate.restore();
            aggregate.markAsDeleted();

            // then
            then(aggregate.getHookInvocationCount())
                    .as("onMarkedAsDeleted hook should fire once per delete cycle")
                    .isEqualTo(3);
        }

        @Test
        void onMarkedAsDeleted_ShouldSetDeletedStateBeforeHook_WhenCalled() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();

            // then
            then(aggregate.isDeleted())
                    .as("Aggregate should be in deleted state after hook invocation")
                    .isTrue();
            then(aggregate.getHookInvocationCount())
                    .as("Hook should have been invoked")
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("onRestored")
    class OnRestoredTests {

        @Test
        void onRestored_ShouldBeInvokedOnce_WhenRestoreCalledAfterDeletion() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();

            // when
            aggregate.restore();

            // then
            then(aggregate.getRestoredHookCount())
                    .as("onRestored hook should be invoked exactly once")
                    .isEqualTo(1);
        }

        @Test
        void onRestored_ShouldNotBeInvoked_WhenRestoreCalledWithoutPriorDeletion() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when & then
            thenThrownBy(aggregate::restore)
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessage("Aggregate is not restorable");

            then(aggregate.getRestoredHookCount())
                    .as("onRestored hook should not be invoked when not restorable")
                    .isZero();
        }

        @Test
        void onRestored_ShouldNotBeInvokedAgain_WhenRestoreCalledTwiceAfterSingleDeletion() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();
            aggregate.restore();

            // when & then
            thenThrownBy(aggregate::restore)
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessage("Aggregate is not restorable");

            then(aggregate.getRestoredHookCount())
                    .as("onRestored hook must fire exactly once per restore cycle")
                    .isEqualTo(1);
        }

        @Test
        void onRestored_ShouldBeInvokedForEachCycle_WhenMultipleDeleteRestoreCycles() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();
            aggregate.restore();
            aggregate.markAsDeleted();
            aggregate.restore();
            aggregate.markAsDeleted();
            aggregate.restore();

            // then
            then(aggregate.getRestoredHookCount())
                    .as("onRestored hook should fire once per restore cycle")
                    .isEqualTo(3);
        }

        @Test
        void onRestored_ShouldClearDeletedStateBeforeHook_WhenCalled() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();

            // when
            aggregate.restore();

            // then
            then(aggregate.isDeleted())
                    .as("Aggregate should not be in deleted state after restore")
                    .isFalse();
            then(aggregate.getRestoredHookCount())
                    .as("Hook should have been invoked")
                    .isEqualTo(1);
        }

        @Test
        void onRestored_ShouldAllowFreshDeletionAfterRestore_WhenCalled() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();
            aggregate.restore();

            // when
            aggregate.markAsDeleted();

            // then
            then(aggregate.isDeleted())
                    .as("Should be able to delete again after restore")
                    .isTrue();
            then(aggregate.getHookInvocationCount())
                    .as("onMarkedAsDeleted should fire again for fresh deletion")
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("onMarkedAsDeleted and onRestored interaction")
    class HookInteractionTests {

        @Test
        void hooks_ShouldAlternateCorrectly_WhenMultipleCyclesExecuted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();
            then(aggregate.getHookInvocationCount()).isEqualTo(1);
            then(aggregate.getRestoredHookCount()).isZero();

            aggregate.restore();
            then(aggregate.getHookInvocationCount()).isEqualTo(1);
            then(aggregate.getRestoredHookCount()).isEqualTo(1);

            aggregate.markAsDeleted();
            then(aggregate.getHookInvocationCount()).isEqualTo(2);
            then(aggregate.getRestoredHookCount()).isEqualTo(1);

            aggregate.restore();
            then(aggregate.getHookInvocationCount()).isEqualTo(2);
            then(aggregate.getRestoredHookCount()).isEqualTo(2);

            // then
            then(aggregate.isDeleted()).isFalse();
        }

        @Test
        void hooks_ShouldMaintainCorrectCounts_WhenIdempotentCallsMixed() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();
            aggregate.markAsDeleted();
            aggregate.markAsDeleted();

            aggregate.restore();

            // then
            then(aggregate.getHookInvocationCount())
                    .as("onMarkedAsDeleted should fire exactly once despite multiple calls")
                    .isEqualTo(1);
            then(aggregate.getRestoredHookCount())
                    .as("onRestored should fire exactly once")
                    .isEqualTo(1);

            // when & then - second restore should throw because aggregate is no longer restorable
            thenThrownBy(aggregate::restore)
                    .as("Second restore should throw BusinessValidationException")
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessage("Aggregate is not restorable");

            // then - counts should remain unchanged after failed restore attempt
            then(aggregate.getHookInvocationCount())
                    .as("onMarkedAsDeleted count should remain unchanged")
                    .isEqualTo(1);
            then(aggregate.getRestoredHookCount())
                    .as("onRestored count should remain unchanged after failed restore")
                    .isEqualTo(1);
        }

        @Test
        void hooks_ShouldResetProperly_WhenFullCycleCompleted() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();
            aggregate.restore();

            // then
            then(aggregate.isDeleted()).isFalse();
            then(aggregate.isRestorable()).isFalse();
            then(aggregate.getHookInvocationCount()).isEqualTo(1);
            then(aggregate.getRestoredHookCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should contain class name, id, and deleted status")
        void toString_ShouldContainAllFields_WhenCalled() {
            // given
            TestSoftDeletableAggregate aggregate = createAggregate();

            // when
            String result = aggregate.toString();

            // then
            then(result)
                    .as("Should contain class name")
                    .contains("TestSoftDeletableAggregate")
                    .as("Should contain id")
                    .contains("id=" + AGGREGATE_ID)
                    .as("Should contain deleted status")
                    .contains("deleted=false");
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should support method chaining")
        void builder_ShouldSupportMethodChaining_WhenCalled() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);

            // when
            TestSoftDeletableAggregate.TestBuilder builder =
                    new TestSoftDeletableAggregate.TestBuilder()
                            .identifier(id)
                            .deleted(false);

            // then
            then(builder)
                    .as("Builder should support method chaining")
                    .isNotNull();
        }
    }

    private TestSoftDeletableAggregate createAggregate() {
        return new TestSoftDeletableAggregate(new TestIdentifier(AGGREGATE_ID));
    }

    // ========================================================================================
    // Test Doubles
    // ========================================================================================

    private record TestIdentifier(String value) implements Identifier<String> {

        @Override
        public @NonNull String value() {
            return this.value;
        }
    }

    private static class TestSoftDeletableAggregate
            extends AbstractSoftDeletableAggregateRoot<TestIdentifier> {

        @Serial
        private static final long serialVersionUID = 5185849174136277871L;

        /**
         * Counts invocations of the {@code onMarkedAsDeleted} hook so tests
         * can verify the idempotency guard in the base class.
         */
        private int markedAsDeletedHookCount;

        /**
         * Counts invocations of the {@code onRestored} hook.
         */
        private int restoredHookCount;

        TestSoftDeletableAggregate(TestIdentifier id) {
            super(id);
        }

        TestSoftDeletableAggregate(TestBuilder builder) {
            super(builder);
        }

        @Override
        public void onMarkedAsDeleted() {
            this.markedAsDeletedHookCount++;
        }

        @Override
        public void onRestored() {
            this.restoredHookCount++;
        }

        int getHookInvocationCount() {
            return this.markedAsDeletedHookCount;
        }

        int getRestoredHookCount() {
            return this.restoredHookCount;
        }

        static class TestBuilder
                extends AbstractBuilder<TestIdentifier,
                TestSoftDeletableAggregate, TestBuilder> {

            @Override
            protected TestBuilder self() {
                return this;
            }

            @Override
            public TestSoftDeletableAggregate build() {
                return new TestSoftDeletableAggregate(this);
            }
        }
    }
}
