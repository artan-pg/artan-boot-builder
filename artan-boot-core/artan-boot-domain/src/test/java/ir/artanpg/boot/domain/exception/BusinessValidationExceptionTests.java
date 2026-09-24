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

package ir.artanpg.boot.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link BusinessValidationException}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("BusinessValidationException")
class BusinessValidationExceptionTests {

    private static final String SIMPLE_ERROR_MESSAGE = "Use case execution failed";
    private static final String SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N = "validation.email.format";
    private static final String SIMPLE_ERROR_MESSAGE_CAUSE = "Root cause";
    private static final String VALIDATION_ERROR_MESSAGE = "Validation failed";
    private static final String BUILDER_IS_NULL_MESSAGE = "Message is required for BusinessValidationException";

    private static final String EIGHTEEN_MESSAGE_ARG = "18";
    private static final String ONE_HUNDRED_MESSAGE_ARG = "100";

    private static final String EMAIL_FIELD_NAME = "email";
    private static final String INVALID_EMAIL_FIELD_NAME = "invalid-email";

    @Nested
    @DisplayName("Constructor(String)")
    class ConstructorWithMessage {

        @Test
        @DisplayName("should set message when message is provided")
        void constructor_ShouldSetMessage_WhenMessageIsProvided() {
            // given
            String message = VALIDATION_ERROR_MESSAGE;

            // when
            BusinessValidationException exception = new BusinessValidationException(message);

            // then
            then(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("should set null fieldName when only message is provided")
        void constructor_ShouldSetNullFieldName_WhenOnlyMessageIsProvided() {
            // given

            // when
            BusinessValidationException exception = new BusinessValidationException(VALIDATION_ERROR_MESSAGE);

            // then
            then(exception.getFieldName()).isNull();
        }

        @Test
        @DisplayName("should set null rejectedValue when only message is provided")
        void constructor_ShouldSetNullRejectedValue_WhenOnlyMessageIsProvided() {
            // given

            // when
            BusinessValidationException exception = new BusinessValidationException(VALIDATION_ERROR_MESSAGE);

            // then
            then(exception.getRejectedValue()).isNull();
        }
    }

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("should create builder when message is provided")
        void builder_ShouldCreateBuilder_WhenMessageIsProvided() {
            // given

            // when
            BusinessValidationException.Builder builder = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE);

            // then
            then(builder).isNotNull();
        }

        @Test
        @DisplayName("should build exception with all fields when all are set")
        void builder_ShouldBuildExceptionWithAllFields_WhenAllAreSet() {
            // given
            String message = VALIDATION_ERROR_MESSAGE;
            String fieldName = EMAIL_FIELD_NAME;
            String rejectedValue = INVALID_EMAIL_FIELD_NAME;
            String messageCode = SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N;
            String[] messageArgs = {ONE_HUNDRED_MESSAGE_ARG};
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            BusinessValidationException exception = BusinessValidationException.builder(message)
                    .fieldName(fieldName)
                    .rejectedValue(rejectedValue)
                    .messageCode(messageCode)
                    .messageArgs(messageArgs)
                    .cause(cause)
                    .build();

            // then
            then(exception.getMessage()).isEqualTo(message);
            then(exception.getFieldName()).isEqualTo(fieldName);
            then(exception.getRejectedValue()).isEqualTo(rejectedValue);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).containsExactly(ONE_HUNDRED_MESSAGE_ARG);
            then(exception.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("should build exception with only message when no other fields are set")
        void builder_ShouldBuildExceptionWithOnlyMessage_WhenNoOtherFieldsAreSet() {
            // given
            String message = VALIDATION_ERROR_MESSAGE;

            // when
            BusinessValidationException exception = BusinessValidationException.builder(message)
                    .build();

            // then
            then(exception.getMessage()).isEqualTo(message);
            then(exception.getFieldName()).isNull();
            then(exception.getRejectedValue()).isNull();
            then(exception.getMessageCode()).isNull();
            then(exception.getMessageArgs()).isNull();
            then(exception.getCause()).isNull();
        }

        @SuppressWarnings({"ConstantValue", "ThrowableNotThrown"})
        @Test
        @DisplayName("should throw IllegalStateException when message is null")
        void builder_ShouldThrowIllegalStateException_WhenMessageIsNull() {
            // given
            String nullMessage = null;

            // when & then
            thenThrownBy(() -> BusinessValidationException.builder(nullMessage).build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BUILDER_IS_NULL_MESSAGE);
        }

        @SuppressWarnings("ThrowableNotThrown")
        @Test
        @DisplayName("should throw IllegalStateException when message is empty")
        void builder_ShouldThrowIllegalStateException_WhenMessageIsEmpty() {
            // given
            String emptyMessage = "";

            // when & then
            thenThrownBy(() -> BusinessValidationException.builder(emptyMessage).build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(BUILDER_IS_NULL_MESSAGE);
        }

        @Test
        @DisplayName("should accept non-empty message")
        void builder_ShouldAcceptNonEmptyMessage_WhenProvided() {
            // given
            String message = "a";

            // when
            BusinessValidationException exception = BusinessValidationException.builder(message)
                    .build();

            // then
            then(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("should set fieldName when provided")
        void builder_ShouldSetFieldName_WhenProvided() {
            // given
            String fieldName = EMAIL_FIELD_NAME;

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .fieldName(fieldName)
                    .build();

            // then
            then(exception.getFieldName()).isEqualTo(fieldName);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should accept null fieldName")
        void builder_ShouldAcceptNullFieldName_WhenProvided() {
            // given
            String fieldName = null;

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .fieldName(fieldName)
                    .build();

            // then
            then(exception.getFieldName()).isNull();
        }

        @Test
        @DisplayName("should set rejectedValue when provided as String")
        void builder_ShouldSetRejectedValue_WhenProvidedAsString() {
            // given
            String rejectedValue = INVALID_EMAIL_FIELD_NAME;

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .rejectedValue(rejectedValue)
                    .build();

            // then
            then(exception.getRejectedValue()).isEqualTo(rejectedValue);
        }

        @Test
        @DisplayName("should set rejectedValue when provided as Integer")
        void builder_ShouldSetRejectedValue_WhenProvidedAsInteger() {
            // given
            Integer rejectedValue = 15;

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .rejectedValue(rejectedValue)
                    .build();

            // then
            then(exception.getRejectedValue()).isEqualTo(rejectedValue);
        }

        @Test
        @DisplayName("should set rejectedValue when provided as LocalDate")
        void builder_ShouldSetRejectedValue_WhenProvidedAsLocalDate() {
            // given
            LocalDate rejectedValue = LocalDate.of(2020, 1, 1);

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .rejectedValue(rejectedValue)
                    .build();

            // then
            then(exception.getRejectedValue()).isEqualTo(rejectedValue);
        }

        @Test
        @DisplayName("should accept null rejectedValue")
        void builder_ShouldAcceptNullRejectedValue_WhenProvided() {
            // given

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .rejectedValue(null)
                    .build();

            // then
            then(exception.getRejectedValue()).isNull();
        }

        @Test
        @DisplayName("should set messageCode when provided")
        void builder_ShouldSetMessageCode_WhenProvided() {
            // given
            String messageCode = SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N;

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .messageCode(messageCode)
                    .build();

            // then
            then(exception.getMessageCode()).isEqualTo(messageCode);
        }

        @Test
        @DisplayName("should set messageArgs when provided")
        void builder_ShouldSetMessageArgs_WhenProvided() {
            // given
            String[] messageArgs = {EIGHTEEN_MESSAGE_ARG, ONE_HUNDRED_MESSAGE_ARG};

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .messageArgs(messageArgs)
                    .build();

            // then
            then(exception.getMessageArgs()).containsExactly(EIGHTEEN_MESSAGE_ARG, ONE_HUNDRED_MESSAGE_ARG);
        }

        @Test
        @DisplayName("should accept empty messageArgs array")
        void builder_ShouldAcceptEmptyMessageArgsArray_WhenProvided() {
            // given
            String[] messageArgs = {};

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .messageArgs(messageArgs)
                    .build();

            // then
            then(exception.getMessageArgs()).isEmpty();
        }

        @Test
        @DisplayName("should set cause when provided")
        void builder_ShouldSetCause_WhenProvided() {
            // given
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .cause(cause)
                    .build();

            // then
            then(exception.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("should accept null cause")
        void builder_ShouldAcceptNullCause_WhenProvided() {
            // given

            // when
            BusinessValidationException exception = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .cause(null)
                    .build();

            // then
            then(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("should support method chaining")
        void builder_ShouldSupportMethodChaining_WhenCalled() {
            // given

            // when
            BusinessValidationException.Builder builder = BusinessValidationException.builder(VALIDATION_ERROR_MESSAGE)
                    .fieldName(EMAIL_FIELD_NAME)
                    .rejectedValue("invalid")
                    .messageCode("validation.email")
                    .messageArgs("arg1")
                    .cause(new RuntimeException());

            // then
            then(builder).isNotNull();
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class Inheritance {

        @Test
        @DisplayName("should be instance of DomainException")
        void inheritance_ShouldBeInstanceOfDomainException_WhenCreated() {
            // given
            BusinessValidationException exception = new BusinessValidationException(SIMPLE_ERROR_MESSAGE);

            // when & then
            then(exception).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("should be instance of RuntimeException")
        void inheritance_ShouldBeInstanceOfRuntimeException_WhenCreated() {
            // given
            BusinessValidationException exception = new BusinessValidationException(SIMPLE_ERROR_MESSAGE);

            // when & then
            then(exception).isInstanceOf(RuntimeException.class);
        }
    }
}
