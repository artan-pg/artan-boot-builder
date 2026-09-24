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
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

class InterceptorVetoExceptionTests {

    @Nested
    @DisplayName("Constructor with message")
    class ConstructorWithMessageTests {

        @Test
        void constructor_ShouldCreateExceptionWithMessage_WhenMessageProvided() {
            // given
            String message = "Interceptor vetoed the operation";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(message);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
        }

        @Test
        void constructor_ShouldThrowExceptionWithCorrectMessage_WhenThrown() {
            // given
            String message = "Operation blocked by interceptor";

            // when & then
            thenThrownBy(() -> {
                throw new InterceptorVetoException(message);
            })
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage(message);
        }

        @Test
        void constructor_ShouldHandleNullMessage_WhenNullProvided() {
            // given

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
        }

        @Test
        void constructor_ShouldHandleEmptyMessage_WhenEmptyProvided() {
            // given
            String emptyMessage = "";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(emptyMessage);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(emptyMessage);
        }
    }

    @Nested
    @DisplayName("Constructor with messageCode and message")
    class ConstructorWithMessageCodeTests {

        @Test
        void constructor_ShouldCreateExceptionWithMessageCodeAndMessage_WhenBothProvided() {
            // given
            String messageCode = "INTERCEPTOR_VETO";
            String message = "Operation vetoed by security interceptor";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, message);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
        }

        @Test
        void constructor_ShouldThrowExceptionWithCorrectCodeAndMessage_WhenThrown() {
            // given
            String messageCode = "VALIDATION_FAILED";
            String message = "Validation interceptor rejected the request";

            // when & then
            thenThrownBy(() -> {
                throw new InterceptorVetoException(messageCode, message);
            })
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage(message)
                    .satisfies(ex -> {
                        InterceptorVetoException ive = (InterceptorVetoException) ex;
                        then(ive.getMessageCode()).isEqualTo(messageCode);
                    });
        }

        @Test
        void constructor_ShouldHandleNullMessageCode_WhenNullProvided() {
            // given
            String message = "Some message";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null, message);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isNull();
        }

        @Test
        void constructor_ShouldHandleNullMessage_WhenNullProvided() {
            // given
            String messageCode = "SOME_CODE";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, (String) null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
            then(exception.getMessageCode()).isEqualTo(messageCode);
        }

        @Test
        void constructor_ShouldHandleBothNull_WhenBothNullProvided() {
            // given

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null, (String) null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
            then(exception.getMessageCode()).isNull();
        }
    }

    @Nested
    @DisplayName("Constructor with messageCode, messageArgs and message")
    class ConstructorWithMessageArgsTests {

        @Test
        void constructor_ShouldCreateExceptionWithAllFields_WhenAllProvided() {
            // given
            String messageCode = "INTERCEPTOR_VETO_WITH_ARGS";
            String[] messageArgs = {"arg1", "123", "arg3"};
            String message = "Operation vetoed with arguments";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, messageArgs, message);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).isEqualTo(messageArgs);
        }

        @Test
        void constructor_ShouldThrowExceptionWithAllFields_WhenThrown() {
            // given
            String messageCode = "MULTI_ARG_VETO";
            String[] messageArgs = {"user", "admin", "42"};
            String message = "Interceptor blocked operation for user";

            // when & then
            thenThrownBy(() -> {
                throw new InterceptorVetoException(messageCode, messageArgs, message);
            })
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage(message)
                    .satisfies(ex -> {
                        InterceptorVetoException ive = (InterceptorVetoException) ex;
                        then(ive.getMessageCode()).isEqualTo(messageCode);
                        then(ive.getMessageArgs()).containsExactly("user", "admin", "42");
                    });
        }

        @Test
        void constructor_ShouldHandleNullMessageArgs_WhenNullProvided() {
            // given
            String messageCode = "NULL_ARGS_CODE";
            String message = "Message with null args";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, null, message);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).isNull();
        }

        @Test
        void constructor_ShouldHandleEmptyMessageArgs_WhenEmptyArrayProvided() {
            // given
            String messageCode = "EMPTY_ARGS_CODE";
            String[] emptyMessageArgs = new String[0];
            String message = "Message with empty args";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, emptyMessageArgs, message);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).isEmpty();
        }

        @Test
        void constructor_ShouldHandleSingleArg_WhenSingleArgProvided() {
            // given
            String messageCode = "SINGLE_ARG_CODE";
            String[] singleArg = {"only-arg"};
            String message = "Message with single arg";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, singleArg, message);

            // then
            then(exception).isNotNull();
            then(exception.getMessageArgs()).hasSize(1);
            then(exception.getMessageArgs()).containsExactly("only-arg");
        }

        @Test
        void constructor_ShouldHandleMultipleArgTypes_WhenMixedTypesProvided() {
            // given
            String messageCode = "MIXED_TYPES_CODE";
            String[] mixedArgs = {"string", "123", "45.67", "true", null};
            String message = "Message with mixed types";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, mixedArgs, message);

            // then
            then(exception).isNotNull();
            then(exception.getMessageArgs()).hasSize(5);
            then(exception.getMessageArgs()).containsExactly("string", "123", "45.67", "true", null);
        }

        @Test
        void constructor_ShouldHandleAllNullFields_WhenAllNullProvided() {
            // given

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null, (String[]) null, null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
            then(exception.getMessageCode()).isNull();
            then(exception.getMessageArgs()).isNull();
        }
    }

    @Nested
    @DisplayName("Constructor with message and cause")
    class ConstructorWithMessageAndCauseTests {

        @Test
        void constructor_ShouldCreateExceptionWithMessageAndCause_WhenBothProvided() {
            // given
            String message = "Operation vetoed";
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(message, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldThrowExceptionWithCorrectMessageAndCause_WhenThrown() {
            // given
            String message = "Interceptor blocked the operation";
            Throwable cause = new IllegalStateException("Invalid state");

            // when & then
            thenThrownBy(() -> {
                throw new InterceptorVetoException(message, cause);
            })
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage(message)
                    .hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleNullMessage_WhenNullProvided() {
            // given
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleNullCause_WhenNullProvided() {
            // given
            String message = "Some message";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(message, (Throwable) null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getCause()).isNull();
        }

        @Test
        void constructor_ShouldHandleBothNull_WhenBothNullProvided() {
            // given

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null, (Throwable) null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
            then(exception.getCause()).isNull();
        }

        @Test
        void constructor_ShouldPreserveCauseChain_WhenNestedCauseProvided() {
            // given
            String message = "Top level veto";
            Throwable rootCause = new IllegalArgumentException("Root");
            Throwable intermediateCause = new RuntimeException("Intermediate", rootCause);

            // when
            InterceptorVetoException exception = new InterceptorVetoException(message, intermediateCause);

            // then
            then(exception).hasCause(intermediateCause);
            then(exception).hasRootCause(rootCause);
        }
    }

    @Nested
    @DisplayName("Constructor with messageCode, message and cause")
    class ConstructorWithMessageCodeMessageAndCauseTests {

        @Test
        void constructor_ShouldCreateExceptionWithAllFields_WhenAllProvided() {
            // given
            String messageCode = "INTERCEPTOR_VETO";
            String message = "Operation vetoed by security interceptor";
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, message, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldThrowExceptionWithAllFields_WhenThrown() {
            // given
            String messageCode = "VALIDATION_FAILED";
            String message = "Validation interceptor rejected the request";
            Throwable cause = new IllegalStateException("Invalid state");

            // when & then
            thenThrownBy(() -> {
                throw new InterceptorVetoException(messageCode, message, cause);
            })
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage(message)
                    .satisfies(ex -> {
                        InterceptorVetoException ive = (InterceptorVetoException) ex;
                        then(ive.getMessageCode()).isEqualTo(messageCode);
                    })
                    .hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleNullMessageCode_WhenNullProvided() {
            // given
            String message = "Some message";
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null, message, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isNull();
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleNullMessage_WhenNullProvided() {
            // given
            String messageCode = "SOME_CODE";
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, null, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleNullCause_WhenNullProvided() {
            // given
            String messageCode = "SOME_CODE";
            String message = "Some message";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, message, null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getCause()).isNull();
        }

        @Test
        void constructor_ShouldHandleAllNull_WhenAllNullProvided() {
            // given

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null, null, (Throwable) null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
            then(exception.getMessageCode()).isNull();
            then(exception.getCause()).isNull();
        }
    }

    @Nested
    @DisplayName("Constructor with messageCode, messageArgs, message and cause")
    class ConstructorWithMessageCodeMessageArgsMessageAndCauseTests {

        @Test
        void constructor_ShouldCreateExceptionWithAllFields_WhenAllProvided() {
            // given
            String messageCode = "INTERCEPTOR_VETO_WITH_ARGS";
            String[] messageArgs = {"arg1", "123", "arg3"};
            String message = "Operation vetoed with arguments";
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, messageArgs, message, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).isEqualTo(messageArgs);
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldThrowExceptionWithAllFields_WhenThrown() {
            // given
            String messageCode = "MULTI_ARG_VETO";
            String[] messageArgs = {"user", "admin", "42"};
            String message = "Interceptor blocked operation for user";
            Throwable cause = new IllegalStateException("Invalid state");

            // when & then
            thenThrownBy(() -> {
                throw new InterceptorVetoException(messageCode, messageArgs, message, cause);
            })
                    .isInstanceOf(InterceptorVetoException.class)
                    .hasMessage(message)
                    .satisfies(ex -> {
                        InterceptorVetoException ive = (InterceptorVetoException) ex;
                        then(ive.getMessageCode()).isEqualTo(messageCode);
                        then(ive.getMessageArgs()).containsExactly("user", "admin", "42");
                    })
                    .hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleNullMessageCode_WhenNullProvided() {
            // given
            String[] messageArgs = {"arg1"};
            String message = "Message";
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null, messageArgs, message, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isNull();
            then(exception.getMessageArgs()).isEqualTo(messageArgs);
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleNullMessageArgs_WhenNullProvided() {
            // given
            String messageCode = "SOME_CODE";
            String message = "Message";
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, null, message, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).isNull();
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleEmptyMessageArgs_WhenEmptyArrayProvided() {
            // given
            String messageCode = "EMPTY_ARGS_CODE";
            String[] emptyMessageArgs = new String[0];
            String message = "Message with empty args";
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception =
                    new InterceptorVetoException(messageCode, emptyMessageArgs, message, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).isEmpty();
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleNullMessage_WhenNullProvided() {
            // given
            String messageCode = "SOME_CODE";
            String[] messageArgs = {"arg1"};
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, messageArgs, null, cause);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).isEqualTo(messageArgs);
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldHandleNullCause_WhenNullProvided() {
            // given
            String messageCode = "SOME_CODE";
            String[] messageArgs = {"arg1"};
            String message = "Message";

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, messageArgs, message, null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(message);
            then(exception.getMessageCode()).isEqualTo(messageCode);
            then(exception.getMessageArgs()).isEqualTo(messageArgs);
            then(exception.getCause()).isNull();
        }

        @Test
        void constructor_ShouldHandleAllNull_WhenAllNullProvided() {
            // given

            // when
            InterceptorVetoException exception = new InterceptorVetoException(null, null, null, null);

            // then
            then(exception).isNotNull();
            then(exception).hasMessage(null);
            then(exception.getMessageCode()).isNull();
            then(exception.getMessageArgs()).isNull();
            then(exception.getCause()).isNull();
        }

        @Test
        void constructor_ShouldHandleMixedTypesInMessageArgs_WhenMixedTypesProvided() {
            // given
            String messageCode = "MIXED_TYPES_CODE";
            String[] mixedArgs = {"string", "123", "45.67", "true", null};
            String message = "Message with mixed types";
            Throwable cause = new RuntimeException("Root cause");

            // when
            InterceptorVetoException exception = new InterceptorVetoException(messageCode, mixedArgs, message, cause);

            // then
            then(exception).isNotNull();
            then(exception.getMessageArgs()).hasSize(5);
            then(exception.getMessageArgs()).containsExactly("string", "123", "45.67", "true", null);
            then(exception).hasCause(cause);
        }

        @Test
        void constructor_ShouldPreserveCauseChain_WhenNestedCauseProvided() {
            // given
            String messageCode = "NESTED_CAUSE";
            String[] messageArgs = {"arg1"};
            String message = "Message";
            Throwable rootCause = new IllegalArgumentException("Root");
            Throwable intermediateCause = new RuntimeException("Intermediate", rootCause);

            // when
            InterceptorVetoException exception =
                    new InterceptorVetoException(messageCode, messageArgs, message, intermediateCause);

            // then
            then(exception).hasCause(intermediateCause);
            then(exception).hasRootCause(rootCause);
        }
    }
}
