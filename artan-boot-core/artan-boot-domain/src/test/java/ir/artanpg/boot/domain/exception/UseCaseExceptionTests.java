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
 * Unit tests for {@link UseCaseException}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("UseCaseException")
class UseCaseExceptionTests {

    private static final String SIMPLE_ERROR_MESSAGE = "Use case execution failed";
    private static final String SIMPLE_ERROR_MESSAGE_CODE_FORMATED_I18N = "usecase.error.test";
    private static final String SIMPLE_ERROR_MESSAGE_CAUSE = "Root cause";
    private static final String PARAM_1 = "param1";
    private static final String PARAM_2 = "param2";

    @Nested
    @DisplayName("Constructor(String)")
    class ConstructorWithMessage {

        @Test
        @DisplayName("should set message when message is provided")
        void constructor_ShouldSetMessage_WhenMessageIsProvided() {
            // given
            String message = SIMPLE_ERROR_MESSAGE;

            // when
            UseCaseException exception = new UseCaseException(message);

            // then
            then(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("should set null messageCode when only message is provided")
        void constructor_ShouldSetNullMessageCode_WhenOnlyMessageIsProvided() {
            // given

            // when
            UseCaseException exception = new UseCaseException(SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getMessageCode()).isNull();
        }

        @Test
        @DisplayName("should set null messageArgs when only message is provided")
        void constructor_ShouldSetNullMessageArgs_WhenOnlyMessageIsProvided() {
            // given

            // when
            UseCaseException exception = new UseCaseException(SIMPLE_ERROR_MESSAGE);

            // then
            then(exception.getMessageArgs()).isNull();
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
            String message = SIMPLE_ERROR_MESSAGE;

            // when
            UseCaseException exception = new UseCaseException(messageCode, message);

            // then
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessage()).isEqualTo(message);
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
            String[] messageArgs = {PARAM_1, PARAM_2};
            String message = SIMPLE_ERROR_MESSAGE;

            // when
            UseCaseException exception = new UseCaseException(messageCode, messageArgs, message);

            // then
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).containsExactly(PARAM_1, PARAM_2);
            then(exception.getMessage()).isEqualTo(message);
        }
    }

    @Nested
    @DisplayName("Constructor(String message, Throwable cause)")
    class ConstructorWithMessageAndCause {

        @Test
        @DisplayName("should set message and cause when both are provided")
        void constructor_ShouldSetMessageAndCause_WhenBothAreProvided() {
            // given
            String message = SIMPLE_ERROR_MESSAGE;
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            UseCaseException exception = new UseCaseException(message, cause);

            // then
            then(exception.getMessage()).isEqualTo(message);
            then(exception.getCause()).isSameAs(cause);
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
            String message = SIMPLE_ERROR_MESSAGE;
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            UseCaseException exception = new UseCaseException(messageCode, message, cause);

            // then
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessage()).isEqualTo(message);
            then(exception.getCause()).isSameAs(cause);
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
            String[] messageArgs = {PARAM_1, PARAM_2};
            String message = SIMPLE_ERROR_MESSAGE;
            Throwable cause = new RuntimeException(SIMPLE_ERROR_MESSAGE_CAUSE);

            // when
            UseCaseException exception = new UseCaseException(messageCode, messageArgs, message, cause);

            // then
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).containsExactly(PARAM_1, PARAM_2);
            then(exception.getMessage()).isEqualTo(message);
            then(exception.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class Inheritance {

        @Test
        @DisplayName("should be instance of DomainException")
        void inheritance_ShouldBeInstanceOfDomainException_WhenCreated() {
            // given
            UseCaseException exception = new UseCaseException(SIMPLE_ERROR_MESSAGE);

            // when & then
            then(exception).isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("should be instance of RuntimeException")
        void inheritance_ShouldBeInstanceOfRuntimeException_WhenCreated() {
            // given
            UseCaseException exception = new UseCaseException(SIMPLE_ERROR_MESSAGE);

            // when & then
            then(exception).isInstanceOf(RuntimeException.class);
        }
    }
}
