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
 * Unit tests for {@link TransactionIsolation}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("TransactionIsolation unit tests")
class TransactionIsolationTests {

    @Nested
    @DisplayName("Enum constants")
    class EnumConstants {

        @Test
        @DisplayName("values_ShouldContainAllFiveIsolationLevels_WhenEnumIsDefined")
        void values_ShouldContainAllFiveIsolationLevels_WhenEnumIsDefined() {
            // given
            // when
            TransactionIsolation[] values = TransactionIsolation.values();

            // then
            then(values)
                    .hasSize(5)
                    .containsExactly(
                            TransactionIsolation.READ_UNCOMMITTED,
                            TransactionIsolation.READ_COMMITTED,
                            TransactionIsolation.REPEATABLE_READ,
                            TransactionIsolation.SERIALIZABLE,
                            TransactionIsolation.DEFAULT
                    );
        }

        @Test
        @DisplayName("valueOf_ShouldReturnCorrectConstant_WhenValidNameIsProvided")
        void valueOf_ShouldReturnCorrectConstant_WhenValidNameIsProvided() {
            // given
            String name = "READ_COMMITTED";

            // when
            TransactionIsolation result = TransactionIsolation.valueOf(name);

            // then
            then(result).isEqualTo(TransactionIsolation.READ_COMMITTED);
        }
    }

    @Nested
    @DisplayName("getCode()")
    class GetCode {

        @Test
        @DisplayName("getCode_ShouldReturnOne_WhenIsolationIsReadUncommitted")
        void getCode_ShouldReturnOne_WhenIsolationIsReadUncommitted() {
            // given
            TransactionIsolation isolation = TransactionIsolation.READ_UNCOMMITTED;

            // when
            int code = isolation.getCode();

            // then
            then(code).isEqualTo(1);
        }

        @Test
        @DisplayName("getCode_ShouldReturnTwo_WhenIsolationIsReadCommitted")
        void getCode_ShouldReturnTwo_WhenIsolationIsReadCommitted() {
            // given
            TransactionIsolation isolation = TransactionIsolation.READ_COMMITTED;

            // when
            int code = isolation.getCode();

            // then
            then(code).isEqualTo(2);
        }

        @Test
        @DisplayName("getCode_ShouldReturnFour_WhenIsolationIsRepeatableRead")
        void getCode_ShouldReturnFour_WhenIsolationIsRepeatableRead() {
            // given
            TransactionIsolation isolation = TransactionIsolation.REPEATABLE_READ;

            // when
            int code = isolation.getCode();

            // then
            then(code).isEqualTo(4);
        }

        @Test
        @DisplayName("getCode_ShouldReturnEight_WhenIsolationIsSerializable")
        void getCode_ShouldReturnEight_WhenIsolationIsSerializable() {
            // given
            TransactionIsolation isolation = TransactionIsolation.SERIALIZABLE;

            // when
            int code = isolation.getCode();

            // then
            then(code).isEqualTo(8);
        }

        @Test
        @DisplayName("getCode_ShouldReturnMinusOne_WhenIsolationIsDefault")
        void getCode_ShouldReturnMinusOne_WhenIsolationIsDefault() {
            // given
            TransactionIsolation isolation = TransactionIsolation.DEFAULT;

            // when
            int code = isolation.getCode();

            // then
            then(code).isEqualTo(-1);
        }
    }
}
