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

package ir.artanpg.boot.domain.model;

import ir.artanpg.boot.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link Identifier}.
 *
 * @author Mohammad Yazdian
 */
public class IdentifierTests {

    @Nested
    @DisplayName("requireValid")
    class RequireValidTests {

        @Test
        void requireValid_ShouldReturnValue_WhenValueIsValidString() {
            // given
            Identifier<String> identifier = () -> "valid-id";

            // when
            String result = identifier.requireValid();

            // then
            then(result).isEqualTo("valid-id");
        }

        @Test
        void requireValid_ShouldReturnValue_WhenValueIsValidLong() {
            // given
            Identifier<Long> identifier = () -> 123L;

            // when
            Long result = identifier.requireValid();

            // then
            then(result).isEqualTo(123L);
        }

        @Test
        void requireValid_ShouldReturnValue_WhenValueIsValidUuid() {
            // given
            String uuid = "550e8400-e29b-41d4-a716-446655440000";
            Identifier<String> identifier = () -> uuid;

            // when
            String result = identifier.requireValid();

            // then
            then(result).isEqualTo(uuid);
        }

        @Test
        void requireValid_ShouldThrowDomainException_WhenValueIsNull() {
            // given
            Identifier<String> identifier = () -> null;

            // when & then
            thenThrownBy(identifier::requireValid)
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifier value cannot be null");
        }

        @Test
        void requireValid_ShouldThrowDomainException_WhenValueIsNullLong() {
            // given
            Identifier<Long> identifier = () -> null;

            // when & then
            thenThrownBy(identifier::requireValid)
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifier value cannot be null");
        }

        @Test
        void requireValid_ShouldThrowDomainException_WhenValueIsBlankString() {
            // given
            Identifier<String> identifier = () -> "   ";

            // when & then
            thenThrownBy(identifier::requireValid)
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifier value cannot be blank");
        }

        @Test
        void requireValid_ShouldThrowDomainException_WhenValueIsEmptyString() {
            // given
            Identifier<String> identifier = () -> "";

            // when & then
            thenThrownBy(identifier::requireValid)
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifier value cannot be blank");
        }

        @Test
        void requireValid_ShouldThrowDomainException_WhenValueIsTabString() {
            // given
            Identifier<String> identifier = () -> "\t";

            // when & then
            thenThrownBy(identifier::requireValid)
                    .isInstanceOf(DomainException.class)
                    .hasMessage("The identifier value cannot be blank");
        }

        @Test
        void requireValid_ShouldReturnValue_WhenStringContainsWhitespaceButNotBlank() {
            // given
            Identifier<String> identifier = () -> " valid-id ";

            // when
            String result = identifier.requireValid();

            // then
            then(result).isEqualTo(" valid-id ");
        }

        @Test
        void requireValid_ShouldReturnValue_WhenStringContainsInternalSpaces() {
            // given
            Identifier<String> identifier = () -> "valid id";

            // when
            String result = identifier.requireValid();

            // then
            then(result).isEqualTo("valid id");
        }

        @Test
        void requireValid_ShouldReturnValue_WhenValueIsZero() {
            // given
            Identifier<Long> identifier = () -> 0L;

            // when
            Long result = identifier.requireValid();

            // then
            then(result).isEqualTo(0L);
        }

        @Test
        void requireValid_ShouldReturnValue_WhenValueIsNegativeNumber() {
            // given
            Identifier<Long> identifier = () -> -1L;

            // when
            Long result = identifier.requireValid();

            // then
            then(result).isEqualTo(-1L);
        }
    }

    @Nested
    @DisplayName("value")
    class ValueTests {

        @Test
        void value_ShouldReturnValue_WhenImplementedByLambda() {
            // given
            Identifier<String> identifier = () -> "test-id";

            // when
            String result = identifier.value();

            // then
            then(result).isEqualTo("test-id");
        }

        @SuppressWarnings("ConstantValue")
        @Test
        void value_ShouldReturnNull_WhenLambdaReturnsNull() {
            // given
            Identifier<String> identifier = () -> null;

            // when
            String result = identifier.value();

            // then
            then(result).isNull();
        }

        @Test
        void value_ShouldReturnLong_WhenImplementedWithLong() {
            // given
            Identifier<Long> identifier = () -> 42L;

            // when
            Long result = identifier.value();

            // then
            then(result).isEqualTo(42L);
        }

        @Test
        void value_ShouldBeInvokedMultipleTimes_WhenCalledRepeatedly() {
            // given
            Identifier<String> identifier = () -> "test-id";

            // when
            String result1 = identifier.value();
            String result2 = identifier.value();
            String result3 = identifier.value();

            // then
            then(result1).isEqualTo("test-id");
            then(result2).isEqualTo("test-id");
            then(result3).isEqualTo("test-id");
        }
    }
}
