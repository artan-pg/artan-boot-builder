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

package ir.artanpg.boot.domain.transaction;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link TransactionPropagation}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("TransactionPropagation unit tests")
class TransactionPropagationTests {

    @Nested
    @DisplayName("Enum constants")
    class EnumConstants {

        @Test
        @DisplayName("values_ShouldContainAllSevenPropagationModes_WhenEnumIsDefined")
        void values_ShouldContainAllSevenPropagationModes_WhenEnumIsDefined() {
            // given
            // when
            TransactionPropagation[] values = TransactionPropagation.values();

            // then
            then(values)
                    .hasSize(7)
                    .containsExactly(
                            TransactionPropagation.REQUIRED,
                            TransactionPropagation.SUPPORTS,
                            TransactionPropagation.MANDATORY,
                            TransactionPropagation.REQUIRES_NEW,
                            TransactionPropagation.NOT_SUPPORTED,
                            TransactionPropagation.NEVER,
                            TransactionPropagation.NESTED
                    );
        }

        @Test
        @DisplayName("valueOf_ShouldReturnCorrectConstant_WhenValidNameIsProvided")
        void valueOf_ShouldReturnCorrectConstant_WhenValidNameIsProvided() {
            // given
            String name = "REQUIRED";

            // when
            TransactionPropagation result = TransactionPropagation.valueOf(name);

            // then
            then(result).isEqualTo(TransactionPropagation.REQUIRED);
        }
    }

    @Nested
    @DisplayName("getCode()")
    class GetCode {

        @Test
        @DisplayName("getCode_ShouldReturnZero_WhenPropagationIsRequired")
        void getCode_ShouldReturnZero_WhenPropagationIsRequired() {
            // given
            TransactionPropagation propagation = TransactionPropagation.REQUIRED;

            // when
            int code = propagation.getCode();

            // then
            then(code).isZero();
        }

        @Test
        @DisplayName("getCode_ShouldReturnOne_WhenPropagationIsSupports")
        void getCode_ShouldReturnOne_WhenPropagationIsSupports() {
            // given
            TransactionPropagation propagation = TransactionPropagation.SUPPORTS;

            // when
            int code = propagation.getCode();

            // then
            then(code).isEqualTo(1);
        }

        @Test
        @DisplayName("getCode_ShouldReturnTwo_WhenPropagationIsMandatory")
        void getCode_ShouldReturnTwo_WhenPropagationIsMandatory() {
            // given
            TransactionPropagation propagation = TransactionPropagation.MANDATORY;

            // when
            int code = propagation.getCode();

            // then
            then(code).isEqualTo(2);
        }

        @Test
        @DisplayName("getCode_ShouldReturnThree_WhenPropagationIsRequiresNew")
        void getCode_ShouldReturnThree_WhenPropagationIsRequiresNew() {
            // given
            TransactionPropagation propagation = TransactionPropagation.REQUIRES_NEW;

            // when
            int code = propagation.getCode();

            // then
            then(code).isEqualTo(3);
        }

        @Test
        @DisplayName("getCode_ShouldReturnFour_WhenPropagationIsNotSupported")
        void getCode_ShouldReturnFour_WhenPropagationIsNotSupported() {
            // given
            TransactionPropagation propagation = TransactionPropagation.NOT_SUPPORTED;

            // when
            int code = propagation.getCode();

            // then
            then(code).isEqualTo(4);
        }

        @Test
        @DisplayName("getCode_ShouldReturnFive_WhenPropagationIsNever")
        void getCode_ShouldReturnFive_WhenPropagationIsNever() {
            // given
            TransactionPropagation propagation = TransactionPropagation.NEVER;

            // when
            int code = propagation.getCode();

            // then
            then(code).isEqualTo(5);
        }

        @Test
        @DisplayName("getCode_ShouldReturnSix_WhenPropagationIsNested")
        void getCode_ShouldReturnSix_WhenPropagationIsNested() {
            // given
            TransactionPropagation propagation = TransactionPropagation.NESTED;

            // when
            int code = propagation.getCode();

            // then
            then(code).isEqualTo(6);
        }
    }
}
