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
 * Unit tests for {@link AbstractAuditableAggregateRoot}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("AbstractAuditableAggregateRoot")
class AuditableAggregateRootTests {

    private static final String AGGREGATE_ID = "aggregate-1";
    private static final String USER_1 = "user-1";
    private static final String USER_2 = "user-2";
    private static final String USER_3 = "user-3";
    private static final String BLANK_STRING = "   ";
    private static final String ERROR_CREATED_BY_NULL_OR_BLANK = "The createdBy cannot be null or blank";
    private static final String ERROR_CREATED_DATE_NULL = "The createdDate cannot be null";
    private static final String ERROR_CREATED_DATE_AFTER_NOW = "The createdDate cannot be after now";
    private static final String ERROR_LAST_MODIFIED_BY_NULL_OR_BLANK = "The lastModifiedBy cannot be null or blank";
    private static final String ERROR_LAST_MODIFIED_DATE_NULL = "The lastModifiedDate cannot be null";
    private static final String ERROR_LAST_MODIFIED_DATE_AFTER_NOW = "The lastModifiedDate cannot be after now";

    @Nested
    @DisplayName("Constructor with Identifier and Audit Fields")
    class ConstructorWithIdentifierAndAuditFields {

        @Test
        @DisplayName("should set all fields when valid parameters are provided")
        void constructor_ShouldSetAllFields_WhenValidParametersAreProvided() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);

            // when
            TestAuditableAggregate aggregate = new TestAuditableAggregate(
                    id, USER_1, createdDate);

            // then
            then(aggregate.getId())
                    .as("Aggregate id should match the provided identifier")
                    .isEqualTo(id);
            then(aggregate.getCreatedBy())
                    .as("Created by should match the provided value")
                    .isEqualTo(USER_1);
            then(aggregate.getCreatedDate())
                    .as("Created date should match the provided value")
                    .isEqualTo(createdDate);
            then(aggregate.getLastModifiedBy())
                    .as("Last modified by should be null initially")
                    .isNull();
            then(aggregate.getLastModifiedDate())
                    .as("Last modified date should be null initially")
                    .isNull();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when createdBy is null")
        void constructor_ShouldThrowDomainException_WhenCreatedByIsNull() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            String nullCreatedBy = null;
            Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);

            // when & then
            thenThrownBy(() -> new TestAuditableAggregate(id, nullCreatedBy, createdDate))
                    .as("Should throw DomainException when createdBy is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_BY_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainException when createdBy is blank")
        void constructor_ShouldThrowDomainException_WhenCreatedByIsBlank() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);

            // when & then
            thenThrownBy(() -> new TestAuditableAggregate(id, BLANK_STRING, createdDate))
                    .as("Should throw DomainException when createdBy is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_BY_NULL_OR_BLANK);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when createdDate is null")
        void constructor_ShouldThrowDomainException_WhenCreatedDateIsNull() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant nullCreatedDate = null;

            // when & then
            thenThrownBy(() -> new TestAuditableAggregate(id, USER_1, nullCreatedDate))
                    .as("Should throw DomainException when createdDate is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_DATE_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when createdDate is in the future")
        void constructor_ShouldThrowDomainException_WhenCreatedDateIsInTheFuture() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant futureCreatedDate = Instant.now().plus(1, ChronoUnit.DAYS);

            // when & then
            thenThrownBy(() -> new TestAuditableAggregate(id, USER_1, futureCreatedDate))
                    .as("Should throw DomainException when createdDate is in the future")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_DATE_AFTER_NOW);
        }
    }

    @Nested
    @DisplayName("Constructor with Builder")
    class ConstructorWithBuilder {

        @Test
        @DisplayName("should set all fields when valid builder is provided")
        void constructor_ShouldSetAllFields_WhenValidBuilderIsProvided() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant lastModifiedDate = Instant.now();

            TestAuditableAggregate.TestAuditableAggregateBuilder builder =
                    new TestAuditableAggregate.TestAuditableAggregateBuilder()
                            .identifier(id)
                            .createdBy(USER_1)
                            .createdDate(createdDate)
                            .lastModifiedBy(USER_2)
                            .lastModifiedDate(lastModifiedDate);

            // when
            TestAuditableAggregate aggregate = new TestAuditableAggregate(builder);

            // then
            then(aggregate.getId())
                    .as("Aggregate id should match the builder's identifier")
                    .isEqualTo(id);
            then(aggregate.getCreatedBy())
                    .as("Created by should match the builder's value")
                    .isEqualTo(USER_1);
            then(aggregate.getCreatedDate())
                    .as("Created date should match the builder's value")
                    .isEqualTo(createdDate);
            then(aggregate.getLastModifiedBy())
                    .as("Last modified by should match the builder's value")
                    .isEqualTo(USER_2);
            then(aggregate.getLastModifiedDate())
                    .as("Last modified date should match the builder's value")
                    .isEqualTo(lastModifiedDate);
        }

        @Test
        @DisplayName("should throw DomainException when builder has invalid createdBy")
        void constructor_ShouldThrowDomainException_WhenBuilderHasInvalidCreatedBy() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);

            TestAuditableAggregate.TestAuditableAggregateBuilder builder =
                    new TestAuditableAggregate.TestAuditableAggregateBuilder()
                            .identifier(id)
                            .createdBy(BLANK_STRING)
                            .createdDate(createdDate);

            // when & then
            thenThrownBy(() -> new TestAuditableAggregate(builder))
                    .as("Should throw DomainException when builder has invalid createdBy")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_BY_NULL_OR_BLANK);
        }
    }

    @Nested
    @DisplayName("markAsModified")
    class MarkAsModified {

        @Test
        @DisplayName("should update lastModifiedBy and lastModifiedDate when valid modifiedBy")
        void markAsModified_ShouldUpdateFields_WhenValidModifiedByIsProvided() {
            // given
            TestAuditableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsModified(USER_2);

            // then
            then(aggregate.getLastModifiedBy())
                    .as("Last modified by should be updated")
                    .isEqualTo(USER_2);
            then(aggregate.getLastModifiedDate())
                    .as("Last modified date should be updated")
                    .isNotNull()
                    .isBeforeOrEqualTo(Instant.now());
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when modifiedBy is null")
        void markAsModified_ShouldThrowDomainException_WhenModifiedByIsNull() {
            // given
            TestAuditableAggregate aggregate = createAggregate();
            String nullModifiedBy = null;

            // when & then
            thenThrownBy(() -> aggregate.markAsModified(nullModifiedBy))
                    .as("Should throw DomainException when modifiedBy is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_BY_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should throw DomainException when modifiedBy is blank")
        void markAsModified_ShouldThrowDomainException_WhenModifiedByIsBlank() {
            // given
            TestAuditableAggregate aggregate = createAggregate();

            // when & then
            thenThrownBy(() -> aggregate.markAsModified(BLANK_STRING))
                    .as("Should throw DomainException when modifiedBy is blank")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_BY_NULL_OR_BLANK);
        }

        @Test
        @DisplayName("should update lastModifiedDate on each call")
        void markAsModified_ShouldUpdateLastModifiedDate_OnEachCall() {
            // given
            TestAuditableAggregate aggregate = createAggregate();

            // when
            aggregate.markAsModified(USER_2);
            Instant firstModifiedDate = aggregate.getLastModifiedDate();

            // then
            then(firstModifiedDate)
                    .as("First modification date should be set")
                    .isNotNull();

            // when - second modification
            aggregate.markAsModified(USER_3);
            Instant secondModifiedDate = aggregate.getLastModifiedDate();

            // then
            then(secondModifiedDate)
                    .as("Second modification date should be after first")
                    .isAfterOrEqualTo(firstModifiedDate);
            then(aggregate.getLastModifiedBy())
                    .as("Last modified by should be updated to latest")
                    .isEqualTo(USER_3);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        @DisplayName("should return string representation with all audit fields")
        void toString_ShouldReturnStringRepresentation_WhenCalled() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);
            TestAuditableAggregate aggregate = new TestAuditableAggregate(
                    id, USER_1, createdDate);

            // when
            String result = aggregate.toString();

            // then
            then(result)
                    .as("toString should contain class name")
                    .contains("TestAuditableAggregate")
                    .as("toString should contain id")
                    .contains("id=" + AGGREGATE_ID)
                    .as("toString should contain createdBy")
                    .contains("createdBy='" + USER_1 + "'")
                    .as("toString should contain createdDate")
                    .contains("createdDate=");
        }
    }

    @Nested
    @DisplayName("Validation Methods")
    class ValidationMethods {

        @Test
        @DisplayName("should accept valid createdBy")
        void validateCreatedBy_ShouldAcceptValidValue_WhenProvided() {
            // when & then
            thenCode(() -> AbstractAuditableAggregateRoot.validateCreatedBy(USER_1))
                    .as("Should not throw exception for valid createdBy")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept valid createdDate")
        void validateCreatedDate_ShouldAcceptValidValue_WhenProvided() {
            // given
            Instant validCreatedDate = Instant.now().minus(1, ChronoUnit.DAYS);

            // when & then
            thenCode(() -> AbstractAuditableAggregateRoot.validateCreatedDate(validCreatedDate))
                    .as("Should not throw exception for valid createdDate")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept valid lastModifiedBy")
        void validateLastModifiedBy_ShouldAcceptValidValue_WhenProvided() {
            // when & then
            thenCode(() -> AbstractAuditableAggregateRoot.validateLastModifiedBy(USER_2))
                    .as("Should not throw exception for valid lastModifiedBy")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept valid lastModifiedDate")
        void validateLastModifiedDate_ShouldAcceptValidValue_WhenProvided() {
            // given
            Instant validLastModifiedDate = Instant.now().minus(1, ChronoUnit.HOURS);

            // when & then
            thenCode(() -> AbstractAuditableAggregateRoot.validateLastModifiedDate(validLastModifiedDate))
                    .as("Should not throw exception for valid lastModifiedDate")
                    .doesNotThrowAnyException();
        }

        @SuppressWarnings({"ConstantValue", "DataFlowIssue"})
        @Test
        @DisplayName("should throw DomainException when lastModifiedDate is null")
        void validateLastModifiedDate_ShouldThrowDomainException_WhenLastModifiedDateIsNull() {
            // given
            Instant nullLastModifiedDate = null;

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(nullLastModifiedDate))
                    .as("Should throw DomainException when lastModifiedDate is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_NULL);
        }

        @Test
        @DisplayName("should throw DomainException when lastModifiedDate is in the future")
        void validateLastModifiedDate_ShouldThrow_WhenLastModifiedDateIsInTheFuture() {
            // given
            Instant futureLastModifiedDate = Instant.now().plus(1, ChronoUnit.DAYS);

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(
                            futureLastModifiedDate))
                    .as("Should throw when lastModifiedDate is in the future")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_AFTER_NOW);
        }
    }

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        @DisplayName("should have correct constant values")
        void constants_ShouldHaveCorrectValues_WhenAccessed() {
            // when & then
            then(AbstractAuditableAggregateRoot.CREATED_BY)
                    .as("CREATED_BY constant should be correct")
                    .isEqualTo("createdBy");
            then(AbstractAuditableAggregateRoot.CREATED_DATE)
                    .as("CREATED_DATE constant should be correct")
                    .isEqualTo("createdDate");
            then(AbstractAuditableAggregateRoot.LAST_MODIFIED_BY)
                    .as("LAST_MODIFIED_BY constant should be correct")
                    .isEqualTo("lastModifiedBy");
            then(AbstractAuditableAggregateRoot.LAST_MODIFIED_DATE)
                    .as("LAST_MODIFIED_DATE constant should be correct")
                    .isEqualTo("lastModifiedDate");
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
            TestAuditableAggregate.TestAuditableAggregateBuilder builder =
                    new TestAuditableAggregate.TestAuditableAggregateBuilder()
                            .identifier(id)
                            .createdBy(USER_1)
                            .createdDate(Instant.now().minus(1, ChronoUnit.DAYS))
                            .lastModifiedBy(USER_2)
                            .lastModifiedDate(Instant.now());

            // then
            then(builder)
                    .as("Builder should support method chaining")
                    .isNotNull();
        }
    }

    // ========================================================================================
    // Helper Methods
    // ========================================================================================

    private TestAuditableAggregate createAggregate() {
        TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
        Instant createdDate = Instant.now().minus(1, ChronoUnit.DAYS);
        return new TestAuditableAggregate(id, USER_1, createdDate);
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

    private static class TestAuditableAggregate
            extends AbstractAuditableAggregateRoot<TestIdentifier> {

        @Serial
        private static final long serialVersionUID = -8698846257496573492L;

        TestAuditableAggregate(TestIdentifier id, String createdBy, Instant createdDate) {
            super(id, createdBy, createdDate);
        }

        TestAuditableAggregate(TestAuditableAggregateBuilder builder) {
            super(builder);
        }

        static class TestAuditableAggregateBuilder
                extends AbstractBuilder<TestIdentifier, TestAuditableAggregate,
                TestAuditableAggregateBuilder> {

            @Override
            protected TestAuditableAggregateBuilder self() {
                return this;
            }

            @Override
            public TestAuditableAggregate build() {
                return new TestAuditableAggregate(this);
            }
        }
    }
}
