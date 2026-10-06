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

package ir.artanpg.boot.domain.event.retry;

import ir.artanpg.boot.domain.exception.DomainEventException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static java.lang.Double.POSITIVE_INFINITY;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link RetryPolicy}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("RetryPolicy")
class RetryPolicyTests {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw when initialInterval is null")
        void constructor_ShouldThrowDomainEventException_WhenInitialIntervalIsNull() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, null, 2.0, ofSeconds(5), 0.0, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The initialInterval cannot be null");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw when maxInterval is null")
        void constructor_ShouldThrowDomainEventException_WhenMaxIntervalIsNull() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), 2.0, null, 0.0, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The maxInterval cannot be null");
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw when deadline is null")
        void constructor_ShouldThrowDomainEventException_WhenDeadlineIsNull() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), 2.0, ofSeconds(5), 0.0, null))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The deadline cannot be null");
        }

        @Test
        @DisplayName("should throw when maxRetries is negative")
        void constructor_ShouldThrowDomainEventException_WhenMaxRetriesIsNegative() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(-1, ofMillis(100), 2.0, ofSeconds(5), 0.0, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The maxRetries cannot be negative");
        }

        @Test
        @DisplayName("should throw when initialInterval is negative")
        void constructor_ShouldThrowDomainEventException_WhenInitialIntervalIsNegative() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(-100), 2.0, ofSeconds(5), 0.0, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The initialInterval cannot be negative");
        }

        @Test
        @DisplayName("should throw when maxInterval is negative")
        void constructor_ShouldThrowDomainEventException_WhenMaxIntervalIsNegative() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), 2.0, ofMillis(-500), 0.0, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The maxInterval cannot be negative");
        }

        @Test
        @DisplayName("should throw when deadline is negative")
        void constructor_ShouldThrowDomainEventException_WhenDeadlineIsNegative() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), 2.0, ofSeconds(5), 0.0, ofMillis(-1)))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The deadline cannot be negative");
        }

        @Test
        @DisplayName("should throw when multiplier is NaN")
        void constructor_ShouldThrowDomainEventException_WhenMultiplierIsNaN() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), Double.NaN, ofSeconds(5), 0.0, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The multiplier must be a finite number >= 1.0");
        }

        @Test
        @DisplayName("should throw when multiplier is infinite")
        void constructor_ShouldThrowDomainEventException_WhenMultiplierIsInfinite() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), POSITIVE_INFINITY, ofSeconds(5), 0.0, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The multiplier must be a finite number >= 1.0");
        }

        @Test
        @DisplayName("should throw when multiplier is less than 1.0")
        void constructor_ShouldThrowDomainEventException_WhenMultiplierIsLessThanOne() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), 0.5, ofSeconds(5), 0.0, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The multiplier must be a finite number >= 1.0");
        }

        @Test
        @DisplayName("should throw when jitterFactor is NaN")
        void constructor_ShouldThrowDomainEventException_WhenJitterFactorIsNaN() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), 2.0, ofSeconds(5), Double.NaN, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The jitterFactor must be within [0.0, 1.0]");
        }

        @Test
        @DisplayName("should throw when jitterFactor is less than 0.0")
        void constructor_ShouldThrowDomainEventException_WhenJitterFactorIsNegative() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), 2.0, ofSeconds(5), -0.1, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The jitterFactor must be within [0.0, 1.0]");
        }

        @Test
        @DisplayName("should throw when jitterFactor is greater than 1.0")
        void constructor_ShouldThrowDomainEventException_WhenJitterFactorIsGreaterThanOne() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ofMillis(100), 2.0, ofSeconds(5), 1.1, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The jitterFactor must be within [0.0, 1.0]");
        }

        @Test
        @DisplayName("should throw when maxRetries is positive and initialInterval is zero")
        void constructor_ShouldThrowDomainEventException_WhenMaxRetriesPositiveAndInitialIntervalZero() {
            // given

            // when & then
            thenThrownBy(() -> new RetryPolicy(3, ZERO, 2.0, ofSeconds(5), 0.0, ZERO))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The initialInterval must be positive when retries are allowed");
        }

        @Test
        @DisplayName("should allow zero initialInterval when maxRetries is zero")
        void constructor_ShouldAllowZeroInitialInterval_WhenMaxRetriesIsZero() {
            // given

            // when
            RetryPolicy policy = new RetryPolicy(0, ZERO, 1.0, ofSeconds(5), 0.0, ZERO);

            // then
            then(policy.maxRetries()).isZero();
            then(policy.initialInterval()).isZero();
        }

        @Test
        @DisplayName("should allow jitterFactor exactly 0.0")
        void constructor_ShouldAllowJitterFactorZero_WhenProvided() {
            // given

            // when
            RetryPolicy policy = new RetryPolicy(3, ofMillis(100), 2.0, ofSeconds(5), 0.0, ZERO);

            // then
            then(policy.jitterFactor()).isZero();
        }

        @Test
        @DisplayName("should allow jitterFactor exactly 1.0")
        void constructor_ShouldAllowJitterFactorOne_WhenProvided() {
            // given

            // when
            RetryPolicy policy = new RetryPolicy(3, ofMillis(100), 2.0, ofSeconds(5), 1.0, ZERO);

            // then
            then(policy.jitterFactor()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("should allow multiplier exactly 1.0")
        void constructor_ShouldAllowMultiplierOne_WhenProvided() {
            // given

            // when
            RetryPolicy policy = new RetryPolicy(3, ofMillis(100), 1.0, ofSeconds(5), 0.0, ZERO);

            // then
            then(policy.multiplier()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("none()")
    class NoneFactory {

        @Test
        @DisplayName("should return policy with zero retries")
        void none_ShouldReturnPolicyWithZeroRetries_WhenCalled() {
            // given

            // when
            RetryPolicy policy = RetryPolicy.none();

            // then
            then(policy.maxRetries()).isZero();
            then(policy.enabled()).isFalse();
        }

        @Test
        @DisplayName("should return same instance on multiple calls")
        void none_ShouldReturnSameInstance_WhenCalledMultipleTimes() {
            // given

            // when
            RetryPolicy policy1 = RetryPolicy.none();
            RetryPolicy policy2 = RetryPolicy.none();

            // then
            then(policy1).isSameAs(policy2);
        }
    }

    @Nested
    @DisplayName("fixed()")
    class FixedFactory {

        @Test
        @DisplayName("should create policy with constant delay")
        void fixed_ShouldCreatePolicyWithConstantDelay_WhenCalled() {
            // given
            int maxRetries = 5;
            Duration interval = ofMillis(200);

            // when
            RetryPolicy policy = RetryPolicy.fixed(maxRetries, interval);

            // then
            then(policy.maxRetries()).isEqualTo(maxRetries);
            then(policy.initialInterval()).isEqualTo(interval);
            then(policy.multiplier()).isEqualTo(1.0);
            then(policy.maxInterval()).isEqualTo(interval);
            then(policy.jitterFactor()).isZero();
            then(policy.deadline()).isEqualTo(ZERO);
        }
    }

    @Nested
    @DisplayName("exponential()")
    class ExponentialFactory {

        @Test
        @DisplayName("should create policy with all parameters")
        void exponential_ShouldCreatePolicyWithAllParameters_WhenCalled() {
            // given
            int maxRetries = 3;
            Duration initialInterval = ofMillis(100);
            double multiplier = 2.0;
            Duration maxInterval = ofSeconds(10);
            double jitterFactor = 0.3;
            Duration deadline = Duration.ofMinutes(1);

            // when
            RetryPolicy policy =
                    RetryPolicy
                            .exponential(maxRetries, initialInterval, multiplier, maxInterval, jitterFactor, deadline);

            // then
            then(policy.maxRetries()).isEqualTo(maxRetries);
            then(policy.initialInterval()).isEqualTo(initialInterval);
            then(policy.multiplier()).isEqualTo(multiplier);
            then(policy.maxInterval()).isEqualTo(maxInterval);
            then(policy.jitterFactor()).isEqualTo(jitterFactor);
            then(policy.deadline()).isEqualTo(deadline);
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("should create policy with default values")
        void builder_ShouldCreatePolicyWithDefaultValues_WhenNoSettersCalled() {
            // given

            // when
            RetryPolicy policy = RetryPolicy.builder().build();

            // then
            then(policy.maxRetries()).isEqualTo(3);
            then(policy.initialInterval()).isEqualTo(ofMillis(100));
            then(policy.multiplier()).isEqualTo(2.0);
            then(policy.maxInterval()).isEqualTo(ofSeconds(5));
            then(policy.jitterFactor()).isZero();
            then(policy.deadline()).isEqualTo(ZERO);
        }

        @Test
        @DisplayName("should set maxRetries")
        void builder_maxRetries_ShouldSetValue_WhenCalled() {
            // given

            // when
            RetryPolicy policy = RetryPolicy.builder().maxRetries(10).build();

            // then
            then(policy.maxRetries()).isEqualTo(10);
        }

        @Test
        @DisplayName("should set initialInterval")
        void builder_initialInterval_ShouldSetValue_WhenCalled() {
            // given
            Duration interval = ofSeconds(2);

            // when
            RetryPolicy policy = RetryPolicy.builder().initialInterval(interval).build();

            // then
            then(policy.initialInterval()).isEqualTo(interval);
        }

        @Test
        @DisplayName("should set multiplier")
        void builder_multiplier_ShouldSetValue_WhenCalled() {
            // given

            // when
            RetryPolicy policy = RetryPolicy.builder().multiplier(3.0).build();

            // then
            then(policy.multiplier()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("should set maxInterval")
        void builder_maxInterval_ShouldSetValue_WhenCalled() {
            // given
            Duration maxInterval = ofSeconds(30);

            // when
            RetryPolicy policy = RetryPolicy.builder().maxInterval(maxInterval).build();

            // then
            then(policy.maxInterval()).isEqualTo(maxInterval);
        }

        @Test
        @DisplayName("should set jitterFactor")
        void builder_jitterFactor_ShouldSetValue_WhenCalled() {
            // given

            // when
            RetryPolicy policy = RetryPolicy.builder().jitterFactor(0.5).build();

            // then
            then(policy.jitterFactor()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("should set deadline")
        void builder_deadline_ShouldSetValue_WhenCalled() {
            // given
            Duration deadline = Duration.ofMinutes(5);

            // when
            RetryPolicy policy = RetryPolicy.builder().deadline(deadline).build();

            // then
            then(policy.deadline()).isEqualTo(deadline);
        }

        @Test
        @DisplayName("should support method chaining")
        void builder_ShouldSupportMethodChaining_WhenAllSettersCalled() {
            // given

            // when
            RetryPolicy.Builder builder = RetryPolicy.builder();
            RetryPolicy.Builder returned = builder
                    .maxRetries(5)
                    .initialInterval(ofMillis(200))
                    .multiplier(1.5)
                    .maxInterval(ofSeconds(10))
                    .jitterFactor(0.2)
                    .deadline(Duration.ofMinutes(1));

            // then
            then(returned).isSameAs(builder);
        }
    }

    @Nested
    @DisplayName("enabled")
    class Enabled {

        @Test
        @DisplayName("should return true when maxRetries is positive")
        void enabled_ShouldReturnTrue_WhenMaxRetriesIsPositive() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(3, ofMillis(100));

            // when
            boolean result = policy.enabled();

            // then
            then(result).isTrue();
        }

        @Test
        @DisplayName("should return false when maxRetries is zero")
        void enabled_ShouldReturnFalse_WhenMaxRetriesIsZero() {
            // given
            RetryPolicy policy = RetryPolicy.none();

            // when
            boolean result = policy.enabled();

            // then
            then(result).isFalse();
        }
    }

    @Nested
    @DisplayName("baseIntervalFor")
    class BaseIntervalFor {

        @Test
        @DisplayName("should return zero when policy is disabled")
        void baseIntervalFor_ShouldReturnZero_WhenPolicyIsDisabled() {
            // given
            RetryPolicy policy = RetryPolicy.none();

            // when
            Duration result = policy.baseIntervalFor(1);

            // then
            then(result).isZero();
        }

        @Test
        @DisplayName("should return zero when attemptNumber is less than 1")
        void baseIntervalFor_ShouldReturnZero_WhenAttemptNumberIsLessThanOne() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(3, ofMillis(100));

            // when
            Duration result = policy.baseIntervalFor(0);

            // then
            then(result).isZero();
        }

        @Test
        @DisplayName("should return initialInterval when multiplier is 1.0 and initialInterval less than maxInterval")
        void baseIntervalFor_ShouldReturnInitialInterval_WhenMultiplierIsOneAndInitialLessThanMax() {
            // given
            RetryPolicy policy = RetryPolicy.builder()
                    .maxRetries(3)
                    .initialInterval(ofSeconds(1))
                    .multiplier(1.0)
                    .maxInterval(ofSeconds(5))
                    .build();

            // when
            Duration result = policy.baseIntervalFor(1);

            // then
            then(result).isEqualTo(ofSeconds(1));
        }

        @Test
        @DisplayName("should return maxInterval when multiplier is 1.0 and initialInterval greater than maxInterval")
        void baseIntervalFor_ShouldReturnMaxInterval_WhenMultiplierIsOneAndInitialGreaterThanMax() {
            // given
            RetryPolicy policy = RetryPolicy.builder()
                    .maxRetries(3)
                    .initialInterval(ofSeconds(10))
                    .multiplier(1.0)
                    .maxInterval(ofSeconds(5))
                    .build();

            // when
            Duration result = policy.baseIntervalFor(1);

            // then
            then(result).isEqualTo(ofSeconds(5));
        }

        @Test
        @DisplayName("should return exponential interval when within maxInterval")
        void baseIntervalFor_ShouldReturnExponentialInterval_WhenWithinMaxInterval() {
            // given
            RetryPolicy policy = RetryPolicy.exponential(5, ofSeconds(1), 2.0, ofSeconds(30), 0.0, ZERO);

            // when
            Duration result = policy.baseIntervalFor(3);

            // then
            // attempt 3: factor = 2^(3-1) = 4, millis = 1000 * 4 = 4000ms
            then(result).isEqualTo(ofMillis(4000));
        }

        @Test
        @DisplayName("should return maxInterval when calculated millis exceeds maxInterval")
        void baseIntervalFor_ShouldReturnMaxInterval_WhenCalculatedMillisExceedsMax() {
            // given
            RetryPolicy policy = RetryPolicy.exponential(10, ofSeconds(1), 2.0, ofSeconds(5), 0.0, ZERO);

            // when
            Duration result = policy.baseIntervalFor(5);

            // then
            // attempt 5: factor = 2^4 = 16, millis = 16000ms > 5000ms
            then(result).isEqualTo(ofSeconds(5));
        }

        @Test
        @DisplayName("should return maxInterval when calculated millis is infinite")
        void baseIntervalFor_ShouldReturnMaxInterval_WhenCalculatedMillisIsInfinite() {
            // given
            RetryPolicy policy = RetryPolicy.exponential(500, ofSeconds(1), 10.0, ofSeconds(30), 0.0, ZERO);

            // when
            Duration result = policy.baseIntervalFor(400);

            // then
            // 10^399 * 1000 = Infinity
            then(result).isEqualTo(ofSeconds(30));
        }
    }

    @Nested
    @DisplayName("nextInterval")
    class NextInterval {

        @Test
        @DisplayName("should return zero when base interval is zero")
        void nextInterval_ShouldReturnZero_WhenBaseIntervalIsZero() {
            // given
            RetryPolicy policy = RetryPolicy.none();

            // when
            Duration result = policy.nextInterval(1);

            // then
            then(result).isZero();
        }

        @Test
        @DisplayName("should return base interval when jitterFactor is zero")
        void nextInterval_ShouldReturnBaseInterval_WhenJitterFactorIsZero() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(3, ofMillis(500));

            // when
            Duration result = policy.nextInterval(1);

            // then
            then(result).isEqualTo(ofMillis(500));
        }

        @Test
        @DisplayName("should return jittered interval within bounds when jitterFactor is positive")
        void nextInterval_ShouldReturnJitteredInterval_WhenJitterFactorIsPositive() {
            // given
            RetryPolicy policy = RetryPolicy.exponential(3, ofMillis(1000), 1.0, ofSeconds(5), 0.5, ZERO);

            // when
            Duration result = policy.nextInterval(1);

            // then
            // base = 1000ms, jitter = 0.5, range = [500, 1500]
            then(result.toMillis()).isBetween(500L, 1500L);
        }

        @Test
        @DisplayName("should cap jittered interval at maxInterval")
        void nextInterval_ShouldCapAtMaxInterval_WhenJitteredExceedsMax() {
            // given
            RetryPolicy policy = RetryPolicy.exponential(3, ofMillis(1000), 1.0, ofMillis(1000), 0.5, ZERO);

            // when
            Duration result = policy.nextInterval(1);

            // then
            then(result.toMillis()).isLessThanOrEqualTo(1000L);
        }
    }

    @Nested
    @DisplayName("newState")
    class NewState {

        @Test
        @DisplayName("should return non-null RetryState")
        void newState_ShouldReturnNonNullRetryState_WhenCalled() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(3, ofMillis(100));

            // when
            RetryState state = policy.newState();

            // then
            then(state).isNotNull();
            then(state.getPolicy()).isSameAs(policy);
        }
    }

    @Nested
    @DisplayName("Record methods")
    class RecordMethods {

        @Test
        @DisplayName("equals should return true for same coordinates")
        void equals_ShouldReturnTrue_WhenSameCoordinates() {
            // given
            RetryPolicy p1 = RetryPolicy.fixed(3, ofMillis(100));
            RetryPolicy p2 = RetryPolicy.fixed(3, ofMillis(100));

            // when & then
            then(p1).isEqualTo(p2);
        }

        @Test
        @DisplayName("equals should return false for different coordinates")
        void equals_ShouldReturnFalse_WhenDifferentCoordinates() {
            // given
            RetryPolicy p1 = RetryPolicy.fixed(3, ofMillis(100));
            RetryPolicy p2 = RetryPolicy.fixed(5, ofMillis(100));

            // when & then
            then(p1).isNotEqualTo(p2);
        }

        @Test
        @DisplayName("hashCode should be consistent for equal instances")
        void hashCode_ShouldBeConsistent_WhenEqualInstances() {
            // given
            RetryPolicy p1 = RetryPolicy.fixed(3, ofMillis(100));
            RetryPolicy p2 = RetryPolicy.fixed(3, ofMillis(100));

            // when & then
            then(p1.hashCode()).isEqualTo(p2.hashCode());
        }

        @Test
        @DisplayName("toString should contain field values")
        void toString_ShouldContainFieldValues_WhenCalled() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(3, ofMillis(100));

            // when
            String result = policy.toString();

            // then
            then(result).contains("maxRetries=3");
            then(result).contains("initialInterval=");
        }
    }
}
