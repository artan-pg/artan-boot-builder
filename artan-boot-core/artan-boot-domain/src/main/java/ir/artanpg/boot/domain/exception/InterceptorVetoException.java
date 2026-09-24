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

import java.io.Serial;

/**
 * Exception thrown when a domain event interceptor vetoes the
 * current processing phase.
 *
 * <p>Indicates that an interceptor has explicitly aborted the
 * event dispatch or handling cycle.
 *
 * @author Mohammad Yazdian
 * @see DomainException
 * @since 0.1.0
 */
public class InterceptorVetoException extends DomainException {

    @Serial
    private static final long serialVersionUID = 8763302010471429685L;

    /**
     * Constructs a new exception with the specified message.
     *
     * @param message the detail message
     */
    public InterceptorVetoException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a message code and message.
     *
     * @param messageCode the code identifying the error
     * @param message     the detail message
     */
    public InterceptorVetoException(String messageCode, String message) {
        super(messageCode, message);
    }

    /**
     * Constructs a new exception with a message code, arguments,
     * and message.
     *
     * @param messageCode the code identifying the error
     * @param messageArgs the arguments for message interpolation
     * @param message     the detail message
     */
    public InterceptorVetoException(String messageCode, String[] messageArgs, String message) {
        super(messageCode, messageArgs, message);
    }

    /**
     * Constructs a new exception with a message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public InterceptorVetoException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with a message code, message,
     * and cause.
     *
     * @param messageCode the code identifying the error
     * @param message     the detail message
     * @param cause       the cause of the exception
     */
    public InterceptorVetoException(String messageCode, String message, Throwable cause) {
        super(messageCode, message, cause);
    }

    /**
     * Constructs a new exception with a message code, arguments,
     * message, and cause.
     *
     * @param messageCode the code identifying the error
     * @param messageArgs the arguments for message interpolation
     * @param message     the detail message
     * @param cause       the cause of the exception
     */
    public InterceptorVetoException(String messageCode, String[] messageArgs, String message, Throwable cause) {
        super(messageCode, messageArgs, message, cause);
    }
}
