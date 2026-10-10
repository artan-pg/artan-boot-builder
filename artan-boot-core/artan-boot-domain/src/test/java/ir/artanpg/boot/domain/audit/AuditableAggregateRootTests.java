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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

        @Test
        @DisplayName("should set createdDate from Clock when Clock constructor is used")
        void constructor_ShouldSetCreatedDateFromClock_WhenClockProvided() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant fixed = Instant.parse("2026-01-15T10:00:00Z");
            Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);

            // when
            TestAuditableAggregate aggregate = new TestAuditableAggregate(id, USER_1, clock);

            // then
            then(aggregate.getCreatedBy())
                    .as("CreatedBy should match the provided value")
                    .isEqualTo(USER_1);
            then(aggregate.getCreatedDate())
                    .as("CreatedDate should match the fixed Clock instant")
                    .isEqualTo(fixed);
            then(aggregate.getLastModifiedBy())
                    .as("LastModifiedBy should be null initially")
                    .isNull();
            then(aggregate.getLastModifiedDate())
                    .as("LastModifiedDate should be null initially")
                    .isNull();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw DomainException when Clock createdDate is null")
        void constructor_ShouldThrow_WhenClockCreatedDateIsNull() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Clock nullClock = null;

            // when & then
            thenThrownBy(() -> new TestAuditableAggregate(id, USER_1, nullClock))
                    .as("Should throw DomainException when Clock createdDate is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_DATE_NULL);
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

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw DomainException when modifiedBy is null")
        void markAsModified_ShouldThrowDomainException_WhenModifiedByIsNull() {
            // given
            TestAuditableAggregate aggregate = createAggregate();

            // when & then
            thenThrownBy(() -> aggregate.markAsModified(null))
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

        @Test
        @DisplayName("should set lastModifiedDate on first call when only modifiedBy is provided")
        void markAsModified_ShouldSetDate_WhenCalledFirstTimeWithOnlyModifiedBy() {
            // given
            TestAuditableAggregate aggregate = createAggregate();
            then(aggregate.getLastModifiedDate())
                    .as("LastModifiedDate should be null before first modification")
                    .isNull();

            // when
            aggregate.markAsModified(USER_2);

            // then
            then(aggregate.getLastModifiedBy())
                    .as("LastModifiedBy should be updated to the provided user")
                    .isEqualTo(USER_2);
            then(aggregate.getLastModifiedDate())
                    .as("LastModifiedDate should be set on first markAsModified call")
                    .isNotNull()
                    .isBeforeOrEqualTo(Instant.now());
        }

        @Test
        @DisplayName("should use explicit Instant when markAsModified(String, Instant) is called")
        void markAsModified_ShouldUseExplicitInstant_WhenInstantProvided() {
            // given
            TestAuditableAggregate aggregate = createAggregate();
            Instant explicit = Instant.parse("2026-03-01T12:00:00Z");

            // when
            aggregate.markAsModified(USER_2, explicit);

            // then
            then(aggregate.getLastModifiedBy())
                    .as("LastModifiedBy should match the provided user")
                    .isEqualTo(USER_2);
            then(aggregate.getLastModifiedDate())
                    .as("LastModifiedDate should match the explicit Instant")
                    .isEqualTo(explicit);
        }

        @Test
        @DisplayName("should use Clock instant when markAsModified(String, Clock) is called")
        void markAsModified_ShouldUseClockInstant_WhenClockProvided() {
            // given
            TestAuditableAggregate aggregate = createAggregate();
            Instant fixed = Instant.parse("2026-04-01T08:30:00Z");
            Clock clock = Clock.fixed(fixed, ZoneOffset.UTC);

            // when
            aggregate.markAsModified(USER_2, clock);

            // then
            then(aggregate.getLastModifiedBy())
                    .as("LastModifiedBy should match the provided user")
                    .isEqualTo(USER_2);
            then(aggregate.getLastModifiedDate())
                    .as("LastModifiedDate should match the fixed Clock instant")
                    .isEqualTo(fixed);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should fall back to system UTC when Clock is null")
        void markAsModified_ShouldFallBackToSystemUtc_WhenClockIsNull() {
            // given
            TestAuditableAggregate aggregate = createAggregate();
            Clock nullClock = null;

            // when
            aggregate.markAsModified(USER_2, nullClock);

            // then
            then(aggregate.getLastModifiedBy())
                    .as("LastModifiedBy should match the provided user")
                    .isEqualTo(USER_2);
            then(aggregate.getLastModifiedDate())
                    .as("LastModifiedDate should fall back to system UTC when Clock is null")
                    .isNotNull()
                    .isBeforeOrEqualTo(Instant.now());
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw when explicit Instant is null")
        void markAsModified_ShouldThrow_WhenExplicitInstantIsNull() {
            // given
            TestAuditableAggregate aggregate = createAggregate();

            // when & then
            thenThrownBy(() -> aggregate.markAsModified(USER_2, (Instant) null))
                    .as("Should throw DomainException when explicit Instant is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_NULL);
        }

        @Test
        @DisplayName("should throw when explicit Instant is in the future")
        void markAsModified_ShouldThrow_WhenExplicitInstantIsInTheFuture() {
            // given
            TestAuditableAggregate aggregate = createAggregate();
            Instant future = Instant.now().plus(1, ChronoUnit.DAYS);

            // when & then
            thenThrownBy(() -> aggregate.markAsModified(USER_2, future))
                    .as("Should throw DomainException when explicit Instant is in the future")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_AFTER_NOW);
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

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw DomainException when lastModifiedDate is null")
        void validateLastModifiedDate_ShouldThrowDomainException_WhenLastModifiedDateIsNull() {
            // given

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate((Instant) null))
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

        @Test
        @DisplayName("should accept valid Clock when using single-arg lastModifiedDate Clock overload")
        void validateLastModifiedDate_ShouldAcceptValidClock_WhenSingleArgClock() {
            // given
            Clock past = Clock.fixed(Instant.now().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC);

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(past))
                    .as("Should not throw for a valid past lastModifiedDate Clock")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw when lastModifiedDate Clock is null (single-arg Clock overload)")
        void validateLastModifiedDate_ShouldThrow_WhenSingleArgClockIsNull() {
            // given

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate((Clock) null))
                    .as("Should throw DomainException when lastModifiedDate Clock is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_NULL);
        }

        @Test
        @DisplayName("should throw when single-arg lastModifiedDate Clock is in the future")
        void validateLastModifiedDate_ShouldThrow_WhenSingleArgClockIsInTheFuture() {
            // given
            Clock future = Clock.fixed(Instant.now().plus(1, ChronoUnit.DAYS), ZoneOffset.UTC);

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(future))
                    .as("Should throw DomainException when lastModifiedDate Clock is in the future")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_AFTER_NOW);
        }

        @Test
        @DisplayName("should accept when createdDate Clock is before reference Clock")
        void validateCreatedDate_ShouldAccept_WhenClockBeforeReferenceClock() {
            // given
            Clock created = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
            Clock reference = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(created, reference))
                    .as("Should not throw when createdDate Clock is before reference Clock")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw when createdDate Clock is after reference Clock")
        void validateCreatedDate_ShouldThrow_WhenClockAfterReferenceClock() {
            // given
            Clock created = Clock.fixed(Instant.parse("2026-12-01T00:00:00Z"), ZoneOffset.UTC);
            Clock reference = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(created, reference))
                    .as("Should throw DomainException when createdDate Clock is after reference Clock")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_DATE_AFTER_NOW);
        }

        @Test
        @DisplayName("should throw when createdDate Clock is null (two-arg Clock)")
        void validateCreatedDate_ShouldThrow_WhenTwoArgCreatedClockIsNull() {
            // given
            Clock reference = Clock.systemUTC();

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(null, reference))
                    .as("Should throw DomainException when createdDate Clock is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_DATE_NULL);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should treat null reference Clock as system UTC for createdDate")
        void validateCreatedDate_ShouldUseSystemUtc_WhenReferenceClockIsNull() {
            // given
            Clock past = Clock.fixed(Instant.now().minus(1, ChronoUnit.DAYS), ZoneOffset.UTC);
            Clock nullReference = null;

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(past, nullReference))
                    .as("Should not throw when reference Clock is null and createdDate is in the past")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should use system now when reference Instant is null for createdDate")
        void validateCreatedDate_ShouldUseSystemNow_WhenReferenceInstantIsNull() {
            // given
            Instant past = Instant.now().minus(1, ChronoUnit.DAYS);

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(past, null))
                    .as("Should not throw when reference Instant is null and createdDate is in the past")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should use provided reference Instant when non-null for createdDate")
        void validateCreatedDate_ShouldUseProvidedReference_WhenReferenceInstantIsNonNull() {
            // given
            Instant created = Instant.parse("2026-01-01T00:00:00Z");
            Instant reference = Instant.parse("2026-06-01T00:00:00Z");

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(created, reference))
                    .as("Should not throw when createdDate is before non-null reference Instant")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should reject createdDate after non-null reference Instant")
        void validateCreatedDate_ShouldThrow_WhenCreatedAfterNonNullReference() {
            // given
            Instant created = Instant.parse("2026-12-01T00:00:00Z");
            Instant reference = Instant.parse("2026-01-01T00:00:00Z");

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(created, reference))
                    .as("Should throw DomainException when createdDate is after non-null reference Instant")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_DATE_AFTER_NOW);
        }

        @Test
        @DisplayName("should throw when createdDate is null even if reference Instant is null")
        void validateCreatedDate_ShouldThrow_WhenCreatedInstantIsNullAndReferenceIsNull() {
            // given

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(null, (Instant) null))
                    .as("Should throw DomainException when createdDate Instant is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_DATE_NULL);
        }

        @Test
        @DisplayName("should accept when createdDate equals non-null reference Instant")
        void validateCreatedDate_ShouldAccept_WhenCreatedEqualsReference() {
            // given
            Instant same = Instant.parse("2026-03-15T12:00:00Z");

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(same, same))
                    .as("Should not throw when createdDate equals reference Instant")
                    .doesNotThrowAnyException();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should throw when both createdDate and reference Clocks are null")
        void validateCreatedDate_ShouldThrow_WhenBothClocksAreNull() {
            // given
            Clock nullCreated = null;
            Clock nullReference = null;

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateCreatedDate(nullCreated, nullReference))
                    .as("Should throw DomainException when both createdDate and reference Clocks are null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_CREATED_DATE_NULL);
        }

        @Test
        @DisplayName("should accept when lastModifiedDate Clock is before reference Clock")
        void validateLastModifiedDate_ShouldAccept_WhenClockBeforeReferenceClock() {
            // given
            Clock modified = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
            Clock reference = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(modified, reference))
                    .as("Should not throw when lastModifiedDate Clock is before reference Clock")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw when lastModifiedDate Clock is after reference Clock")
        void validateLastModifiedDate_ShouldThrow_WhenClockAfterReferenceClock() {
            // given
            Clock modified = Clock.fixed(Instant.parse("2026-12-01T00:00:00Z"), ZoneOffset.UTC);
            Clock reference = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(modified, reference))
                    .as("Should throw DomainException when lastModifiedDate Clock is after reference Clock")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_AFTER_NOW);
        }

        @Test
        @DisplayName("should throw when lastModifiedDate Clock is null (two-arg Clock)")
        void validateLastModifiedDate_ShouldThrow_WhenTwoArgModifiedClockIsNull() {
            // given
            Clock reference = Clock.systemUTC();

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(null, reference))
                    .as("Should throw DomainException when lastModifiedDate Clock is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_NULL);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should treat null reference Clock as system UTC for lastModifiedDate")
        void validateLastModifiedDate_ShouldUseSystemUtc_WhenReferenceClockIsNull() {
            // given
            Clock past = Clock.fixed(Instant.now().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC);
            Clock nullReference = null;

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(past, nullReference))
                    .as("Should not throw when reference Clock is null and lastModifiedDate is in the past")
                    .doesNotThrowAnyException();
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw when both lastModifiedDate and reference Clocks are null")
        void validateLastModifiedDate_ShouldThrow_WhenBothClocksAreNull() {
            // given

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot
                            .validateLastModifiedDate(null, (Clock) null))
                    .as("Should throw DomainException when both lastModifiedDate and reference Clocks are null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_NULL);
        }

        @Test
        @DisplayName("should accept when lastModifiedDate Instant equals reference Instant")
        void validateLastModifiedDate_ShouldAccept_WhenInstantEqualsReference() {
            // given
            Instant same = Instant.parse("2026-03-15T12:00:00Z");

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(same, same))
                    .as("Should not throw when lastModifiedDate equals reference Instant")
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should accept when lastModifiedDate Instant is before reference Instant")
        void validateLastModifiedDate_ShouldAccept_WhenInstantBeforeReference() {
            // given
            Instant modified = Instant.parse("2026-01-01T00:00:00Z");
            Instant reference = Instant.parse("2026-06-01T00:00:00Z");

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(modified, reference))
                    .as("Should not throw when lastModifiedDate is before reference Instant")
                    .doesNotThrowAnyException();
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw when lastModifiedDate Instant is null (two-arg Instant)")
        void validateLastModifiedDate_ShouldThrow_WhenTwoArgModifiedInstantIsNull() {
            // given
            Instant reference = Instant.now();

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot
                            .validateLastModifiedDate(null, reference))
                    .as("Should throw DomainException when lastModifiedDate Instant is null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_NULL);
        }

        @Test
        @DisplayName("should throw when lastModifiedDate Instant is after reference Instant")
        void validateLastModifiedDate_ShouldThrow_WhenInstantAfterReference() {
            // given
            Instant modified = Instant.parse("2026-12-01T00:00:00Z");
            Instant reference = Instant.parse("2026-01-01T00:00:00Z");

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(modified, reference))
                    .as("Should throw DomainException when lastModifiedDate is after reference Instant")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_AFTER_NOW);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should treat null reference Instant as system now for lastModifiedDate")
        void validateLastModifiedDate_ShouldUseSystemNow_WhenReferenceInstantIsNull() {
            // given
            Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant nullReference = null;

            // when & then
            thenCode(() ->
                    AbstractAuditableAggregateRoot.validateLastModifiedDate(past, nullReference))
                    .as("Should not throw when reference Instant is null and lastModifiedDate is in the past")
                    .doesNotThrowAnyException();
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw when both lastModifiedDate and reference Instants are null")
        void validateLastModifiedDate_ShouldThrow_WhenBothInstantsAreNull() {
            // given

            // when & then
            thenThrownBy(() ->
                    AbstractAuditableAggregateRoot
                            .validateLastModifiedDate(null, (Instant) null))
                    .as("Should throw DomainException when both lastModifiedDate and reference Instants are null")
                    .isInstanceOf(DomainException.class)
                    .hasMessage(ERROR_LAST_MODIFIED_DATE_NULL);
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

        @Test
        @DisplayName("should accept createdDate and lastModifiedDate as Clock")
        void builder_ShouldAcceptClockTimestamps_WhenProvided() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant createdFixed = Instant.parse("2026-01-01T00:00:00Z");
            Instant modifiedFixed = Instant.parse("2026-01-02T00:00:00Z");

            // when
            TestAuditableAggregate aggregate =
                    new TestAuditableAggregate.TestAuditableAggregateBuilder()
                            .identifier(id)
                            .createdBy(USER_1)
                            .createdDate(Clock.fixed(createdFixed, ZoneOffset.UTC))
                            .lastModifiedBy(USER_2)
                            .lastModifiedDate(Clock.fixed(modifiedFixed, ZoneOffset.UTC))
                            .build();

            // then
            then(aggregate.getCreatedDate()).isEqualTo(createdFixed);
            then(aggregate.getLastModifiedDate()).isEqualTo(modifiedFixed);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should fall back to system UTC when createdDate Clock is null")
        void builder_ShouldFallBackToSystemUtc_WhenCreatedDateClockIsNull() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Clock nullClock = null;

            // when
            TestAuditableAggregate aggregate =
                    new TestAuditableAggregate.TestAuditableAggregateBuilder()
                            .identifier(id)
                            .createdBy(USER_1)
                            .createdDate(nullClock)
                            .build();

            // then
            then(aggregate.getCreatedDate())
                    .isNotNull()
                    .isBeforeOrEqualTo(Instant.now());
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should fall back to system UTC when lastModifiedDate Clock is null")
        void builder_ShouldFallBackToSystemUtc_WhenLastModifiedDateClockIsNull() {
            // given
            TestIdentifier id = new TestIdentifier(AGGREGATE_ID);
            Instant created = Instant.now().minus(1, ChronoUnit.DAYS);
            Clock nullClock = null;

            // when
            TestAuditableAggregate aggregate =
                    new TestAuditableAggregate.TestAuditableAggregateBuilder()
                            .identifier(id)
                            .createdBy(USER_1)
                            .createdDate(created)
                            .lastModifiedBy(USER_2)
                            .lastModifiedDate(nullClock)
                            .build();

            // then
            then(aggregate.getLastModifiedDate())
                    .isNotNull()
                    .isBeforeOrEqualTo(Instant.now());
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

        TestAuditableAggregate(TestIdentifier id, String createdBy, Clock createdDate) {
            super(id, createdBy, createdDate);
        }

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
