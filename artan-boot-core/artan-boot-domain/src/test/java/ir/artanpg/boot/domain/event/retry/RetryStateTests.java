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

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofNanos;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Unit tests for {@link RetryState}.
 *
 * @author Mohammad Yazdian
 */
@DisplayName("RetryState")
class RetryStateTests {

    private static final RetryPolicy DEFAULT_POLICY = RetryPolicy.fixed(3, ofMillis(100));

    // ========================================================================================
    // Constructor
    // ========================================================================================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("should throw when policy is null")
        void constructor_ShouldThrowDomainEventException_WhenPolicyIsNull() {
            // given

            // when & then
            thenThrownBy(() -> new RetryState(null))
                    .isInstanceOf(DomainEventException.class)
                    .hasMessage("The policy cannot be null");
        }

        @Test
        @DisplayName("should create state with valid policy")
        void constructor_ShouldCreateState_WhenPolicyIsValid() {
            // given
            // nothing

            // when
            RetryState state = new RetryState(DEFAULT_POLICY);

            // then
            then(state).isNotNull();
            then(state.getPolicy()).isSameAs(DEFAULT_POLICY);
        }
    }

    // ========================================================================================
    // getPolicy
    // ========================================================================================

    @Nested
    @DisplayName("getPolicy")
    class GetPolicy {

        @Test
        @DisplayName("should return the bound policy")
        void getPolicy_ShouldReturnBoundPolicy_WhenCalled() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);

            // when
            RetryPolicy policy = state.getPolicy();

            // then
            then(policy).isSameAs(DEFAULT_POLICY);
        }
    }

    // ========================================================================================
    // beginAttempt
    // ========================================================================================

    @Nested
    @DisplayName("beginAttempt")
    class BeginAttempt {

        @Test
        @DisplayName("should increment attemptsExecuted on first call")
        void beginAttempt_ShouldIncrementAttemptsExecuted_WhenFirstCall() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);

            // when
            state.beginAttempt();

            // then
            then(state.getAttemptsExecuted()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment attemptsExecuted on subsequent calls")
        void beginAttempt_ShouldIncrementAttemptsExecuted_WhenSubsequentCalls() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);

            // when
            state.beginAttempt();
            state.beginAttempt();
            state.beginAttempt();

            // then
            then(state.getAttemptsExecuted()).isEqualTo(3);
        }

        @Test
        @DisplayName("should clear lastAttemptFailed flag")
        void beginAttempt_ShouldClearLastAttemptFailed_WhenCalled() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);
            state.beginAttempt();
            state.onFailure(false);

            // when
            state.beginAttempt();

            // then
            then(state.isLastAttemptFailed()).isFalse();
        }
    }

    // ========================================================================================
    // onFailure
    // ========================================================================================

    @Nested
    @DisplayName("onFailure")
    class OnFailure {

        @Test
        @DisplayName("should return false and not exhaust when retryRequested is false")
        void onFailure_ShouldReturnFalseAndNotExhaust_WhenRetryRequestedIsFalse() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);
            state.beginAttempt();

            // when
            boolean result = state.onFailure(false);

            // then
            then(result).isFalse();
            then(state.isLastAttemptFailed()).isTrue();
            then(state.isExhausted()).isFalse();
        }

        @Test
        @DisplayName("should return false and exhaust when maxRetries is zero")
        void onFailure_ShouldReturnFalseAndExhaust_WhenMaxRetriesIsZero() {
            // given
            RetryPolicy noRetryPolicy = RetryPolicy.none();
            RetryState state = new RetryState(noRetryPolicy);
            state.beginAttempt();

            // when
            boolean result = state.onFailure(true);

            // then
            then(result).isFalse();
            then(state.isExhausted()).isTrue();
        }

        @Test
        @DisplayName("should return false and exhaust when retries are exhausted")
        void onFailure_ShouldReturnFalseAndExhaust_WhenRetriesAreExhausted() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(2, ofMillis(100));
            RetryState state = new RetryState(policy);

            // when
            state.beginAttempt();
            state.onFailure(true); // retry 1 granted
            state.beginAttempt();
            state.onFailure(true); // retry 2 granted
            state.beginAttempt();
            boolean result = state.onFailure(true); // no more retries

            // then
            then(result).isFalse();
            then(state.isExhausted()).isTrue();
            then(state.getRetriesGranted()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return false and exhaust when deadline is elapsed")
        void onFailure_ShouldReturnFalseAndExhaust_WhenDeadlineIsElapsed() {
            // given
            RetryPolicy policy = RetryPolicy.exponential(10, ofMillis(100), 2.0, ofSeconds(5), 0.0, ofNanos(1));

            RetryState state = new RetryState(policy);
            state.beginAttempt();

            // when & then
            await()
                    .atMost(ofMillis(100))
                    .pollInterval(ofMillis(1))
                    .untilAsserted(() -> {
                        boolean result = state.onFailure(true);
                        then(result).isFalse();
                        then(state.isExhausted()).isTrue();
                    });
        }

        @Test
        @DisplayName("should return true and grant retry when budget is available")
        void onFailure_ShouldReturnTrueAndGrantRetry_WhenBudgetIsAvailable() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);
            state.beginAttempt();

            // when
            boolean result = state.onFailure(true);

            // then
            then(result).isTrue();
            then(state.getRetriesGranted()).isEqualTo(1);
            then(state.isExhausted()).isFalse();
        }

        @Test
        @DisplayName("no-arg onFailure should delegate to onFailure(true)")
        void onFailure_ShouldDelegateToOnFailureTrue_WhenNoArgCalled() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);
            state.beginAttempt();

            // when
            boolean result = state.onFailure();

            // then
            then(result).isTrue();
            then(state.getRetriesGranted()).isEqualTo(1);
        }
    }

    // ========================================================================================
    // clearRetryRequest
    // ========================================================================================

    @Nested
    @DisplayName("clearRetryRequest")
    class ClearRetryRequest {

        @Test
        @DisplayName("should clear lastAttemptFailed flag")
        void clearRetryRequest_ShouldClearLastAttemptFailed_WhenCalled() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);
            state.beginAttempt();
            state.onFailure(false);
            then(state.isLastAttemptFailed()).isTrue();

            // when
            state.clearRetryRequest();

            // then
            then(state.isLastAttemptFailed()).isFalse();
        }
    }

    // ========================================================================================
    // Getters
    // ========================================================================================

    @Nested
    @DisplayName("Getters")
    class Getters {

        @Test
        @DisplayName("getAttemptsExecuted should return zero initially")
        void getAttemptsExecuted_ShouldReturnZero_WhenInitiallyCreated() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);

            // when
            int attempts = state.getAttemptsExecuted();

            // then
            then(attempts).isZero();
        }

        @Test
        @DisplayName("getRetriesGranted should return zero initially")
        void getRetriesGranted_ShouldReturnZero_WhenInitiallyCreated() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);

            // when
            int retries = state.getRetriesGranted();

            // then
            then(retries).isZero();
        }

        @Test
        @DisplayName("remainingRetries should return maxRetries initially")
        void remainingRetries_ShouldReturnMaxRetries_WhenInitiallyCreated() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);

            // when
            int remaining = state.remainingRetries();

            // then
            then(remaining).isEqualTo(DEFAULT_POLICY.maxRetries());
        }

        @Test
        @DisplayName("remainingRetries should return zero when all retries used")
        void remainingRetries_ShouldReturnZero_WhenAllRetriesUsed() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(2, ofMillis(100));
            RetryState state = new RetryState(policy);
            state.beginAttempt();
            state.onFailure(true);
            state.beginAttempt();
            state.onFailure(true);

            // when
            int remaining = state.remainingRetries();

            // then
            then(remaining).isZero();
        }

        @Test
        @DisplayName("remainingRetries should never return negative")
        void remainingRetries_ShouldNeverReturnNegative_WhenOverExhausted() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(1, ofMillis(100));
            RetryState state = new RetryState(policy);
            state.beginAttempt();
            state.onFailure(true);

            // when
            int remaining = state.remainingRetries();

            // then
            then(remaining).isZero();
        }

        @Test
        @DisplayName("isLastAttemptFailed should return false initially")
        void isLastAttemptFailed_ShouldReturnFalse_WhenInitiallyCreated() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);

            // when
            boolean failed = state.isLastAttemptFailed();

            // then
            then(failed).isFalse();
        }

        @Test
        @DisplayName("isExhausted should return false initially")
        void isExhausted_ShouldReturnFalse_WhenInitiallyCreated() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);

            // when
            boolean exhausted = state.isExhausted();

            // then
            then(exhausted).isFalse();
        }
    }

    // ========================================================================================
    // nextInterval
    // ========================================================================================

    @Nested
    @DisplayName("nextInterval")
    class NextIntervalTests {

        @Test
        @DisplayName("should return interval from policy")
        void nextInterval_ShouldReturnIntervalFromPolicy_WhenCalled() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(3, ofMillis(200));
            RetryState state = new RetryState(policy);

            // when
            Duration interval = state.nextInterval();

            // then
            then(interval).isEqualTo(ofMillis(200));
        }
    }

    // ========================================================================================
    // elapsed
    // ========================================================================================

    @Nested
    @DisplayName("elapsed")
    class Elapsed {

        @Test
        @DisplayName("should return zero when not started")
        void elapsed_ShouldReturnZero_WhenNotStarted() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);

            // when
            Duration elapsed = state.elapsed();

            // then
            then(elapsed).isZero();
        }

        @Test
        @DisplayName("should return positive duration when started")
        void elapsed_ShouldReturnPositiveDuration_WhenStarted() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);
            state.beginAttempt();

            // when
            Duration elapsed = state.elapsed();

            // then
            then(elapsed).isGreaterThanOrEqualTo(Duration.ZERO);
        }
    }

    // ========================================================================================
    // isDeadlineElapsed
    // ========================================================================================

    @Nested
    @DisplayName("isDeadlineElapsed")
    class IsDeadlineElapsed {

        @Test
        @DisplayName("should return false when deadline is zero")
        void isDeadlineElapsed_ShouldReturnFalse_WhenDeadlineIsZero() {
            // given
            RetryPolicy policy = RetryPolicy.fixed(3, ofMillis(100));
            RetryState state = new RetryState(policy);
            state.beginAttempt();

            // when
            boolean result = state.isDeadlineElapsed();

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("should return false when not started")
        void isDeadlineElapsed_ShouldReturnFalse_WhenNotStarted() {
            // given
            RetryPolicy policy = RetryPolicy.exponential(3, ofMillis(100), 2.0, ofSeconds(5), 0.0, ofSeconds(10));
            RetryState state = new RetryState(policy);

            // when
            boolean result = state.isDeadlineElapsed();

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("should return false when deadline has not elapsed")
        void isDeadlineElapsed_ShouldReturnFalse_WhenDeadlineHasNotElapsed() {
            // given
            RetryPolicy policy = RetryPolicy.exponential(3, ofMillis(100), 2.0, ofSeconds(5), 0.0, ofMinutes(1));
            RetryState state = new RetryState(policy);
            state.beginAttempt();

            // when
            boolean result = state.isDeadlineElapsed();

            // then
            then(result).isFalse();
        }

        @Test
        @DisplayName("should return true when deadline has elapsed")
        void isDeadlineElapsed_ShouldReturnTrue_WhenDeadlineHasElapsed() {
            // given
            RetryPolicy policy = RetryPolicy.exponential(3, ofMillis(100), 2.0, ofSeconds(5), 0.0, ofNanos(1));
            RetryState state = new RetryState(policy);
            state.beginAttempt();

            // when & then
            await()
                    .atMost(ofMillis(100))
                    .pollInterval(ofMillis(1))
                    .untilAsserted(() -> {
                        boolean result = state.isDeadlineElapsed();
                        then(result).isTrue();
                    });
        }
    }

    // ========================================================================================
    // toString
    // ========================================================================================

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("should contain class name and fields")
        void toString_ShouldContainClassNameAndFields_WhenCalled() {
            // given
            RetryState state = new RetryState(DEFAULT_POLICY);
            state.beginAttempt();

            // when
            String result = state.toString();

            // then
            then(result).contains("RetryState");
            then(result).contains("policy=");
            then(result).contains("startedAtNanos=");
            then(result).contains("attemptsExecuted=");
            then(result).contains("retriesGranted=");
            then(result).contains("lastAttemptFailed=");
            then(result).contains("exhausted=");
        }
    }
}
