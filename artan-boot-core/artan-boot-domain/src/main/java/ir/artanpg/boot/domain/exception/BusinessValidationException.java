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

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * A specialized domain exception for business validation failures at the field
 * level.
 *
 * <p>This exception is thrown when a specific field or attribute fails
 * business validation rules within the domain layer. It extends
 * {@link DomainException} with additional context about the invalid field and
 * the rejected value, making it easier to provide detailed, actionable
 * feedback to clients.
 *
 * <h2>Design Rationale</h2>
 *
 * <p>In Domain-Driven Design, validation logic is encapsulated within the
 * domain model. When validation fails, the domain needs to communicate not
 * just that an error occurred, but specifically:
 * <ul>
 *   <li><b>Which field</b> failed validation (for UI binding and error
 *      highlighting)</li>
 *   <li><b>What value</b> was rejected (for debugging and user feedback)</li>
 *   <li><b>Why it failed</b> (with i18n support via message codes)</li>
 * </ul>
 *
 * <h2>Builder Pattern</h2>
 *
 * <p>Due to the multiple optional parameters (field name, rejected value,
 * message code, message args, cause), this exception uses the Builder pattern
 * to provide a fluent, readable API for construction. This avoids the
 * "telescoping constructor" problem and makes the code more maintainable as
 * new fields are added.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple validation error
 * throw new BusinessValidationException("Email is required");
 *
 * // Field-level validation with rejected value
 * throw BusinessValidationException.builder()
 *     .fieldName("email")
 *     .rejectedValue("invalid-email")
 *     .messageCode("validation.email.format")
 *     .message("Email format is invalid")
 *     .build();
 *
 * // Validation with i18n and arguments
 * throw BusinessValidationException.builder()
 *     .fieldName("age")
 *     .rejectedValue(15)
 *     .messageCode("validation.age.minimum")
 *     .messageArgs("18")
 *     .message("Age must be at least 18 years")
 *     .build();
 *
 * // Validation with underlying cause
 * try {
 *     validateBusinessRule(entity);
 * } catch (SomeException e) {
 *     throw BusinessValidationException.builder()
 *         .fieldName("status")
 *         .messageCode("validation.status.invalid")
 *         .message("Invalid status transition")
 *         .cause(e)
 *         .build();
 * }
 * }</pre>
 *
 * <h2>Integration with REST APIs</h2>
 *
 * <p>When used with Spring Boot or similar frameworks, this exception can be
 * caught by a global exception handler and transformed into a structured error
 * response:
 * <pre>{@code
 * @ExceptionHandler(BusinessValidationException.class)
 * public ResponseEntity<ErrorResponse> handleValidation(BusinessValidationException ex) {
 *     ErrorResponse error = new ErrorResponse();
 *     error.setField(ex.getFieldName());
 *     error.setRejectedValue(ex.getRejectedValue());
 *     error.setCode(ex.getMessageCode());
 *     error.setMessage(ex.getMessage());
 *     return ResponseEntity.badRequest().body(error);
 * }
 * }</pre>
 *
 * @author Mohammad Yazdian
 * @see DomainException
 * @see UseCaseException
 * @since 0.1.0
 */
public class BusinessValidationException extends DomainException {

    @Serial
    private static final long serialVersionUID = 8487420833167772478L;

    /**
     * The name of the field that failed validation.
     *
     * <p>This should match the field name in the domain model or DTO to enable
     * proper error binding in client applications.
     */
    @Nullable
    private final String fieldName;

    /**
     * The value that was rejected during validation.
     *
     * <p>This provides context for debugging and can be displayed to users to
     * help them understand what went wrong.
     */
    @Nullable
    private final Serializable rejectedValue;

    /**
     * Constructs a new business validation exception with a detail message.
     *
     * <p>This is the simplest constructor for cases where field-level context
     * is not needed.
     *
     * @param message the detail message explaining the validation failure
     */
    public BusinessValidationException(String message) {
        super(message);
        this.fieldName = null;
        this.rejectedValue = null;
    }

    /**
     * Constructs a new business validation exception from a builder.
     *
     * <p>This constructor is intended for internal use by the {@link Builder}
     * class and should not be called directly by client code.
     *
     * @param builder the builder containing the exception configuration
     */
    public BusinessValidationException(Builder builder) {
        super(builder.messageCode, builder.messageArgs, builder.message, builder.cause);
        this.fieldName = builder.fieldName;
        this.rejectedValue = builder.rejectedValue;
    }

    /**
     * Creates a new builder for constructing
     * {@code BusinessValidationException} instances.
     *
     * <p>The builder pattern provides a fluent API for constructing exceptions
     * with multiple optional parameters, improving code readability and
     * maintainability.
     *
     * @param message the detail message explaining the validation failure
     * @return a new builder instance
     */
    public static Builder builder(String message) {
        return new Builder(message);
    }

    /**
     * Returns the name of the field that failed validation.
     *
     * <p>The field name can be used to:
     * <ul>
     *   <li>Bind error messages to specific form fields in the UI</li>
     *   <li>Highlight invalid fields in API responses</li>
     *   <li>Provide field-specific error messages</li>
     * </ul>
     *
     * @return the field name, or {@code null} if not applicable
     */
    @Nullable
    public String getFieldName() {
        return this.fieldName;
    }

    /**
     * Returns the value that was rejected during validation.
     *
     * <p>The rejected value provides context for:
     * <ul>
     *   <li>Debugging validation failures</li>
     *   <li>Displaying the invalid value to users for correction</li>
     *   <li>Logging and audit trails</li>
     * </ul>
     *
     * <p><b>Note:</b> The returned value is of type {@link Serializable} to
     * ensure the exception itself remains serializable. Common types include
     * {@link String}, {@link Number}, {@link LocalDate}, etc.
     *
     * @return the rejected value, or {@code null} if not available
     */
    @Nullable
    public Serializable getRejectedValue() {
        return this.rejectedValue;
    }

    /**
     * Builder for constructing {@link BusinessValidationException} instances.
     *
     * <p>This builder provides a fluent API for creating validation exceptions
     * with multiple optional parameters. All methods return the builder
     * instance to enable method chaining.
     *
     * <h2>Required Parameters</h2>
     * <ul>
     *   <li><b>message</b> - The detail message explaining the validation
     *      failure</li>
     * </ul>
     *
     * <h2>Optional Parameters</h2>
     * <ul>
     *   <li><b>fieldName</b> - The name of the field that failed validation</li>
     *   <li><b>rejectedValue</b> - The value that was rejected</li>
     *   <li><b>messageCode</b> - A code for i18n error handling</li>
     *   <li><b>messageArgs</b> - Arguments for parameterized message formatting</li>
     *   <li><b>cause</b> - The underlying cause of the validation failure</li>
     * </ul>
     */
    public static final class Builder {
        private final String message;

        private String messageCode;
        private String[] messageArgs;

        private String fieldName;
        private Serializable rejectedValue;

        private Throwable cause;

        /**
         * Private constructor to enforce use of {@link #builder(String)}.
         *
         * @param message the detail message explaining the validation failure
         */
        private Builder(String message) {
            this.message = message;
        }

        /**
         * Sets the message code for internationalization (i18n) error
         * handling.
         *
         * <p>The message code is used to look up localized error messages from
         * resource bundles. It should follow a consistent naming convention
         * (e.g., "validation.email.format", "error.user.notFound").
         *
         * @param messageCode the message code for i18n
         * @return this builder instance for method chaining
         */
        public Builder messageCode(String messageCode) {
            this.messageCode = messageCode;
            return this;
        }

        /**
         * Sets the arguments for parameterized message formatting.
         *
         * <p>These arguments are used with the message code to generate
         * localized, parameterized messages. For example, if the message
         * template is "Age must be at least {0}", the argument would be the
         * minimum age value.
         *
         * @param messageArgs the arguments for message formatting
         * @return this builder instance for method chaining
         */
        public Builder messageArgs(String... messageArgs) {
            this.messageArgs = messageArgs;
            return this;
        }

        /**
         * Sets the name of the field that failed validation.
         *
         * <p>The field name should match the field name in the domain model or
         * DTO to enable proper error binding in client applications
         * (e.g., form validation).
         *
         * @param fieldName the name of the field that failed validation
         * @return this builder instance for method chaining
         */
        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        /**
         * Sets the value that was rejected during validation.
         *
         * <p>The rejected value provides context for debugging and can be
         * displayed to users to help them understand what went wrong. The
         * value must be {@link Serializable} to ensure the exception itself
         * remains serializable.
         *
         * @param rejectedValue the value that was rejected
         * @return this builder instance for method chaining
         */
        public Builder rejectedValue(@Nullable Serializable rejectedValue) {
            this.rejectedValue = rejectedValue;
            return this;
        }

        /**
         * Sets the underlying cause of the validation failure.
         *
         * <p>This is useful when the validation failure is caused by an
         * underlying exception (e.g., a database constraint violation) and you
         * want to preserve the exception chain for debugging.
         *
         * @param cause the underlying cause
         * @return this builder instance for method chaining
         */
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        /**
         * Builds and returns a new {@link BusinessValidationException}
         * instance.
         *
         * <p>This method validates that the required parameters are present
         * and constructs the exception with all configured values.
         *
         * @return a new {@code BusinessValidationException} instance
         * @throws IllegalStateException if the message is null or empty
         */
        public BusinessValidationException build() {
            if (this.message == null || this.message.isEmpty()) {
                throw new IllegalStateException("Message is required for BusinessValidationException");
            }
            return new BusinessValidationException(this);
        }
    }
}
