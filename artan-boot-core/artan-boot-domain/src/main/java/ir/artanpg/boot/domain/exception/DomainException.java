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
 * A runtime exception representing a violation of domain business rules.
 *
 * <p>Domain exceptions are thrown when a business rule or invariant is
 * violated within the domain layer. Unlike technical exceptions, domain
 * exceptions represent meaningful business errors that should be handled
 * appropriately by the application layer.
 *
 * @author Mohammad Yazdian
 * @see UseCaseException
 * @see BusinessValidationException
 * @since 0.1.0
 */
public class DomainException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1399794951728675553L;

    /**
     * The code representing the error localization.
     */
    private final String messageCode;

    /**
     * The arguments for message formatting.
     */
    private final String[] messageArgs;

    /**
     * Constructs a new domain exception with the specified detail message.
     *
     * @param message the detail message explaining the validation failure
     */
    public DomainException(String message) {
        super(message);
        this.messageCode = null;
        this.messageArgs = null;
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
    public DomainException(String messageCode, String message) {
        super(message);
        this.messageCode = messageCode;
        this.messageArgs = null;
    }

    /**
     * Constructs a new domain exception with the specified message code
     * localization, arguments, and detail message.
     *
     * <p>This constructor creates a domain exception with full support for
     * internationalization and parameterized messages.
     *
     * @param messageCode a code representing the error localization
     * @param messageArgs the arguments for parameterized message formatting
     * @param message     the detail message explaining the validation failure
     */
    public DomainException(String messageCode, String[] messageArgs, String message) {
        super(message);
        this.messageCode = messageCode;
        this.messageArgs = messageArgs;
    }

    /**
     * Constructs a new domain exception with the specified message and cause.
     *
     * @param message the detail message explaining the validation failure
     * @param cause   the underlying cause of this exception
     */
    public DomainException(String message, Throwable cause) {
        super(message, cause);
        this.messageCode = null;
        this.messageArgs = null;
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
    public DomainException(String messageCode, String message, Throwable cause) {
        super(message, cause);
        this.messageCode = messageCode;
        this.messageArgs = null;
    }

    /**
     * Constructs a new domain exception with the specified message code
     * localization, arguments, detail message, and cause.
     *
     * <p>This constructor provides full exception chaining support by
     * preserving the original cause. It is useful when wrapping lower-level
     * exceptions while maintaining structured error information.
     *
     * @param messageCode a code representing the error localization
     * @param messageArgs the arguments for parameterized message formatting
     * @param message     the detail message explaining the validation failure
     * @param cause       the underlying cause of this exception
     */
    public DomainException(String messageCode, String[] messageArgs, String message, Throwable cause) {
        super(message, cause);
        this.messageCode = messageCode;
        this.messageArgs = messageArgs;
    }

    /**
     * Returns the error code associated with this exception.
     *
     * <p>The error code can be used for client-side error handling,
     * internationalization, or system monitoring.
     *
     * @return the message code, or {@code null} if not set
     */
    public String getMessageCode() {
        return this.messageCode;
    }

    /**
     * Returns the message arguments array for this exception.
     *
     * <p>These arguments are used for parameterized message formatting and
     * internationalization (i18n). They are typically passed to a message
     * source or formatter to generate the final error message.
     *
     * @return an array of message arguments
     */
    public String[] getMessageArgs() {
        return this.messageArgs;
    }
}
