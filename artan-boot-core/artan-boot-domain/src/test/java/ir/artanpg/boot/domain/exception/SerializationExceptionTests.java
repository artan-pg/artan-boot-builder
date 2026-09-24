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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link SerializationException}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("Serialization Exception Tests")
class SerializationExceptionTests {

    @Test
    @DisplayName("Should set message when only message is provided")
    void Constructor_ShouldSetMessage_WhenMessageIsPassed() {
        // given
        String message = "Test serialization error message";

        // when
        SerializationException exception = new SerializationException(message);

        // then
        then(exception).isInstanceOf(DomainException.class);
        then(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should set message code and message when both are provided")
    void Constructor_ShouldSetMessageCodeAndMessage_WhenMessageCodeAndMessageArePassed() {
        // given
        String messageCode = "EVT_001";
        String message = "Test serialization error message";

        // when
        SerializationException exception = new SerializationException(messageCode, message);

        // then
        then(exception.getMessage()).isEqualTo(message);
        then(exception.getMessageCode()).isEqualTo(messageCode);
    }

    @Test
    @DisplayName("Should set message code, args, and message when all are provided")
    void Constructor_ShouldSetMessageCodeArgsAndMessage_WhenMessageCodeArgsAndMessageArePassed() {
        // given
        String messageCode = "EVT_002";
        String[] messageArgs = {"arg1", "arg2"};
        String message = "Test serialization error message with args";

        // when
        SerializationException exception = new SerializationException(messageCode, messageArgs, message);

        // then
        then(exception.getMessage()).isEqualTo(message);
        then(exception.getMessageCode()).isEqualTo(messageCode);
        then(exception.getMessageArgs()).isEqualTo(messageArgs);
    }

    @Test
    @DisplayName("Should set message and cause when both are provided")
    void Constructor_ShouldSetMessageAndCause_WhenMessageAndCauseArePassed() {
        // given
        String message = "Test serialization error message";
        Throwable cause = new RuntimeException("Root cause");

        // when
        SerializationException exception = new SerializationException(message, cause);

        // then
        then(exception.getMessage()).isEqualTo(message);
        then(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should set message code, message, and cause when all are provided")
    void Constructor_ShouldSetMessageCodeMessageAndCause_WhenMessageCodeMessageAndCauseArePassed() {
        // given
        String messageCode = "EVT_003";
        String message = "Test serialization error message";
        Throwable cause = new RuntimeException("Root cause");

        // when
        SerializationException exception = new SerializationException(messageCode, message, cause);

        // then
        then(exception.getMessage()).isEqualTo(message);
        then(exception.getMessageCode()).isEqualTo(messageCode);
        then(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should set message code, args, message, and cause when all are provided")
    void Constructor_ShouldSetMessageCodeArgsMessageAndCause_WhenMessageCodeArgsMessageAndCauseArePassed() {
        // given
        String messageCode = "EVT_004";
        String[] messageArgs = {"arg1", "arg2"};
        String message = "Test serialization error message with args and cause";
        Throwable cause = new RuntimeException("Root cause");

        // when
        SerializationException exception = new SerializationException(messageCode, messageArgs, message, cause);

        // then
        then(exception.getMessage()).isEqualTo(message);
        then(exception.getMessageCode()).isEqualTo(messageCode);
        then(exception.getMessageArgs()).isEqualTo(messageArgs);
        then(exception.getCause()).isEqualTo(cause);
    }
}
