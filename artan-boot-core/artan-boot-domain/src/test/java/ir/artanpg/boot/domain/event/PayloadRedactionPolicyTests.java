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

package ir.artanpg.boot.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link PayloadRedactionPolicy}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("PayloadRedactionPolicy")
class PayloadRedactionPolicyTests {

    private static final String EXPECTED_PLACEHOLDER = "[REDACTED]";

    @Nested
    @DisplayName("REDACTED_PLACEHOLDER constant")
    class RedactedPlaceholderConstant {

        @Test
        @DisplayName("should be equal to [REDACTED]")
        void redactedPlaceholder_ShouldBeEqualToRedacted_WhenAccessed() {
            // given
            // nothing

            // when
            String placeholder = PayloadRedactionPolicy.REDACTED_PLACEHOLDER;

            // then
            then(placeholder)
                    .as("REDACTED_PLACEHOLDER should be [REDACTED]")
                    .isEqualTo(EXPECTED_PLACEHOLDER);
        }
    }

    @Nested
    @DisplayName("NONE policy")
    class NonePolicy {

        @Test
        @DisplayName("should not be null")
        void none_ShouldNotBeNull_WhenAccessed() {
            // given
            // nothing

            // when
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.NONE;

            // then
            then(policy)
                    .as("NONE policy should not be null")
                    .isNotNull();
        }

        @Test
        @DisplayName("should return string representation of payload when payload is a string")
        void redact_ShouldReturnStringRepresentation_WhenPayloadIsString() {
            // given
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.NONE;
            Object payload = "hello world";

            // when
            String result = policy.redact(payload);

            // then
            then(result)
                    .as("Should return the string representation")
                    .isEqualTo("hello world");
        }

        @Test
        @DisplayName("should return 'null' when payload is null")
        void redact_ShouldReturnNullString_WhenPayloadIsNull() {
            // given
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.NONE;

            // when
            String result = policy.redact(null);

            // then
            then(result)
                    .as("Should return 'null' for null payload")
                    .isEqualTo("null");
        }

        @Test
        @DisplayName("should return toString of object when payload is a complex object")
        void redact_ShouldReturnToStringOfObject_WhenPayloadIsComplexObject() {
            // given
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.NONE;
            Object payload = new TestPayload("data");

            // when
            String result = policy.redact(payload);

            // then
            then(result)
                    .as("Should return toString of object")
                    .isEqualTo(payload.toString());
        }

        @Test
        @DisplayName("should return toString of number when payload is a number")
        void redact_ShouldReturnToStringOfNumber_WhenPayloadIsNumber() {
            // given
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.NONE;
            Object payload = 42;

            // when
            String result = policy.redact(payload);

            // then
            then(result)
                    .as("Should return string representation of number")
                    .isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("MASKED policy")
    class MaskedPolicy {

        @Test
        @DisplayName("should not be null")
        void masked_ShouldNotBeNull_WhenAccessed() {
            // given
            // nothing

            // when
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.MASKED;

            // then
            then(policy)
                    .as("MASKED policy should not be null")
                    .isNotNull();
        }

        @Test
        @DisplayName("should return REDACTED placeholder when payload is a string")
        void redact_ShouldReturnRedactedPlaceholder_WhenPayloadIsString() {
            // given
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.MASKED;
            Object payload = "sensitive data";

            // when
            String result = policy.redact(payload);

            // then
            then(result)
                    .as("Should return REDACTED placeholder")
                    .isEqualTo(EXPECTED_PLACEHOLDER);
        }

        @Test
        @DisplayName("should return REDACTED placeholder when payload is null")
        void redact_ShouldReturnRedactedPlaceholder_WhenPayloadIsNull() {
            // given
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.MASKED;

            // when
            String result = policy.redact(null);

            // then
            then(result)
                    .as("Should return REDACTED placeholder for null payload")
                    .isEqualTo(EXPECTED_PLACEHOLDER);
        }

        @Test
        @DisplayName("should return REDACTED placeholder when payload is a complex object")
        void redact_ShouldReturnRedactedPlaceholder_WhenPayloadIsComplexObject() {
            // given
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.MASKED;
            Object payload = new TestPayload("sensitive");

            // when
            String result = policy.redact(payload);

            // then
            then(result)
                    .as("Should return REDACTED placeholder for complex object")
                    .isEqualTo(EXPECTED_PLACEHOLDER);
        }

        @Test
        @DisplayName("should return REDACTED placeholder when payload is a number")
        void redact_ShouldReturnRedactedPlaceholder_WhenPayloadIsNumber() {
            // given
            PayloadRedactionPolicy policy = PayloadRedactionPolicy.MASKED;
            Object payload = 12345;

            // when
            String result = policy.redact(payload);

            // then
            then(result)
                    .as("Should return REDACTED placeholder for number")
                    .isEqualTo(EXPECTED_PLACEHOLDER);
        }
    }

    @Nested
    @DisplayName("Custom implementation")
    class CustomImplementation {

        @Test
        @DisplayName("should allow custom redaction logic")
        void redact_ShouldAllowCustomRedactionLogic_WhenCustomPolicyProvided() {
            // given
            PayloadRedactionPolicy customPolicy = payload -> "CUSTOM_" + payload;
            Object payload = "test";

            // when
            String result = customPolicy.redact(payload);

            // then
            then(result)
                    .as("Should use custom redaction logic")
                    .isEqualTo("CUSTOM_test");
        }

        @Test
        @DisplayName("should handle null payload with custom logic")
        void redact_ShouldHandleNullPayload_WhenCustomPolicyProvided() {
            // given
            PayloadRedactionPolicy customPolicy = _ -> "NULL_PAYLOAD";

            // when
            String result = customPolicy.redact(null);

            // then
            then(result)
                    .as("Should handle null with custom logic")
                    .isEqualTo("NULL_PAYLOAD");
        }

        @Test
        @DisplayName("should be usable as a functional interface")
        void customPolicy_ShouldBeUsableAsFunctionalInterface_WhenLambdaProvided() {
            // given
            PayloadRedactionPolicy policy = _ -> "always-redacted";

            // when
            String result = policy.redact("anything");

            // then
            then(result)
                    .as("Should work as functional interface")
                    .isEqualTo("always-redacted");
        }
    }

    // ========================================================================================
    // Test Doubles
    // ========================================================================================

    /**
     * Simple test payload for verifying toString behavior.
     */
    private record TestPayload(String data) {
        @Override
        public String toString() {
            return "TestPayload[data=" + data + "]";
        }
    }
}
