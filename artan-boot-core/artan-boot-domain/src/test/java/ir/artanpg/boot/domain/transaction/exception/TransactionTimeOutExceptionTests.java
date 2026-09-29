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

package ir.artanpg.boot.domain.transaction.exception;

import ir.artanpg.boot.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link TransactionTimeOutException}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("TransactionTimedOutException")
class TransactionTimeOutExceptionTests {

    private static final String MESSAGE = "Transaction timed out";
    private static final String MESSAGE_CODE = "TX_TIMEOUT_001";
    private static final String[] MESSAGE_ARGS = {"30s"};

    @Nested
    @DisplayName("Constructor with message")
    class ConstructorWithMessage {

        @Test
        @DisplayName("should create exception with message")
        void constructor_ShouldCreateException_WhenMessageProvided() {
            // given
            // nothing

            // when
            TransactionTimeOutException exception = new TransactionTimeOutException(MESSAGE);

            // then
            then(exception)
                    .as("Exception should not be null")
                    .isNotNull();
            then(exception.getMessage())
                    .as("Message should match")
                    .isEqualTo(MESSAGE);
            then(exception)
                    .as("Should be instance of TransactionException")
                    .isInstanceOf(TransactionException.class);
            then(exception)
                    .as("Should be instance of DomainException")
                    .isInstanceOf(DomainException.class);
        }
    }

    @Nested
    @DisplayName("Constructor with messageCode and message")
    class ConstructorWithMessageCodeAndMessage {

        @Test
        @DisplayName("should create exception with messageCode and message")
        void constructor_ShouldCreateException_WhenMessageCodeAndMessageProvided() {
            // given
            // nothing

            // when
            TransactionTimeOutException exception = new TransactionTimeOutException(MESSAGE_CODE, MESSAGE);

            // then
            then(exception)
                    .as("Exception should not be null")
                    .isNotNull();
            then(exception.getMessage())
                    .as("Message should match")
                    .isEqualTo(MESSAGE);
            then(exception.getMessageCode())
                    .as("MessageCode should match")
                    .isEqualTo(MESSAGE_CODE);
        }
    }

    @Nested
    @DisplayName("Constructor with messageCode, messageArgs and message")
    class ConstructorWithMessageCodeArgsAndMessage {

        @Test
        @DisplayName("should create exception with messageCode, messageArgs and message")
        void constructor_ShouldCreateException_WhenMessageCodeArgsAndMessageProvided() {
            // given
            // nothing

            // when
            TransactionTimeOutException exception =
                    new TransactionTimeOutException(MESSAGE_CODE, MESSAGE_ARGS, MESSAGE);

            // then
            then(exception)
                    .as("Exception should not be null")
                    .isNotNull();
            then(exception.getMessage())
                    .as("Message should match")
                    .isEqualTo(MESSAGE);
            then(exception.getMessageCode())
                    .as("MessageCode should match")
                    .isEqualTo(MESSAGE_CODE);
            then(exception.getMessageArgs())
                    .as("MessageArgs should match")
                    .containsExactly(MESSAGE_ARGS);
        }
    }

    @Nested
    @DisplayName("Constructor with message and cause")
    class ConstructorWithMessageAndCause {

        @Test
        @DisplayName("should create exception with message and cause")
        void constructor_ShouldCreateException_WhenMessageAndCauseProvided() {
            // given
            Throwable cause = new IllegalStateException("Root cause");

            // when
            TransactionTimeOutException exception = new TransactionTimeOutException(MESSAGE, cause);

            // then
            then(exception)
                    .as("Exception should not be null")
                    .isNotNull();
            then(exception.getMessage())
                    .as("Message should match")
                    .isEqualTo(MESSAGE);
            then(exception.getCause())
                    .as("Cause should match")
                    .isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("Constructor with messageCode, message and cause")
    class ConstructorWithMessageCodeMessageAndCause {

        @Test
        @DisplayName("should create exception with messageCode, message and cause")
        void constructor_ShouldCreateException_WhenMessageCodeMessageAndCauseProvided() {
            // given
            Throwable cause = new IllegalStateException("Root cause");

            // when
            TransactionTimeOutException exception = new TransactionTimeOutException(MESSAGE_CODE, MESSAGE, cause);

            // then
            then(exception)
                    .as("Exception should not be null")
                    .isNotNull();
            then(exception.getMessage())
                    .as("Message should match")
                    .isEqualTo(MESSAGE);
            then(exception.getMessageCode())
                    .as("MessageCode should match")
                    .isEqualTo(MESSAGE_CODE);
            then(exception.getCause())
                    .as("Cause should match")
                    .isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("Constructor with messageCode, messageArgs, message and cause")
    class ConstructorWithMessageCodeArgsMessageAndCause {

        @Test
        @DisplayName("should create exception with messageCode, messageArgs, message and cause")
        void constructor_ShouldCreateException_WhenMessageCodeArgsMessageAndCauseProvided() {
            // given
            Throwable cause = new IllegalStateException("Root cause");

            // when
            TransactionTimeOutException exception =
                    new TransactionTimeOutException(MESSAGE_CODE, MESSAGE_ARGS, MESSAGE, cause);

            // then
            then(exception)
                    .as("Exception should not be null")
                    .isNotNull();
            then(exception.getMessage())
                    .as("Message should match")
                    .isEqualTo(MESSAGE);
            then(exception.getMessageCode())
                    .as("MessageCode should match")
                    .isEqualTo(MESSAGE_CODE);
            then(exception.getMessageArgs())
                    .as("MessageArgs should match")
                    .containsExactly(MESSAGE_ARGS);
            then(exception.getCause())
                    .as("Cause should match")
                    .isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("Inheritance")
    class Inheritance {

        @Test
        @DisplayName("should be instance of TransactionException")
        void inheritance_ShouldBeInstanceOfTransactionException_WhenCreated() {
            // given
            // nothing

            // when
            TransactionTimeOutException exception = new TransactionTimeOutException(MESSAGE);

            // then
            then(exception)
                    .as("Should be instance of TransactionException")
                    .isInstanceOf(TransactionException.class);
        }

        @Test
        @DisplayName("should be instance of DomainException")
        void inheritance_ShouldBeInstanceOfDomainException_WhenCreated() {
            // given
            // nothing

            // when
            TransactionTimeOutException exception = new TransactionTimeOutException(MESSAGE);

            // then
            then(exception)
                    .as("Should be instance of DomainException")
                    .isInstanceOf(DomainException.class);
        }

        @Test
        @DisplayName("should be instance of RuntimeException")
        void inheritance_ShouldBeInstanceOfRuntimeException_WhenCreated() {
            // given
            // nothing

            // when
            TransactionTimeOutException exception = new TransactionTimeOutException(MESSAGE);

            // then
            then(exception)
                    .as("Should be instance of RuntimeException")
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
