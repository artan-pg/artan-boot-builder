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

package ir.artanpg.boot.application.port.driven.transaction;

import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
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
@DisplayName("TransactionDefinition")
class TransactionDefinitionTests {

    @Nested
    @DisplayName("withDefaults()")
    class WithDefaults {

        @Test
        @DisplayName("Returns a definition populated with default values")
        void WithDefaults_ShouldReturnDefaultDefinition_WhenCalled() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // then
            then(definition.getPropagation())
                    .as("default propagation")
                    .isEqualTo(TransactionPropagation.REQUIRED);
            then(definition.getIsolation())
                    .as("default isolation")
                    .isEqualTo(TransactionIsolation.DEFAULT);
            then(definition.getTimeout())
                    .as("default timeout")
                    .isEqualTo(TransactionDefinition.DEFAULT_TIMEOUT);
            then(definition.isReadOnly())
                    .as("default readOnly flag")
                    .isFalse();
            then(definition.getName())
                    .as("default name")
                    .isEqualTo("default");
        }
    }

    @Nested
    @DisplayName("builder()")
    class BuilderFactory {

        @Test
        @DisplayName("Returns a fresh builder instance")
        void Builder_ShouldReturnFreshBuilder_WhenCalled() {
            // given
            // when
            TransactionDefinition.Builder builder = TransactionDefinition.builder();

            // then
            then(builder)
                    .as("builder instance returned by builder()")
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("getPropagation()")
    class GetPropagation {

        @Test
        @DisplayName("Returns the configured propagation")
        void GetPropagation_ShouldReturnConfiguredValue_WhenBuiltWithCustomPropagation() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRES_NEW)
                    .build();

            // when
            TransactionPropagation result = definition.getPropagation();

            // then
            then(result)
                    .as("propagation returned by getter")
                    .isEqualTo(TransactionPropagation.REQUIRES_NEW);
        }
    }

    @Nested
    @DisplayName("getIsolation()")
    class GetIsolation {

        @Test
        @DisplayName("Returns the configured isolation")
        void GetIsolation_ShouldReturnConfiguredValue_WhenBuiltWithCustomIsolation() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.SERIALIZABLE)
                    .build();

            // when
            TransactionIsolation result = definition.getIsolation();

            // then
            then(result)
                    .as("isolation returned by getter")
                    .isEqualTo(TransactionIsolation.SERIALIZABLE);
        }
    }

    @Nested
    @DisplayName("getTimeout()")
    class GetTimeout {

        @Test
        @DisplayName("Returns the configured timeout")
        void GetTimeout_ShouldReturnConfiguredValue_WhenBuiltWithCustomTimeout() {
            // given
            Duration timeout = Duration.ofMinutes(2);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .timeout(timeout)
                    .build();

            // when
            Duration result = definition.getTimeout();

            // then
            then(result)
                    .as("timeout returned by getter")
                    .isEqualTo(timeout);
        }
    }

    @Nested
    @DisplayName("isReadOnly()")
    class IsReadOnly {

        @Test
        @DisplayName("Returns true when built as read-only")
        void IsReadOnly_ShouldReturnTrue_WhenBuiltAsReadOnly() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .readOnly(true)
                    .build();

            // when
            boolean result = definition.isReadOnly();

            // then
            then(result)
                    .as("readOnly flag when built with readOnly(true)")
                    .isTrue();
        }

        @Test
        @DisplayName("Returns false when built as read-write")
        void IsReadOnly_ShouldReturnFalse_WhenBuiltAsReadWrite() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .readOnly(false)
                    .build();

            // when
            boolean result = definition.isReadOnly();

            // then
            then(result)
                    .as("readOnly flag when built with readOnly(false)")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("getName()")
    class GetName {

        @Test
        @DisplayName("Returns the configured name")
        void GetName_ShouldReturnConfiguredValue_WhenBuiltWithCustomName() {
            // given
            TransactionDefinition definition = TransactionDefinition.builder()
                    .name("order-service")
                    .build();

            // when
            String result = definition.getName();

            // then
            then(result)
                    .as("name returned by getter")
                    .isEqualTo("order-service");
        }
    }

    @Nested
    @DisplayName("Builder.propagation(TransactionPropagation)")
    class BuilderPropagation {

        @Test
        @DisplayName("Sets the propagation on the built definition")
        void Propagation_ShouldSetValue_WhenNonNull() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.MANDATORY)
                    .build();

            // then
            then(definition.getPropagation())
                    .as("propagation after builder.propagation(MANDATORY)")
                    .isEqualTo(TransactionPropagation.MANDATORY);
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws TransactionalDomainException when propagation is null")
        void Propagation_ShouldThrowTransactionalDomainException_WhenNull() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder();

            // when / then
            thenThrownBy(() -> builder.propagation(null))
                    .as("exception when propagation is null")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Propagation must not be null");
        }
    }

    @Nested
    @DisplayName("Builder.isolation(TransactionIsolation)")
    class BuilderIsolation {

        @Test
        @DisplayName("Sets the isolation on the built definition")
        void Isolation_ShouldSetValue_WhenNonNull() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.READ_COMMITTED)
                    .build();

            // then
            then(definition.getIsolation())
                    .as("isolation after builder.isolation(READ_COMMITTED)")
                    .isEqualTo(TransactionIsolation.READ_COMMITTED);
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws TransactionalDomainException when isolation is null")
        void Isolation_ShouldThrowTransactionalDomainException_WhenNull() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder();

            // when / then
            thenThrownBy(() -> builder.isolation(null))
                    .as("exception when isolation is null")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Isolation must not be null");
        }
    }

    @Nested
    @DisplayName("Builder.timeout(Duration)")
    class BuilderTimeout {

        @Test
        @DisplayName("Sets the timeout on the built definition")
        void Timeout_ShouldSetValue_WhenPositive() {
            // given
            Duration timeout = Duration.ofSeconds(10);

            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .timeout(timeout)
                    .build();

            // then
            then(definition.getTimeout())
                    .as("timeout after builder.timeout(10s)")
                    .isEqualTo(timeout);
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws TransactionalDomainException when timeout is null")
        void Timeout_ShouldThrowTransactionalDomainException_WhenNull() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder();

            // when / then
            thenThrownBy(() -> builder.timeout(null))
                    .as("exception when timeout is null")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Timeout must not be null");
        }

        @Test
        @DisplayName("Throws TransactionalDomainException when timeout is zero")
        void Timeout_ShouldThrowTransactionalDomainException_WhenZero() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder();

            // when / then
            thenThrownBy(() -> builder.timeout(Duration.ZERO))
                    .as("exception when timeout is zero")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Timeout must be positive");
        }

        @Test
        @DisplayName("Throws TransactionalDomainException when timeout is negative")
        void Timeout_ShouldThrowTransactionalDomainException_WhenNegative() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder();

            // when / then
            thenThrownBy(() -> builder.timeout(Duration.ofSeconds(-1)))
                    .as("exception when timeout is negative")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Timeout must be positive");
        }
    }

    @Nested
    @DisplayName("Builder.readOnly(boolean)")
    class BuilderReadOnly {

        @Test
        @DisplayName("Sets read-only to true")
        void ReadOnly_ShouldSetTrue_WhenTruePassed() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .readOnly(true)
                    .build();

            // then
            then(definition.isReadOnly())
                    .as("readOnly after builder.readOnly(true)")
                    .isTrue();
        }

        @Test
        @DisplayName("Sets read-only to false")
        void ReadOnly_ShouldSetFalse_WhenFalsePassed() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .readOnly(false)
                    .build();

            // then
            then(definition.isReadOnly())
                    .as("readOnly after builder.readOnly(false)")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Builder.name(String)")
    class BuilderName {

        @Test
        @DisplayName("Sets the name on the built definition")
        void Name_ShouldSetValue_WhenNonBlank() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .name("payment-tx")
                    .build();

            // then
            then(definition.getName())
                    .as("name after builder.name(payment-tx)")
                    .isEqualTo("payment-tx");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws TransactionalDomainException when name is null")
        void Name_ShouldThrowTransactionalDomainException_WhenNull() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder();

            // when / then
            thenThrownBy(() -> builder.name(null))
                    .as("exception when name is null")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("The name cannot be null or blank");
        }

        @Test
        @DisplayName("Throws TransactionalDomainException when name is blank")
        void Name_ShouldThrowTransactionalDomainException_WhenBlank() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder();

            // when / then
            thenThrownBy(() -> builder.name("   "))
                    .as("exception when name is blank")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("The name cannot be null or blank");
        }

        @Test
        @DisplayName("Throws TransactionalDomainException when name is empty")
        void Name_ShouldThrowTransactionalDomainException_WhenEmpty() {
            // given
            TransactionDefinition.Builder builder = TransactionDefinition.builder();

            // when / then
            thenThrownBy(() -> builder.name(""))
                    .as("exception when name is empty")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("The name cannot be null or blank");
        }
    }

    @Nested
    @DisplayName("Builder.build()")
    class BuilderBuild {

        @Test
        @DisplayName("Builds a fully customized immutable definition")
        void Build_ShouldReturnImmutableDefinition_WhenAllPropertiesSet() {
            // given
            Duration timeout = Duration.ofSeconds(15);

            // when
            TransactionDefinition definition = TransactionDefinition.builder()
                    .propagation(TransactionPropagation.REQUIRES_NEW)
                    .isolation(TransactionIsolation.REPEATABLE_READ)
                    .timeout(timeout)
                    .readOnly(true)
                    .name("custom-tx")
                    .build();

            // then
            then(definition.getPropagation())
                    .as("custom propagation")
                    .isEqualTo(TransactionPropagation.REQUIRES_NEW);
            then(definition.getIsolation())
                    .as("custom isolation")
                    .isEqualTo(TransactionIsolation.REPEATABLE_READ);
            then(definition.getTimeout())
                    .as("custom timeout")
                    .isEqualTo(timeout);
            then(definition.isReadOnly())
                    .as("custom readOnly")
                    .isTrue();
            then(definition.getName())
                    .as("custom name")
                    .isEqualTo("custom-tx");
        }

        @Test
        @DisplayName("Builds with defaults when no properties are customized")
        void Build_ShouldUseDefaults_WhenNoPropertiesCustomized() {
            // given
            // when
            TransactionDefinition definition = TransactionDefinition.builder().build();

            // then
            then(definition.getPropagation())
                    .as("default propagation from empty builder")
                    .isEqualTo(TransactionPropagation.REQUIRED);
            then(definition.getIsolation())
                    .as("default isolation from empty builder")
                    .isEqualTo(TransactionIsolation.DEFAULT);
            then(definition.getTimeout())
                    .as("default timeout from empty builder")
                    .isEqualTo(TransactionDefinition.DEFAULT_TIMEOUT);
            then(definition.isReadOnly())
                    .as("default readOnly from empty builder")
                    .isFalse();
            then(definition.getName())
                    .as("default name from empty builder")
                    .isEqualTo("default");
        }
    }
}
