/*
 * Copyright (c) 2026-present the original author or authors.
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

import java.io.Serial;

/**
 * Exception thrown when a use case execution fails due to business rule
 * violations or invalid operations within the application layer.
 *
 * <p>This exception represents errors that occur during the execution of
 * application use cases. It extends {@link DomainException} to maintain
 * consistency with the domain layer while providing specific semantics for use
 * case failures.
 *
 * @author Mohammad Yazdian
 * @see DomainException
 * @since 0.1.0
 */
public class UseCaseException extends DomainException {

    @Serial
    private static final long serialVersionUID = -5577363078275363673L;

    /**
     * Constructs a new domain exception with the specified detail message.
     *
     * <p>The detail message should provide a clear and meaningful explanation
     * of the business rule violation or error that occurred during use case
     * execution.
     *
     * @param message the detail message explaining the validation failure
     */
    public UseCaseException(String message) {
        super(message);
    }

    /**
     * Constructs a new domain exception with a message code localization and
     * detail message.
     *
     * <p>The message code used for internationalization error handling.
     *
     * @param messageCode a code representing the error localization
     * @param message     the detail message explaining the validation failure
     */
    public UseCaseException(String messageCode, String message) {
        super(messageCode, message);
    }

    /**
     * Constructs a new use case exception with message code, arguments, and
     * detail message.
     *
     * @param messageCode a code representing the error localization
     * @param messageArgs the arguments for parameterized message formatting
     * @param message     the detail message explaining the validation failure
     */
    public UseCaseException(String messageCode, String[] messageArgs, String message) {
        super(messageCode, messageArgs, message);
    }

    /**
     * Constructs a new domain exception with the specified detail message and cause.
     *
     * <p>This constructor is useful when wrapping lower-level exceptions with
     * a business-meaningful exception while preserving the root cause for
     * debugging purposes.
     *
     * @param message the detail message explaining the validation failure
     * @param cause   the underlying cause of the exception
     */
    public UseCaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new domain exception with a message code localization,
     * detail message, and cause.
     *
     * <p>The message code used for internationalization error handling.
     *
     * @param messageCode a code representing the error localization
     * @param message     the detail message explaining the validation failure
     * @param cause       the underlying cause of this exception
     */
    public UseCaseException(String messageCode, String message, Throwable cause) {
        super(messageCode, message, cause);
    }

    /**
     * Constructs a new use case exception with message code, arguments, detail
     * message, and cause.
     *
     * @param messageCode a code representing the error localization
     * @param messageArgs the arguments for parameterized message formatting
     * @param message     the detail message explaining the validation failure
     * @param cause       the underlying cause of this exception
     */
    public UseCaseException(String messageCode, String[] messageArgs, String message, Throwable cause) {
        super(messageCode, messageArgs, message, cause);
    }
}
