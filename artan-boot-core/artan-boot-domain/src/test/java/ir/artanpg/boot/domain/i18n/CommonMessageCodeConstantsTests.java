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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import static ir.artanpg.boot.domain.i18n.CommonMessageCodeConstants.FIELD_IS_REQUIRED;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link CommonMessageCodeConstants}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("CommonMessageCodeConstants")
class CommonMessageCodeConstantsTests {

    private static final String UNKNOWN_CODE = "unknown.code";
    private static final String EMAIL_FIELD = "email";
    private static final String USERNAME_FIELD = "username";
    private static final String AGE_FIELD = "age";
    private static final String MIN_VALUE = "5";
    private static final String MAX_VALUE = "100";
    private static final Locale PERSIAN_LOCALE = Locale.of("fa");

    @Nested
    @DisplayName("Constants")
    class Constants {

        @Test
        @DisplayName("should have correct field-level error codes")
        void constants_ShouldHaveCorrectFieldErrorCodes_WhenAccessed() {
            // when & then
            then(FIELD_IS_REQUIRED)
                    .as("FIELD_IS_REQUIRED should have correct code")
                    .isEqualTo("common.error.field.isRequired");
            then(CommonMessageCodeConstants.FIELD_IS_INVALID)
                    .as("FIELD_IS_INVALID should have correct code")
                    .isEqualTo("common.error.field.isInvalid");
            then(CommonMessageCodeConstants.FIELD_IS_LESS_THAN)
                    .as("FIELD_IS_LESS_THAN should have correct code")
                    .isEqualTo("common.error.field.isLessThan");
            then(CommonMessageCodeConstants.FIELD_IS_LONGER_THAN)
                    .as("FIELD_IS_LONGER_THAN should have correct code")
                    .isEqualTo("common.error.field.isLongerThan");
            then(CommonMessageCodeConstants.FIELD_INVALID_FORMAT)
                    .as("FIELD_INVALID_FORMAT should have correct code")
                    .isEqualTo("common.error.field.invalidFormat");
            then(CommonMessageCodeConstants.FIELD_INVALID_LENGTH)
                    .as("FIELD_INVALID_LENGTH should have correct code")
                    .isEqualTo("common.error.field.invalidLength");
            then(CommonMessageCodeConstants.FIELD_INVALID_RANGE)
                    .as("FIELD_INVALID_RANGE should have correct code")
                    .isEqualTo("common.error.field.invalidRange");
        }

        @Test
        @DisplayName("should have correct service-level error codes")
        void constants_ShouldHaveCorrectServiceErrorCodes_WhenAccessed() {
            // when & then
            then(CommonMessageCodeConstants.SERVICE_INBOUND_PARAMETER_IS_REQUIRED)
                    .as("SERVICE_INBOUND_PARAMETER_IS_REQUIRED should have correct code")
                    .isEqualTo("common.error.service.isRequiredInbound");
            then(CommonMessageCodeConstants.SERVICE_IS_DUPLICATE)
                    .as("SERVICE_IS_DUPLICATE should have correct code")
                    .isEqualTo("common.error.service.isDuplicate");
            then(CommonMessageCodeConstants.SERVICE_RECORD_NOT_FOUND)
                    .as("SERVICE_RECORD_NOT_FOUND should have correct code")
                    .isEqualTo("common.error.service.recordNotFound");
            then(CommonMessageCodeConstants.SERVICE_RECORD_IS_INACTIVE)
                    .as("SERVICE_RECORD_IS_INACTIVE should have correct code")
                    .isEqualTo("common.error.service.recordIsInActive");
        }

        @Test
        @DisplayName("should have correct business and concurrency error codes")
        void constants_ShouldHaveCorrectBusinessAndConcurrencyCodes_WhenAccessed() {
            // when & then
            then(CommonMessageCodeConstants.BUSINESS_RULE_VIOLATED)
                    .as("BUSINESS_RULE_VIOLATED should have correct code")
                    .isEqualTo("common.error.business.ruleViolated");
            then(CommonMessageCodeConstants.CONCURRENCY_CONFLICT)
                    .as("CONCURRENCY_CONFLICT should have correct code")
                    .isEqualTo("common.error.concurrency.conflict");
        }

        @Test
        @DisplayName("should have correct auth error codes")
        void constants_ShouldHaveCorrectAuthErrorCodes_WhenAccessed() {
            // when & then
            then(CommonMessageCodeConstants.AUTH_UNAUTHORIZED)
                    .as("AUTH_UNAUTHORIZED should have correct code")
                    .isEqualTo("common.error.auth.unauthorized");
            then(CommonMessageCodeConstants.AUTH_FORBIDDEN)
                    .as("AUTH_FORBIDDEN should have correct code")
                    .isEqualTo("common.error.auth.forbidden");
        }

        @Test
        @DisplayName("should have correct validation error codes")
        void constants_ShouldHaveCorrectValidationErrorCodes_WhenAccessed() {
            // when & then
            then(CommonMessageCodeConstants.VALIDATION_EMAIL_INVALID_FORMAT)
                    .as("VALIDATION_EMAIL_INVALID_FORMAT should have correct code")
                    .isEqualTo("validation.email.invalidFormat");
            then(CommonMessageCodeConstants.VALIDATION_PHONE_INVALID_FORMAT)
                    .as("VALIDATION_PHONE_INVALID_FORMAT should have correct code")
                    .isEqualTo("validation.phone.invalidFormat");
            then(CommonMessageCodeConstants.VALIDATION_NATIONAL_CODE_INVALID)
                    .as("VALIDATION_NATIONAL_CODE_INVALID should have correct code")
                    .isEqualTo("validation.nationalCode.invalid");
            then(CommonMessageCodeConstants.VALIDATION_DATE_INVALID_FORMAT)
                    .as("VALIDATION_DATE_INVALID_FORMAT should have correct code")
                    .isEqualTo("validation.date.invalidFormat");
            then(CommonMessageCodeConstants.VALIDATION_DATE_IN_FUTURE)
                    .as("VALIDATION_DATE_IN_FUTURE should have correct code")
                    .isEqualTo("validation.date.inFuture");
            then(CommonMessageCodeConstants.VALIDATION_DATE_IN_PAST)
                    .as("VALIDATION_DATE_IN_PAST should have correct code")
                    .isEqualTo("validation.date.inPast");
        }
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should throw UnsupportedOperationException when instantiated via reflection")
        void constructor_ShouldThrowUnsupportedOperationException_WhenInstantiatedViaReflection() {
            // given
            Constructor<CommonMessageCodeConstants> constructor = getPrivateConstructor();

            // when & then
            thenThrownBy(constructor::newInstance)
                    .as("Private constructor should throw UnsupportedOperationException")
                    .isInstanceOf(InvocationTargetException.class)
                    .hasCauseInstanceOf(UnsupportedOperationException.class);
        }

        private Constructor<CommonMessageCodeConstants> getPrivateConstructor() {
            try {
                Constructor<CommonMessageCodeConstants> constructor =
                        CommonMessageCodeConstants.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Private constructor not found", e);
            }
        }
    }

    @Nested
    @DisplayName("getMessage with default locale")
    class GetMessageWithDefaultLocale {

        @Test
        @DisplayName("should resolve message with default English locale")
        void getMessage_ShouldResolveMessage_WithDefaultEnglishLocale() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    FIELD_IS_REQUIRED,
                    EMAIL_FIELD);

            // then
            then(result)
                    .as("Should resolve English message")
                    .isEqualTo("The value of the field email is mandatory");
        }

        @Test
        @DisplayName("should return empty string when code is null")
        void getMessage_ShouldReturnEmptyString_WhenCodeIsNull() {
            // when
            String result = CommonMessageCodeConstants.getMessage(null);

            // then
            then(result)
                    .as("Should return empty string for null code")
                    .isEmpty();
        }

        @Test
        @DisplayName("should return code when message not found")
        void getMessage_ShouldReturnCode_WhenMessageNotFound() {
            // when
            String result = CommonMessageCodeConstants.getMessage(UNKNOWN_CODE);

            // then
            then(result)
                    .as("Should return code when not found")
                    .isEqualTo(UNKNOWN_CODE);
        }

        @Test
        @DisplayName("should handle null args gracefully")
        void getMessage_ShouldHandleNullArgs_WhenArgsIsNull() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.SERVICE_RECORD_NOT_FOUND,
                    (Object[]) null);

            // then
            then(result)
                    .as("Should handle null args")
                    .isEqualTo("No records found");
        }

        @Test
        @DisplayName("should handle no args for messages without placeholders")
        void getMessage_ShouldHandleNoArgs_WhenMessageHasNoPlaceholders() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.SERVICE_RECORD_NOT_FOUND);

            // then
            then(result)
                    .as("Should handle no args")
                    .isEqualTo("No records found");
        }

        @Test
        @DisplayName("should format message with single argument")
        void getMessage_ShouldFormatMessage_WithSingleArgument() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    FIELD_IS_REQUIRED,
                    USERNAME_FIELD);

            // then
            then(result)
                    .as("Should format with single argument")
                    .isEqualTo("The value of the field username is mandatory");
        }

        @Test
        @DisplayName("should format message with multiple arguments")
        void getMessage_ShouldFormatMessage_WithMultipleArguments() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.FIELD_IS_LESS_THAN,
                    AGE_FIELD,
                    MIN_VALUE);

            // then
            then(result)
                    .as("Should format with multiple arguments")
                    .isEqualTo("Field age cannot be less than 5 characters long");
        }
    }

    @Nested
    @DisplayName("getMessage with explicit locale")
    class GetMessageWithExplicitLocale {

        @Test
        @DisplayName("should resolve English message with Locale.ENGLISH")
        void getMessage_ShouldResolveEnglishMessage_WhenLocaleIsEnglish() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    Locale.ENGLISH,
                    FIELD_IS_REQUIRED,
                    EMAIL_FIELD);

            // then
            then(result)
                    .as("Should resolve English message")
                    .isEqualTo("The value of the field email is mandatory");
        }

        @Test
        @DisplayName("should resolve Persian message with Persian locale")
        void getMessage_ShouldResolvePersianMessage_WhenLocaleIsPersian() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    PERSIAN_LOCALE,
                    FIELD_IS_REQUIRED,
                    EMAIL_FIELD);

            // then
            then(result)
                    .as("Should resolve Persian message")
                    .isEqualTo("مقداردهی فیلد email اجباری است.");
        }

        @Test
        @DisplayName("should return empty string when code is null with explicit locale")
        void getMessage_ShouldReturnEmptyString_WhenCodeIsNullWithExplicitLocale() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    Locale.ENGLISH, null);

            // then
            then(result)
                    .as("Should return empty string for null code")
                    .isEmpty();
        }

        @Test
        @DisplayName("should use default locale when locale is null")
        void getMessage_ShouldUseDefaultLocale_WhenLocaleIsNull() {
            // when
            String result = CommonMessageCodeConstants.getMessage((Locale) null, FIELD_IS_REQUIRED, EMAIL_FIELD);

            // then
            then(result)
                    .as("Should use default English locale")
                    .isEqualTo("The value of the field email is mandatory");
        }

        @Test
        @DisplayName("should handle null args with explicit locale")
        void getMessage_ShouldHandleNullArgs_WithExplicitLocale() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    Locale.ENGLISH,
                    CommonMessageCodeConstants.SERVICE_RECORD_NOT_FOUND,
                    (Object[]) null);

            // then
            then(result)
                    .as("Should handle null args with explicit locale")
                    .isEqualTo("No records found");
        }

        @Test
        @DisplayName("should return code when message not found with explicit locale")
        void getMessage_ShouldReturnCode_WhenMessageNotFoundWithExplicitLocale() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    Locale.ENGLISH, UNKNOWN_CODE);

            // then
            then(result)
                    .as("Should return code when not found")
                    .isEqualTo(UNKNOWN_CODE);
        }

        @Test
        @DisplayName("should format Persian message with arguments")
        void getMessage_ShouldFormatPersianMessage_WithArguments() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    PERSIAN_LOCALE,
                    FIELD_IS_REQUIRED,
                    USERNAME_FIELD);

            // then
            then(result)
                    .as("Should format Persian message with argument")
                    .isEqualTo("مقداردهی فیلد username اجباری است.");
        }

        @Test
        @DisplayName("should format message with multiple arguments and explicit locale")
        void getMessage_ShouldFormatMessage_WithMultipleArgsAndExplicitLocale() {
            // when
            String result = CommonMessageCodeConstants.getMessage(
                    Locale.ENGLISH,
                    CommonMessageCodeConstants.FIELD_IS_LESS_THAN,
                    AGE_FIELD,
                    MIN_VALUE);

            // then
            then(result)
                    .as("Should format with multiple arguments")
                    .isEqualTo("Field age cannot be less than 5 characters long");
        }
    }

    @Nested
    @DisplayName("Resource Bundle Edge Cases")
    class ResourceBundleEdgeCases {

        @Test
        @DisplayName("should return code when code does not exist in bundle")
        void resourceBundle_ShouldReturnCode_WhenCodeDoesNotExistInBundle() {
            // given
            String nonExistentCode = "non.existent.code";

            // when
            String result = CommonMessageCodeConstants.getMessage(
                    Locale.ENGLISH, nonExistentCode);

            // then
            then(result)
                    .as("Should return code when not in bundle")
                    .isEqualTo(nonExistentCode);
        }

        @Test
        @DisplayName("should resolve all field-level error codes")
        void resourceBundle_ShouldResolveAllFieldErrorCodes_WhenAccessed() {
            // when & then
            then(CommonMessageCodeConstants.getMessage(
                    FIELD_IS_REQUIRED, EMAIL_FIELD))
                    .as("FIELD_IS_REQUIRED should resolve")
                    .isNotEmpty();
            then(CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.FIELD_IS_INVALID, EMAIL_FIELD))
                    .as("FIELD_IS_INVALID should resolve")
                    .isNotEmpty();
            then(CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.FIELD_IS_LESS_THAN, AGE_FIELD, MIN_VALUE))
                    .as("FIELD_IS_LESS_THAN should resolve")
                    .isNotEmpty();
            then(CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.FIELD_IS_LONGER_THAN, AGE_FIELD, MAX_VALUE))
                    .as("FIELD_IS_LONGER_THAN should resolve")
                    .isNotEmpty();
        }

        @Test
        @DisplayName("should resolve all service-level error codes")
        void resourceBundle_ShouldResolveAllServiceErrorCodes_WhenAccessed() {
            // when & then
            then(CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.SERVICE_INBOUND_PARAMETER_IS_REQUIRED))
                    .as("SERVICE_INBOUND_PARAMETER_IS_REQUIRED should resolve")
                    .isNotEmpty();
            then(CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.SERVICE_IS_DUPLICATE))
                    .as("SERVICE_IS_DUPLICATE should resolve")
                    .isNotEmpty();
            then(CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.SERVICE_RECORD_NOT_FOUND))
                    .as("SERVICE_RECORD_NOT_FOUND should resolve")
                    .isNotEmpty();
            then(CommonMessageCodeConstants.getMessage(
                    CommonMessageCodeConstants.SERVICE_RECORD_IS_INACTIVE))
                    .as("SERVICE_RECORD_IS_INACTIVE should resolve")
                    .isNotEmpty();
        }
    }
}
