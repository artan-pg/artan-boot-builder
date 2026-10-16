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

package ir.artanpg.boot.domain.exception.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link TransactionalDomainException}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("TransactionalDomainException")
class TransactionalDomainExceptionTests {

    @Nested
    @DisplayName("TransactionalDomainException(String)")
    class ConstructorWithMessage {

        @Test
        @DisplayName("Sets the message and leaves code, args and cause unset")
        void Constructor_ShouldSetMessageAndLeaveOthersUnset_WhenMessageProvided() {
            // given
            String message = "transaction failed";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(message);

            // then
            then(ex.getMessage())
                    .as("getMessage must return the exact message passed to the constructor")
                    .isEqualTo(message);
            then(ex.getMessageCode())
                    .as("messageCode must remain null when only a message is provided")
                    .isNull();
            then(ex.getMessageArgs())
                    .as("messageArgs must remain null when only a message is provided")
                    .isNull();
            then(ex.getCause())
                    .as("cause must remain null when only a message is provided")
                    .isNull();
        }

        @Test
        @DisplayName("Accepts a null message without throwing")
        void Constructor_ShouldAcceptNullMessage_WhenNullProvided() {
            // given

            // when
            TransactionalDomainException ex = new TransactionalDomainException(null);

            // then
            then(ex.getMessage())
                    .as("getMessage must return null when the constructor was given null")
                    .isNull();
            then(ex.getMessageCode())
                    .as("messageCode must remain null regardless of the message")
                    .isNull();
        }

        @Test
        @DisplayName("Accepts an empty message without throwing")
        void Constructor_ShouldAcceptEmptyMessage_WhenEmptyStringProvided() {
            // given
            String message = "";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(message);

            // then
            then(ex.getMessage())
                    .as("getMessage must return the empty string verbatim")
                    .isEmpty();
        }

        @Test
        @DisplayName("Accepts a blank message without throwing")
        void Constructor_ShouldAcceptBlankMessage_WhenBlankStringProvided() {
            // given
            String message = "   ";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(message);

            // then
            then(ex.getMessage())
                    .as("getMessage must return the blank string verbatim")
                    .isEqualTo("   ");
        }
    }

    @Nested
    @DisplayName("TransactionalDomainException(String, String)")
    class ConstructorWithMessageCodeAndMessage {

        @Test
        @DisplayName("Sets the message code and message, leaves args and cause unset")
        void Constructor_ShouldSetMessageCodeAndMessageAndLeaveOthersUnset_WhenBothProvided() {
            // given
            String messageCode = "TX-001";
            String message = "transaction rolled back";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(messageCode, message);

            // then
            then(ex.getMessageCode())
                    .as("getMessageCode must return the exact code passed to the constructor")
                    .isEqualTo(messageCode);
            then(ex.getMessage())
                    .as("getMessage must return the exact message passed to the constructor")
                    .isEqualTo(message);
            then(ex.getMessageArgs())
                    .as("messageArgs must remain null when not provided")
                    .isNull();
            then(ex.getCause())
                    .as("cause must remain null when not provided")
                    .isNull();
        }

        @Test
        @DisplayName("Accepts a null message code without throwing")
        void Constructor_ShouldAcceptNullMessageCode_WhenNullProvided() {
            // given
            String message = "transaction rolled back";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(null, message);

            // then
            then(ex.getMessageCode())
                    .as("getMessageCode must return null when null was passed")
                    .isNull();
            then(ex.getMessage())
                    .as("getMessage must still return the supplied message")
                    .isEqualTo(message);
        }

        @Test
        @DisplayName("Accepts an empty message code without throwing")
        void Constructor_ShouldAcceptEmptyMessageCode_WhenEmptyStringProvided() {
            // given
            String messageCode = "";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(messageCode, "message");

            // then
            then(ex.getMessageCode())
                    .as("getMessageCode must return the empty string verbatim")
                    .isEmpty();
        }

        @Test
        @DisplayName("Accepts a blank message code without throwing")
        void Constructor_ShouldAcceptBlankMessageCode_WhenBlankStringProvided() {
            // given
            String messageCode = "   ";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(messageCode, "message");

            // then
            then(ex.getMessageCode())
                    .as("getMessageCode must return the blank string verbatim")
                    .isEqualTo("   ");
        }
    }

    @Nested
    @DisplayName("TransactionalDomainException(String, String[], String)")
    class ConstructorWithMessageCodeArgsAndMessage {

        @Test
        @DisplayName("Sets the message code, args and message, leaves cause unset")
        void Constructor_ShouldSetCodeArgsAndMessageAndLeaveCauseUnset_WhenAllProvided() {
            // given
            String messageCode = "TX-002";
            String[] messageArgs = {"order-1", "42"};
            String message = "transaction failed for order";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(messageCode, messageArgs, message);

            // then
            then(ex.getMessageCode())
                    .as("getMessageCode must return the exact code passed to the constructor")
                    .isEqualTo(messageCode);
            then(ex.getMessageArgs())
                    .as("getMessageArgs must return the same arguments as were passed")
                    .containsExactly(messageArgs);
            then(ex.getMessage())
                    .as("getMessage must return the exact message passed to the constructor")
                    .isEqualTo(message);
            then(ex.getCause())
                    .as("cause must remain null when not provided")
                    .isNull();
        }

        @Test
        @DisplayName("Accepts a null message args array without throwing")
        void Constructor_ShouldAcceptNullMessageArgs_WhenNullProvided() {
            // given

            // when
            TransactionalDomainException ex = new TransactionalDomainException("TX-003", null, "message");

            // then
            then(ex.getMessageArgs())
                    .as("getMessageArgs must return null when the constructor was given null")
                    .isNull();
        }

        @Test
        @DisplayName("Accepts an empty message args array without throwing")
        void Constructor_ShouldAcceptEmptyMessageArgs_WhenEmptyArrayProvided() {
            // given
            String[] messageArgs = {};

            // when
            TransactionalDomainException ex = new TransactionalDomainException("TX-004", messageArgs, "message");

            // then
            then(ex.getMessageArgs())
                    .as("getMessageArgs must return an empty array when an empty array was passed")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("TransactionalDomainException(String, Throwable)")
    class ConstructorWithMessageAndCause {

        @Test
        @DisplayName("Sets the message and cause, leaves code and args unset")
        void Constructor_ShouldSetMessageAndCauseAndLeaveOthersUnset_WhenBothProvided() {
            // given
            String message = "wrapping failure";
            Throwable cause = new IllegalStateException("root cause");

            // when
            TransactionalDomainException ex = new TransactionalDomainException(message, cause);

            // then
            then(ex.getMessage())
                    .as("getMessage must return the exact message passed to the constructor")
                    .isEqualTo(message);
            then(ex.getCause())
                    .as("getCause must return the exact cause instance passed to the constructor")
                    .isSameAs(cause);
            then(ex.getMessageCode())
                    .as("messageCode must remain null when not provided")
                    .isNull();
            then(ex.getMessageArgs())
                    .as("messageArgs must remain null when not provided")
                    .isNull();
        }

        @Test
        @DisplayName("Accepts a null cause without throwing")
        void Constructor_ShouldAcceptNullCause_WhenNullProvided() {
            // given

            // when
            TransactionalDomainException ex = new TransactionalDomainException("message", (Throwable) null);

            // then
            then(ex.getCause())
                    .as("getCause must return null when the constructor was given null")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("TransactionalDomainException(String, String, Throwable)")
    class ConstructorWithMessageCodeMessageAndCause {

        @Test
        @DisplayName("Sets the message code, message and cause, leaves args unset")
        void Constructor_ShouldSetCodeMessageAndCauseAndLeaveArgsUnset_WhenAllProvided() {
            // given
            String messageCode = "TX-005";
            String message = "transaction commit failed";
            Throwable cause = new RuntimeException("db error");

            // when
            TransactionalDomainException ex = new TransactionalDomainException(messageCode, message, cause);

            // then
            then(ex.getMessageCode())
                    .as("getMessageCode must return the exact code passed to the constructor")
                    .isEqualTo(messageCode);
            then(ex.getMessage())
                    .as("getMessage must return the exact message passed to the constructor")
                    .isEqualTo(message);
            then(ex.getCause())
                    .as("getCause must return the exact cause instance passed to the constructor")
                    .isSameAs(cause);
            then(ex.getMessageArgs())
                    .as("messageArgs must remain null when not provided")
                    .isNull();
        }

        @Test
        @DisplayName("Accepts a null message code and null cause without throwing")
        void Constructor_ShouldAcceptNullCodeAndNullCause_WhenBothNull() {
            // given
            String message = "transaction failed";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(null, message, null);

            // then
            then(ex.getMessageCode())
                    .as("getMessageCode must return null when null was passed")
                    .isNull();
            then(ex.getCause())
                    .as("getCause must return null when null was passed")
                    .isNull();
            then(ex.getMessage())
                    .as("getMessage must still return the supplied message")
                    .isEqualTo(message);
        }
    }

    @Nested
    @DisplayName("TransactionalDomainException(String, String[], String, Throwable)")
    class ConstructorWithMessageCodeArgsMessageAndCause {

        @Test
        @DisplayName("Sets all four parameters")
        void Constructor_ShouldSetAllParameters_WhenAllProvided() {
            // given
            String messageCode = "TX-006";
            String[] messageArgs = {"tenant-a", "order-7"};
            String message = "transaction failed for tenant";
            Throwable cause = new IllegalStateException("timeout");

            // when
            TransactionalDomainException ex =
                    new TransactionalDomainException(messageCode, messageArgs, message, cause);

            // then
            then(ex.getMessageCode())
                    .as("getMessageCode must return the exact code passed to the constructor")
                    .isEqualTo(messageCode);
            then(ex.getMessageArgs())
                    .as("getMessageArgs must return the same arguments as were passed")
                    .containsExactly(messageArgs);
            then(ex.getMessage())
                    .as("getMessage must return the exact message passed to the constructor")
                    .isEqualTo(message);
            then(ex.getCause())
                    .as("getCause must return the exact cause instance passed to the constructor")
                    .isSameAs(cause);
        }

        @Test
        @DisplayName("Accepts null message args and null cause without throwing")
        void Constructor_ShouldAcceptNullArgsAndNullCause_WhenBothNull() {
            // given
            String messageCode = "TX-007";
            String message = "transaction failed";

            // when
            TransactionalDomainException ex = new TransactionalDomainException(messageCode, null, message, null);

            // then
            then(ex.getMessageArgs())
                    .as("getMessageArgs must return null when null was passed")
                    .isNull();
            then(ex.getCause())
                    .as("getCause must return null when null was passed")
                    .isNull();
            then(ex.getMessageCode())
                    .as("getMessageCode must still return the supplied code")
                    .isEqualTo(messageCode);
            then(ex.getMessage())
                    .as("getMessage must still return the supplied message")
                    .isEqualTo(message);
        }

        @Test
        @DisplayName("Accepts an empty message args array without throwing")
        void Constructor_ShouldAcceptEmptyArgs_WhenEmptyArrayProvided() {
            // given
            String[] messageArgs = {};

            // when
            TransactionalDomainException ex =
                    new TransactionalDomainException("TX-008", messageArgs, "message", new RuntimeException("cause"));

            // then
            then(ex.getMessageArgs())
                    .as("getMessageArgs must return an empty array when an empty array was passed")
                    .isEmpty();
        }
    }
}
