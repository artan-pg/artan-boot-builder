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

import ir.artanpg.boot.domain.exception.DomainException;

import java.io.Serial;

/**
 * Signals that a domain-level transaction rule has been violated.
 *
 * <p>This exception is thrown when an operation cannot proceed because of a
 * domain constraint related to transaction handling, such as an invalid
 * propagation mode, an unsupported isolation level, or a rollback-only
 * violation.
 *
 * <p>Instances carry an optional {@code messageCode} and an optional array of
 * {@code messageArgs} that can be used by upstream layers to resolve a
 * localized error message. When no code is supplied, the plain {@code message}
 * is used as-is.
 *
 * @author Mohammad Yazdian
 * @see DomainException
 * @since 0.1.0
 */
public class TransactionalDomainException extends DomainException {

    @Serial
    private static final long serialVersionUID = -8361502623552009731L;

    /**
     * Constructs an exception with the given message.
     *
     * @param message the detail message explaining the validation failure
     */
    public TransactionalDomainException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with a message code and a message.
     *
     * @param messageCode a code representing the error localization
     * @param message     the detail message explaining the validation failure
     */
    public TransactionalDomainException(String messageCode, String message) {
        super(messageCode, message);
    }

    /**
     * Constructs an exception with a message code, arguments, and a
     * message.
     *
     * @param messageCode a code representing the error localization
     * @param messageArgs the arguments for parameterized message formatting
     * @param message     the detail message explaining the validation failure
     */
    public TransactionalDomainException(String messageCode, String[] messageArgs, String message) {
        super(messageCode, messageArgs, message);
    }

    /**
     * Constructs an exception with a message and a cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause of this exception
     */
    public TransactionalDomainException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an exception with a message code, a message, and a
     * cause.
     *
     * @param messageCode a code representing the error localization
     * @param message     the detail message explaining the validation failure
     * @param cause       the underlying cause of this exception
     */
    public TransactionalDomainException(String messageCode, String message, Throwable cause) {
        super(messageCode, message, cause);
    }

    /**
     * Constructs an exception with a message code, arguments, a
     * message, and a cause.
     *
     * @param messageCode a code representing the error localization
     * @param messageArgs the arguments for parameterized message formatting
     * @param message     the detail message explaining the validation failure
     * @param cause       the underlying cause of this exception
     */
    public TransactionalDomainException(String messageCode, String[] messageArgs, String message, Throwable cause) {
        super(messageCode, messageArgs, message, cause);
    }
}
