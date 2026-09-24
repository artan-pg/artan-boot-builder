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

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link DomainException}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("DomainException")
class DomainExceptionTests {

    private static final String SIMPLE_ERROR_MESSAGE = "Test error message";
    private static final String SIMPLE_ERROR_MESSAGE_CAUSE = "Root cause";
    private static final String SIMPLE_ERROR_MESSAGE_CODE = "Message code should be null when not provided";
    private static final String SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N = "error.code.test";
    private static final String SIMPLE_ERROR_MESSAGE_ARGS = "Message args should be null when not provided";
    private static final String SIMPLE_ERROR_MESSAGE_CAUSE_NULL = "Cause should be null when not provided";

    private static final String MESSAGE_ARG_1 = "arg1";
    private static final String MESSAGE_ARG_2 = "arg2";
    private static final String MESSAGE_ARG_3 = "arg3";
    private static final String[] MESSAGE_ARGS = {MESSAGE_ARG_1, MESSAGE_ARG_2};

    @Nested
    @DisplayName("Constructor(String)")
    class ConstructorWithMessage {

        @Test
        @DisplayName("should set message when message is provided")
        void constructor_ShouldSetMessage_WhenMessageIsProvided() {
            // given

            // when
            DomainException exception = new DomainException(SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getMessage()).isEqualTo(SIMPLE_ERROR_MESSAGE);
        }

        @Test
        @DisplayName("should set null messageCode when only message is provided")
        void constructor_ShouldSetNullMessageCode_WhenOnlyMessageIsProvided() {
            // given

            // when
            DomainException exception = new DomainException(SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getMessageCode())
                    .as(SIMPLE_ERROR_MESSAGE_CODE)
                    .isNull();
        }

        @Test
        @DisplayName("should set null messageArgs when only message is provided")
        void constructor_ShouldSetNullMessageArgs_WhenOnlyMessageIsProvided() {
            // given

            // when
            DomainException exception = new DomainException(SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getMessageArgs())
                    .as(SIMPLE_ERROR_MESSAGE_ARGS)
                    .isNull();
        }

        @Test
        @DisplayName("should set null cause when only message is provided")
        void constructor_ShouldSetNullCause_WhenOnlyMessageIsProvided() {
            // given

            // when
            DomainException exception = new DomainException(SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getCause())
                    .as(SIMPLE_ERROR_MESSAGE_CAUSE_NULL)
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Constructor(String messageCode, String message)")
    class ConstructorWithMessageCodeAndMessage {

        @Test
        @DisplayName("should set messageCode and message when both are provided")
        void constructor_ShouldSetMessageCodeAndMessage_WhenBothAreProvided() {
            // given
            String messageCode = SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N;

            // when
            DomainException exception = new DomainException(messageCode, SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessage()).isEqualTo(SIMPLE_ERROR_MESSAGE);
        }

        @Test
        @DisplayName("should set null messageArgs when messageCode and message are provided")
        void constructor_ShouldSetNullMessageArgs_WhenMessageCodeAndMessageAreProvided() {
            // given

            // when
            DomainException exception =
                    new DomainException(SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N, SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getMessageArgs())
                    .as(SIMPLE_ERROR_MESSAGE_ARGS)
                    .isNull();
        }

        @Test
        @DisplayName("should set null cause when messageCode and message are provided")
        void constructor_ShouldSetNullCause_WhenMessageCodeAndMessageAreProvided() {
            // given

            // when
            DomainException exception =
                    new DomainException(SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N, SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getCause())
                    .as(SIMPLE_ERROR_MESSAGE_CAUSE_NULL)
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Constructor(String messageCode, String[] messageArgs, String message)")
    class ConstructorWithFullMessageDetails {

        @Test
        @DisplayName("should set all message details when all are provided")
        void constructor_ShouldSetAllMessageDetails_WhenAllAreProvided() {
            // given
            String messageCode = SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N;

            // when
            DomainException exception = new DomainException(messageCode, MESSAGE_ARGS, SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).containsExactly(MESSAGE_ARG_1, MESSAGE_ARG_2);
            then(exception.getMessage()).isEqualTo(SIMPLE_ERROR_MESSAGE);
        }

        @Test
        @DisplayName("should set null cause when full message details are provided")
        void constructor_ShouldSetNullCause_WhenFullMessageDetailsAreProvided() {
            // given
            String[] messageArgs = {MESSAGE_ARG_1};

            // when
            DomainException exception =
                    new DomainException(SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N, messageArgs, SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getCause())
                    .as(SIMPLE_ERROR_MESSAGE_CAUSE_NULL)
                    .isNull();
        }

        @Test
        @DisplayName("should accept empty messageArgs array")
        void constructor_ShouldAcceptEmptyMessageArgsArray_WhenProvided() {
            // given
            String[] messageArgs = {};

            // when
            DomainException exception =
                    new DomainException(SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N, messageArgs, SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getMessageArgs()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Constructor(String message, Throwable cause)")
    class ConstructorWithMessageAndCause {

        @Test
        @DisplayName("should set message and cause when both are provided")
        void constructor_ShouldSetMessageAndCause_WhenBothAreProvided() {
            // given
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            DomainException exception = new DomainException(SIMPLE_ERROR_MESSAGE, cause);

            // then
            then(exception.getMessage())
                    .isEqualTo(SIMPLE_ERROR_MESSAGE);

            then(exception.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("should set null messageCode when message and cause are provided")
        void constructor_ShouldSetNullMessageCode_WhenMessageAndCauseAreProvided() {
            // given
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            DomainException exception = new DomainException(SIMPLE_ERROR_MESSAGE, cause);

            // then
            then(exception.getMessageCode())
                    .as(SIMPLE_ERROR_MESSAGE_CODE)
                    .isNull();
        }

        @Test
        @DisplayName("should set null messageArgs when message and cause are provided")
        void constructor_ShouldSetNullMessageArgs_WhenMessageAndCauseAreProvided() {
            // given
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            DomainException exception = new DomainException(SIMPLE_ERROR_MESSAGE, cause);

            // then
            then(exception.getMessageArgs())
                    .as(SIMPLE_ERROR_MESSAGE_ARGS)
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Constructor(String messageCode, String message, Throwable cause)")
    class ConstructorWithMessageCodeMessageAndCause {

        @Test
        @DisplayName("should set all fields when messageCode, message, and cause are provided")
        void constructor_ShouldSetAllFields_WhenMessageCodeMessageAndCauseAreProvided() {
            // given
            String messageCode = SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N;
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            DomainException exception = new DomainException(messageCode, SIMPLE_ERROR_MESSAGE, cause);

            // then
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessage()).isEqualTo(SIMPLE_ERROR_MESSAGE);
            then(exception.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("should set null messageArgs when messageCode, message, and cause are provided")
        void constructor_ShouldSetNullMessageArgs_WhenMessageCodeMessageAndCauseAreProvided() {
            // given
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            DomainException exception =
                    new DomainException(SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N, SIMPLE_ERROR_MESSAGE, cause);

            // then
            then(exception.getMessageArgs())
                    .as(SIMPLE_ERROR_MESSAGE_ARGS)
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Constructor(String messageCode, String[] messageArgs, String message, Throwable cause)")
    class ConstructorWithAllFields {

        @Test
        @DisplayName("should set all fields when all are provided")
        void constructor_ShouldSetAllFields_WhenAllAreProvided() {
            // given
            String messageCode = SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N;
            String[] messageArgs = {MESSAGE_ARG_1, MESSAGE_ARG_1, MESSAGE_ARG_3};
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            DomainException exception = new DomainException(messageCode, messageArgs, SIMPLE_ERROR_MESSAGE, cause);

            // then
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).containsExactly(MESSAGE_ARG_1, MESSAGE_ARG_1, MESSAGE_ARG_3);
            then(exception.getMessage()).isEqualTo(SIMPLE_ERROR_MESSAGE);
            then(exception.getCause()).isSameAs(cause);
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should accept null messageCode")
        void constructor_ShouldAcceptNullMessageCode_WhenProvided() {
            // given
            String messageCode = null;
            String[] messageArgs = {MESSAGE_ARG_1};
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            DomainException exception = new DomainException(messageCode, messageArgs, SIMPLE_ERROR_MESSAGE, cause);

            // then
            then(exception.getMessageCode()).isNull();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should accept null messageArgs")
        void constructor_ShouldAcceptNullMessageArgs_WhenProvided() {
            // given
            String[] messageArgs = null;
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            DomainException exception =
                    new DomainException(
                            SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N, messageArgs, SIMPLE_ERROR_MESSAGE, cause);

            // then
            then(exception.getMessageArgs()).isNull();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("should accept null cause")
        void constructor_ShouldAcceptNullCause_WhenProvided() {
            // given
            String[] messageArgs = {MESSAGE_ARG_1};
            Throwable cause = null;

            // when
            DomainException exception =
                    new DomainException(
                            SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N, messageArgs, SIMPLE_ERROR_MESSAGE, cause);

            // then
            then(exception.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class Inheritance {

        @Test
        @DisplayName("should be instance of RuntimeException")
        void inheritance_ShouldBeInstanceOfRuntimeException_WhenCreated() {
            // given
            DomainException exception = new DomainException(SIMPLE_ERROR_MESSAGE);

            // when & then
            then(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should be throwable")
        void inheritance_ShouldBeThrowable_WhenCreated() {
            // given
            DomainException exception = new DomainException(SIMPLE_ERROR_MESSAGE);

            // when & then
            then(exception).isInstanceOf(Throwable.class);
        }
    }
}
