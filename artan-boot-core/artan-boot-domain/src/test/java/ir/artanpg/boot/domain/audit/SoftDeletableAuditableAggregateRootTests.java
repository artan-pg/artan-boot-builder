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

package ir.artanpg.boot.domain.audit;

import ir.artanpg.boot.domain.exception.BusinessValidationException;
import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serial;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenCode;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link AbstractSoftDeletableAuditableAggregateRoot}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("AbstractSoftDeletableAuditableAggregateRoot")
class SoftDeletableAuditableAggregateRootTests {

    private static final String AGGREGATE_ID = "aggregate-1";
    private static final String USER_1 = "user-1";
    private static final String USER_2 = "user-2";
    private static final String BLANK_STRING = "   ";
    private static final String ERROR_NOT_RESTORABLE = "Aggregate is not restorable";
    private static final String ERROR_DELETED_BY_NULL_OR_BLANK = "The deletedBy cannot be null or blank";

    @Nested
    @DisplayName("Constructor with Identifier and Audit Fields")
    class ConstructorWithIdentifierAndAuditFields {

        @Test
        @DisplayName("should initialize deleted as false when created")
        void constructor_ShouldInitializeDeletedAsFalse_WhenCreated() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);

            // when
            TestSoftDelAuditableAggregate aggregate = new TestSoftDelAuditableAggregate(id, USER_1, createdDate);

            // then
            then(aggregate.isDeleted())
                    .as("Should not be deleted initially")
                    .isFalse();
            then(aggregate.getDeletedBy())
                    .as("DeletedBy should be null initially")
                    .isNull();
            then(aggregate.getDeletedDate())
                    .as("DeletedDate should be null initially")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Constructor with Builder")
    class ConstructorWithBuilder {

        @Test
        @DisplayName("should set all fields from builder when provided")
        void constructor_ShouldSetAllFields_WhenBuilderProvided() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant deletedDate = Instant.now();

            TestSoftDelAuditableAggregate.TestBuilder builder =
                    new TestSoftDelAuditableAggregate.TestBuilder()
                            .identifier(id)
                            .createdBy(USER_1)
                            .createdDate(createdDate)
                            .deleted(true)
                            .deletedBy(USER_2)
                            .deletedDate(deletedDate);

            // when
            TestSoftDelAuditableAggregate aggregate = new TestSoftDelAuditableAggregate(builder);

            // then
            then(aggregate.isDeleted())
                    .as("Deleted should match builder")
                    .isTrue();
            then(aggregate.getDeletedBy())
                    .as("DeletedBy should match builder")
                    .isEqualTo(USER_2);
            then(aggregate.getDeletedDate())
                    .as("DeletedDate should match builder")
                    .isEqualTo(deletedDate);
        }
    }

    @Nested
    @DisplayName("markAsDeleted()")
    class MarkAsDeletedNoArgs {

        @Test
        @DisplayName("should set deleted to true when not deleted")
        void markAsDeleted_ShouldSetDeletedToTrue_WhenNotDeleted() {
            // given
            TestSoftDelAuditableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted();

            // then
            then(aggregate.isDeleted()).isTrue();
            then(aggregate.getDeletedBy())
                    .as("DeletedBy should remain null")
                    .isNull();
            then(aggregate.getDeletedDate())
                    .as("DeletedDate should remain null")
                    .isNull();
        }

        @Test
        @DisplayName("should be idempotent when already deleted")
        void markAsDeleted_ShouldBeIdempotent_WhenAlreadyDeleted() {
            // given
            TestSoftDelAuditableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();

            // when
            aggregate.markAsDeleted();

            // then
            then(aggregate.isDeleted())
                    .as("Should remain deleted")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("markAsDeleted(String)")
    class MarkAsDeletedWithDeleter {

        @Test
        @DisplayName("should set all deletion fields when valid deletedBy")
        void markAsDeleted_ShouldSetAllFields_WhenValidDeletedBy() {
            // given
            TestSoftDelAuditableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsDeleted(USER_2);

            // then
            then(aggregate.isDeleted()).isTrue();
            then(aggregate.getDeletedBy())
                    .as("DeletedBy should be set")
                    .isEqualTo(USER_2);
            then(aggregate.getDeletedDate())
                    .as("DeletedDate should be set")
                    .isNotNull()
                    .isBeforeOrEqualTo(Instant.now());
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when deletedBy is null")
        void markAsDeleted_ShouldThrowException_WhenDeletedByIsNull() {
            // given
            TestSoftDelAuditableAggregate aggregate = createAggregate();
            String nullDeletedBy = null;

            // when & then
            thenThrownBy(() -> aggregate.markAsDeleted(nullDeletedBy))
                    .as("Should throw when deletedBy is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_DELETED_BY_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainException when deletedBy is blank")
        void markAsDeleted_ShouldThrowException_WhenDeletedByIsBlank() {
            // given
            TestSoftDelAuditableAggregate aggregate = createAggregate();

            // when & then
            thenThrownBy(() -> aggregate.markAsDeleted(BLANK_STRING))
                    .as("Should throw when deletedBy is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_DELETED_BY_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should be idempotent when already deleted")
        void markAsDeleted_ShouldBeIdempotent_WhenAlreadyDeleted() {
            // given
            TestSoftDelAuditableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted(USER_2);
            Instant firstDeletedDate = aggregate.getDeletedDate();

            // when
            aggregate.markAsDeleted(USER_1);

            // then
            then(aggregate.getDeletedBy())
                    .as("DeletedBy should not change")
                    .isEqualTo(USER_2);
            then(aggregate.getDeletedDate())
                    .as("DeletedDate should not change")
                    .isEqualTo(firstDeletedDate);
        }
    }

    @Nested
    @DisplayName("isRestorable")
    class IsRestorable {

        @Test
        @DisplayName("should return false when not deleted")
        void isRestorable_ShouldReturnFalse_WhenNotDeleted() {
            // given
            TestSoftDelAuditableAggregate aggregate = createAggregate();

            // when & then
            then(aggregate.isRestorable())
                    .as("Should not be restorable")
                    .isFalse();
        }

        @Test
        @DisplayName("should return true when deleted")
        void isRestorable_ShouldReturnTrue_WhenDeleted() {
            // given
            TestSoftDelAuditableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted();

            // when & then
            then(aggregate.isRestorable())
                    .as("Should be restorable")
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
            TestSoftDelAuditableAggregate aggregate = createAggregate();
            aggregate.markAsDeleted(USER_2);

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
            TestSoftDelAuditableAggregate aggregate = createAggregate();

            // when & then
            thenThrownBy(aggregate::restore)
                    .as("Should throw when not restorable")
                    .isInstanceOf(BusinessValidationException.class)
                    .hasMessage(ERROR_NOT_RESTORABLE);
        }
    }

    @Nested
    @DisplayName("validateDeletedBy")
    class ValidateDeletedBy {

        @Test
        @DisplayName("should accept valid deletedBy")
        void validateDeletedBy_ShouldAcceptValidValue_WhenProvided() {
            // given

            // when & then
            thenCode(() ->
                    AbstractSoftDeletableAuditableAggregateRoot.validateDeletedBy(USER_1))
                    .as("Should not throw for valid deletedBy")
                    .doesNotThrowAnyException();
        }

        @SuppressWarnings({"DataFlowIssue", "ConstantValue"})
        @Test
        @DisplayName("should throw DomainException when deletedBy is null")
        void validateDeletedBy_ShouldThrowException_WhenNull() {
            // given
            String nullValue = null;

            // when & then
            thenThrownBy(() ->
                    AbstractSoftDeletableAuditableAggregateRoot.validateDeletedBy(nullValue))
                    .as("Should throw when null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_DELETED_BY_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainException when deletedBy is blank")
        void validateDeletedBy_ShouldThrowException_WhenBlank() {
            // given

            // when & then
            thenThrownBy(() ->
                    AbstractSoftDeletableAuditableAggregateRoot
                            .validateDeletedBy(BLANK_STRING))
                    .as("Should throw when blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_DELETED_BY_NULL_OR_BLANK);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should contain all relevant fields")
        void toString_ShouldContainAllFields_WhenCalled() {
            // given
            TestSoftDelAuditableAggregate aggregate = createAggregate();

            // when
            String result = aggregate.toString();

            // then
            then(result)
                    .as("Should contain class name")
                    .contains("TestSoftDelAuditableAggregate")
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
            TestSoftDelAuditableAggregate.TestBuilder builder =
                    new TestSoftDelAuditableAggregate.TestBuilder()
                            .identifier(id)
                            .createdBy(USER_1)
                            .createdDate(Instant.now())
                            .deleted(true)
                            .deletedBy(USER_2)
                            .deletedDate(Instant.now());

            // then
            then(builder)
                    .as("Builder should support chaining")
                    .isNotNull();
        }
    }

    private TestSoftDelAuditableAggregate createAggregate() {
        TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
        Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);
        return new TestSoftDelAuditableAggregate(id, USER_1, createdDate);
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

    private static class TestSoftDelAuditableAggregate
            extends AbstractSoftDeletableAuditableAggregateRoot<TestIdentifier> {

        @Serial
        private static final long serialVersionUID = -8087032927637639987L;

        TestSoftDelAuditableAggregate(
                TestIdentifier id, String createdBy, Instant createdDate) {
            super(id, createdBy, createdDate);
        }

        TestSoftDelAuditableAggregate(TestBuilder builder) {
            super(builder);
        }

        static class TestBuilder extends AbstractBuilder<TestIdentifier, TestSoftDelAuditableAggregate, TestBuilder> {

            @Override
            protected TestBuilder self() {
                return this;
            }

            @Override
            public TestSoftDelAuditableAggregate build() {
                return new TestSoftDelAuditableAggregate(this);
            }
        }
    }
}
