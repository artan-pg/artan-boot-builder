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

import ir.artanpg.boot.domain.transaction.exception.TransactionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link TransactionDefinition}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("TransactionDefinition unit tests")
class TransactionDefinitionTests {

    @Nested
    @DisplayName("withDefaults()")
    class WithDefaults {

        @Test
        @DisplayName("withDefaults_ShouldReturnDefinitionWithRequiredPropagation_WhenCalled")
        void withDefaults_ShouldReturnDefinitionWithRequiredPropagation_WhenCalled() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // then
            then(definition.getPropagation()).isEqualTo(TransactionPropagation.REQUIRED);
        }

        @Test
        @DisplayName("withDefaults_ShouldReturnDefinitionWithDefaultIsolation_WhenCalled")
        void withDefaults_ShouldReturnDefinitionWithDefaultIsolation_WhenCalled() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // then
            then(definition.getIsolation()).isEqualTo(TransactionIsolation.DEFAULT);
        }

        @Test
        @DisplayName("withDefaults_ShouldReturnDefinitionWithThirtySecondsTimeout_WhenCalled")
        void withDefaults_ShouldReturnDefinitionWithThirtySecondsTimeout_WhenCalled() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // then
            then(definition.getTimeout()).isEqualTo(Duration.ofSeconds(30L));
        }

        @Test
        @DisplayName("withDefaults_ShouldReturnDefinitionWithReadOnlyFalse_WhenCalled")
        void withDefaults_ShouldReturnDefinitionWithReadOnlyFalse_WhenCalled() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // then
            then(definition.isReadOnly()).isFalse();
        }

        @Test
        @DisplayName("withDefaults_ShouldReturnDefinitionWithDefaultName_WhenCalled")
        void withDefaults_ShouldReturnDefinitionWithDefaultName_WhenCalled() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // then
            then(definition.getName()).isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("Builder - happy path")
    class BuilderHappyPath {

        @Test
        @DisplayName("build_ShouldReturnDefinitionWithConfiguredPropagation_WhenPropagationIsSet")
        void build_ShouldReturnDefinitionWithConfiguredPropagation_WhenPropagationIsSet() {
            // given
            TransactionPropagation expected = TransactionPropagation.REQUIRES_NEW;

            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(expected)
                    .build();

            // then
            then(definition.getPropagation()).isEqualTo(expected);
        }

        @Test
        @DisplayName("build_ShouldReturnDefinitionWithConfiguredIsolation_WhenIsolationIsSet")
        void build_ShouldReturnDefinitionWithConfiguredIsolation_WhenIsolationIsSet() {
            // given
            TransactionIsolation expected = TransactionIsolation.SERIALIZABLE;

            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(expected)
                    .build();

            // then
            then(definition.getIsolation()).isEqualTo(expected);
        }

        @Test
        @DisplayName("build_ShouldReturnDefinitionWithConfiguredTimeout_WhenTimeoutIsSet")
        void build_ShouldReturnDefinitionWithConfiguredTimeout_WhenTimeoutIsSet() {
            // given
            Duration expected = Duration.ofMinutes(2);

            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .timeout(expected)
                    .build();

            // then
            then(definition.getTimeout()).isEqualTo(expected);
        }

        @Test
        @DisplayName("build_ShouldReturnDefinitionWithReadOnlyTrue_WhenReadOnlyIsTrue")
        void build_ShouldReturnDefinitionWithReadOnlyTrue_WhenReadOnlyIsTrue() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .readOnly(true)
                    .build();

            // then
            then(definition.isReadOnly()).isTrue();
        }

        @Test
        @DisplayName("build_ShouldReturnDefinitionWithConfiguredName_WhenNameIsSet")
        void build_ShouldReturnDefinitionWithConfiguredName_WhenNameIsSet() {
            // given
            String expected = "order-placement";

            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .name(expected)
                    .build();

            // then
            then(definition.getName()).isEqualTo(expected);
        }

        @Test
        @DisplayName("build_ShouldReturnFullyCustomizedDefinition_WhenAllAttributesAreSet")
        void build_ShouldReturnFullyCustomizedDefinition_WhenAllAttributesAreSet() {
            // given
            TransactionPropagation propagation = TransactionPropagation.NESTED;
            TransactionIsolation isolation = TransactionIsolation.REPEATABLE_READ;
            Duration timeout = Duration.ofSeconds(45);
            boolean readOnly = true;
            String name = "inventory-update";

            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(propagation)
                    .isolation(isolation)
                    .timeout(timeout)
                    .readOnly(readOnly)
                    .name(name)
                    .build();

            // then
            then(definition.getPropagation()).isEqualTo(propagation);
            then(definition.getIsolation()).isEqualTo(isolation);
            then(definition.getTimeout()).isEqualTo(timeout);
            then(definition.isReadOnly()).isTrue();
            then(definition.getName()).isEqualTo(name);
        }

        @Test
        @DisplayName("builder_ShouldReturnNewBuilderInstance_WhenCalled")
        void builder_ShouldReturnNewBuilderInstance_WhenCalled() {
            // given
            // when
            TransactionDefinition.Builder builder1 = TransactionDefinition.builder();
            TransactionDefinition.Builder builder2 = TransactionDefinition.builder();

            // then
            then(builder1).isNotNull().isNotSameAs(builder2);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Nested
    @DisplayName("Builder - validation")
    class BuilderValidation {

        @Test
        @DisplayName("build_ShouldThrowTransactionException_WhenPropagationIsNull")
        void build_ShouldThrowTransactionException_WhenPropagationIsNull() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder()
                    .propagation(null);

            // when / then
            thenThrownBy(builder::build)
                    .isInstanceOf(TransactionException.class)
                    .hasMessage("The propagation of transaction cannot be null");
        }

        @Test
        @DisplayName("build_ShouldThrowTransactionException_WhenIsolationIsNull")
        void build_ShouldThrowTransactionException_WhenIsolationIsNull() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder()
                    .isolation(null);

            // when / then
            thenThrownBy(builder::build)
                    .isInstanceOf(TransactionException.class)
                    .hasMessage("The isolation of transaction cannot be null");
        }

        @Test
        @DisplayName("build_ShouldThrowTransactionException_WhenTimeoutIsNull")
        void build_ShouldThrowTransactionException_WhenTimeoutIsNull() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder()
                    .timeout(null);

            // when / then
            thenThrownBy(builder::build)
                    .isInstanceOf(TransactionException.class)
                    .hasMessage("The timeout of transaction cannot be null");
        }

        @Test
        @DisplayName("build_ShouldThrowTransactionException_WhenNameIsNull")
        void build_ShouldThrowTransactionException_WhenNameIsNull() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder()
                    .name(null);

            // when / then
            thenThrownBy(builder::build)
                    .isInstanceOf(TransactionException.class)
                    .hasMessage("The name of transaction cannot be null or blank");
        }

        @Test
        @DisplayName("build_ShouldThrowTransactionException_WhenNameIsBlank")
        void build_ShouldThrowTransactionException_WhenNameIsBlank() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder()
                    .name("   ");

            // when / then
            thenThrownBy(builder::build)
                    .isInstanceOf(TransactionException.class)
                    .hasMessage("The name of transaction cannot be null or blank");
        }

        @Test
        @DisplayName("build_ShouldThrowTransactionException_WhenNameIsEmpty")
        void build_ShouldThrowTransactionException_WhenNameIsEmpty() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder()
                    .name("");

            // when / then
            thenThrownBy(builder::build)
                    .isInstanceOf(TransactionException.class)
                    .hasMessage("The name of transaction cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("equals() and hashCode()")
    class EqualsAndHashCode {

        @SuppressWarnings({"EqualsWithItself", "ConstantValue"})
        @Test
        @DisplayName("equals_ShouldReturnTrue_WhenComparedWithSameInstance")
        void equals_ShouldReturnTrue_WhenComparedWithSameInstance() {
            // given
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            boolean result = definition.equals(definition);

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("equals_ShouldReturnTrue_WhenTwoDefinitionsHaveIdenticalAttributes")
        void equals_ShouldReturnTrue_WhenTwoDefinitionsHaveIdenticalAttributes() {
            // given
            TransactionDefinition first = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRED)
                    .isolation(TransactionIsolation.READ_COMMITTED)
                    .timeout(Duration.ofSeconds(10))
                    .readOnly(true)
                    .name("tx-1")
                    .build();
            TransactionDefinition second = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRED)
                    .isolation(TransactionIsolation.READ_COMMITTED)
                    .timeout(Duration.ofSeconds(10))
                    .readOnly(true)
                    .name("tx-1")
                    .build();

            // when
            boolean result = first.equals(second);

            // then
            then(result).isTrue();
            then(first.hashCode()).isEqualTo(second.hashCode());
        }

        @SuppressWarnings("ConstantValue")
        @Test
        @DisplayName("equals_ShouldReturnFalse_WhenComparedWithNull")
        void equals_ShouldReturnFalse_WhenComparedWithNull() {
            // given
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            boolean result = definition.equals(null);

            // then
            then(result).isFalse();
        }

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        @Test
        @DisplayName("equals_ShouldReturnFalse_WhenComparedWithDifferentType")
        void equals_ShouldReturnFalse_WhenComparedWithDifferentType() {
            // given
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            boolean result = definition.equals("not-a-definition");

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("equals_ShouldReturnFalse_WhenPropagationDiffers")
        void equals_ShouldReturnFalse_WhenPropagationDiffers() {
            // given
            TransactionDefinition first = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRED)
                    .build();
            TransactionDefinition second = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRES_NEW)
                    .build();

            // when
            boolean result = first.equals(second);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("equals_ShouldReturnFalse_WhenIsolationDiffers")
        void equals_ShouldReturnFalse_WhenIsolationDiffers() {
            // given
            TransactionDefinition first = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.DEFAULT)
                    .build();
            TransactionDefinition second = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.SERIALIZABLE)
                    .build();

            // when
            boolean result = first.equals(second);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("equals_ShouldReturnFalse_WhenTimeoutDiffers")
        void equals_ShouldReturnFalse_WhenTimeoutDiffers() {
            // given
            TransactionDefinition first = TransactionDefinition.builder()
                    .timeout(Duration.ofSeconds(30))
                    .build();
            TransactionDefinition second = TransactionDefinition.builder()
                    .timeout(Duration.ofSeconds(60))
                    .build();

            // when
            boolean result = first.equals(second);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("equals_ShouldReturnFalse_WhenReadOnlyDiffers")
        void equals_ShouldReturnFalse_WhenReadOnlyDiffers() {
            // given
            TransactionDefinition first = TransactionDefinition.builder()
                    .readOnly(false)
                    .build();
            TransactionDefinition second = TransactionDefinition.builder()
                    .readOnly(true)
                    .build();

            // when
            boolean result = first.equals(second);

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("equals_ShouldReturnFalse_WhenNameDiffers")
        void equals_ShouldReturnFalse_WhenNameDiffers() {
            // given
            TransactionDefinition first = TransactionDefinition.builder()
                    .name("tx-a")
                    .build();
            TransactionDefinition second = TransactionDefinition.builder()
                    .name("tx-b")
                    .build();

            // when
            boolean result = first.equals(second);

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToString {

        @Test
        @DisplayName("toString_ShouldContainAllAttributeValues_WhenCalled")
        void toString_ShouldContainAllAttributeValues_WhenCalled() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.MANDATORY)
                    .isolation(TransactionIsolation.READ_UNCOMMITTED)
                    .timeout(Duration.ofSeconds(15))
                    .readOnly(true)
                    .name("audit-tx")
                    .build();

            // when
            String result = definition.toString();

            // then
            then(result)
                    .contains("TransactionDefinition")
                    .contains("propagation=MANDATORY")
                    .contains("isolation=READ_UNCOMMITTED")
                    .contains("timeout=PT15S")
                    .contains("readOnly=true")
                    .contains("name='audit-tx'");
        }
    }
}
