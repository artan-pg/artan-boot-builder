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

        TestSoftDeletableAggregate(TestIdentifier id) {
            super(id);
        }

        TestSoftDeletableAggregate(TestBuilder builder) {
            super(builder);
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
