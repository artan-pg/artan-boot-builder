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

package ir.artanpg.boot.domain.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Central class for common domain error message codes used across the
 * application.
 *
 * <p>This class provides a centralized location for defining and managing
 * error message codes that are used throughout the domain layer.
 * By consolidating these constants, we ensure consistency in error handling
 * and internationalization (i18n) support.
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li><b>Framework Independence:</b> This class uses only Java SE APIs
 *       ({@link ResourceBundle}) and has no dependencies on external
 *       frameworks like Spring.</li>
 *   <li><b>Resource Bundle Based:</b> Message resolution is performed using
 *       {@link ResourceBundle} with the {@code domain-messages} base name.
 *       </li>
 *   <li><b>Locale Support:</b> Supports multiple locales through standard Java
 *       resource bundle naming conventions
 *       (e.g., {@code domain-messages_fa.properties}).</li>
 * </ul>
 *
 * <h2>Resource Bundle Configuration:</h2>
 *
 * <p>This class expects resource bundles to be available in the classpath:
 * <ul>
 *   <li>{@code domain-messages.properties} (English - default)</li>
 *   <li>{@code domain-messages_fa.properties} (Persian)</li>
 * </ul>
 *
 * <p><b>Note:</b> In a Spring Boot application, you should configure
 * {@code MessageSource} to load these bundles. See the infrastructure
 * layer configuration for details.
 *
 * @author Mohammad Yazdian
 * @see ResourceBundle
 * @since 0.1.0
 */
public class CommonMessageCodeConstants {

    // ========================================================================================
    // Field-level validation errors
    // ========================================================================================

    /**
     * Error code for required field validation failures.
     */
    public static final String FIELD_IS_REQUIRED = "common.error.field.isRequired";

    /**
     * Error code for invalid field validation failures.
     */
    public static final String FIELD_IS_INVALID = "common.error.field.isInvalid";

    /**
     * Error code constant indicating that a field's value is less than the
     * minimum allowed value.
     */
    public static final String FIELD_IS_LESS_THAN = "common.error.field.isLessThan";

    /**
     * Error code constant indicating that a field's value is longer than the
     * maximum allowed length.
     */
    public static final String FIELD_IS_LONGER_THAN = "common.error.field.isLongerThan";

    /**
     * Error code for invalid field format validation failures.
     */
    public static final String FIELD_INVALID_FORMAT = "common.error.field.invalidFormat";

    /**
     * Error code for invalid field length validation failures.
     */
    public static final String FIELD_INVALID_LENGTH = "common.error.field.invalidLength";

    /**
     * Error code for invalid field range validation failures.
     */
    public static final String FIELD_INVALID_RANGE = "common.error.field.invalidRange";

    // ========================================================================================
    // Service-level errors
    // ========================================================================================

    /**
     * Error code for missing required inbound parameters in services.
     */
    public static final String SERVICE_INBOUND_PARAMETER_IS_REQUIRED = "common.error.service.isRequiredInbound";

    /**
     * Error code for duplicate data violation.
     */
    public static final String SERVICE_IS_DUPLICATE = "common.error.service.isDuplicate";

    /**
     * Error code for not finding a record.
     */
    public static final String SERVICE_RECORD_NOT_FOUND = "common.error.service.recordNotFound";

    /**
     * Error code for a record being inactive.
     */
    public static final String SERVICE_RECORD_IS_INACTIVE = "common.error.service.recordIsInActive";

    // ========================================================================================
    // Business rule violations
    // ========================================================================================

    /**
     * Error code for business rule violations.
     */
    public static final String BUSINESS_RULE_VIOLATED = "common.error.business.ruleViolated";

    // ========================================================================================
    // Concurrency errors
    // ========================================================================================

    /**
     * Error code for optimistic locking concurrency conflicts.
     */
    public static final String CONCURRENCY_CONFLICT = "common.error.concurrency.conflict";

    // ========================================================================================
    // Authentication/Authorization errors
    // ========================================================================================

    /**
     * Error code for authentication failures (user not authenticated).
     */
    public static final String AUTH_UNAUTHORIZED = "common.error.auth.unauthorized";

    /**
     * Error code for authorization failures (user not permitted).
     */
    public static final String AUTH_FORBIDDEN = "common.error.auth.forbidden";

    // ========================================================================================
    // Specific field validations
    // ========================================================================================

    /**
     * Error code for invalid email format.
     */
    public static final String VALIDATION_EMAIL_INVALID_FORMAT = "validation.email.invalidFormat";

    /**
     * Error code for invalid phone number format.
     */
    public static final String VALIDATION_PHONE_INVALID_FORMAT = "validation.phone.invalidFormat";

    /**
     * Error code for invalid national code.
     */
    public static final String VALIDATION_NATIONAL_CODE_INVALID = "validation.nationalCode.invalid";

    /**
     * Error code for invalid date format.
     */
    public static final String VALIDATION_DATE_INVALID_FORMAT = "validation.date.invalidFormat";

    /**
     * Error code for dates that are in the future.
     */
    public static final String VALIDATION_DATE_IN_FUTURE = "validation.date.inFuture";

    /**
     * Error code for dates that are in the past.
     */
    public static final String VALIDATION_DATE_IN_PAST = "validation.date.inPast";

    /**
     * The default resource bundle base name for message resolution.
     */
    private static final String DEFAULT_BUNDLE_NAME = "domain-messages";

    /**
     * The default locale to use when no locale is specified.
     */
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws UnsupportedOperationException if called via reflection (implicitly)
     */
    private CommonMessageCodeConstants() {
        throw new UnsupportedOperationException();
    }

    /**
     * Resolves a message code to its localized text using the default locale
     * (English) and the specified arguments.
     *
     * <p>This is a convenience method equivalent to calling
     * {@link #getMessage(Locale, String, Object...)} with
     * {@link Locale#ENGLISH} as the locale.
     *
     * @param code the message code to resolve
     * @param args the arguments for parameterized messages
     * @return the resolved message text, or the code itself if not found
     */
    public static String getMessage(String code, Object... args) {
        return getMessage(DEFAULT_LOCALE, code, args);
    }

    /**
     * Resolves a message code to its localized text using the specified
     * locale and arguments.
     *
     * <p>This method attempts to resolve the message in the following order:
     * <ol>
     *   <li>Otherwise, fall back to {@link ResourceBundle} with the default
     *       bundle name</li>
     *   <li>If the code is not found, return the code itself</li>
     * </ol>
     *
     * @param code   the message code to resolve
     * @param locale the locale to use for resolution
     * @param args   the arguments for parameterized messages (e.g., {0}, {1})
     * @return the resolved message text, or the code itself if not found
     */
    public static String getMessage(Locale locale, String code, Object... args) {
        if (code == null) return "";

        Locale effectiveLocale = locale != null ? locale : DEFAULT_LOCALE;
        Object[] effectiveArgs = args != null ? args : new Object[0];

        return resolveFromResourceBundle(code, effectiveLocale, effectiveArgs);
    }

    /**
     * Resolves a message from the {@link ResourceBundle}.
     *
     * @param code   the message code
     * @param locale the locale
     * @param args   the arguments
     * @return the resolved message, or the code if not found
     */
    private static String resolveFromResourceBundle(String code, Locale locale, Object[] args) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(DEFAULT_BUNDLE_NAME, locale);
            String pattern = bundle.getString(code);
            return MessageFormat.format(pattern, args);
        } catch (MissingResourceException _) {
            return code;
        }
    }
}
