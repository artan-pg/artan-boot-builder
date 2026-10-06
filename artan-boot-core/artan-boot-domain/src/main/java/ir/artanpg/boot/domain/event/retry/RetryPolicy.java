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
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Immutable retry policy defining the budget and backoff strategy
 * for failed event dispatch attempts.
 *
 * <p>A policy is a pure value object; create one per application
 * (or per listener type) and reuse it. Each dispatch gets its own
 * mutable {@link RetryState} via {@link #newState()}.
 *
 * @param maxRetries      maximum number of retries after the initial failure
 * @param initialInterval delay before the first retry
 * @param multiplier      growth factor applied per attempt ({@code >= 1.0})
 * @param maxInterval     upper cap for any single wait
 * @param jitterFactor    randomization ratio in {@code [0, 1]}
 * @param deadline        overall time bound; {@link Duration#ZERO} means no deadline
 * @author Mohammad Yazdian
 * @see RetryState
 * @since 0.1.0
 */
public record RetryPolicy(int maxRetries,
                          @NonNull Duration initialInterval,
                          double multiplier,
                          @NonNull Duration maxInterval,
                          double jitterFactor,
                          @NonNull Duration deadline) {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_INITIAL_INTERVAL = 100;
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private static final int DEFAULT_MAX_INTERVAL = 5;

    /**
     * A shared policy that never grants a retry.
     */
    private static final RetryPolicy NONE =
            new RetryPolicy(0, Duration.ofMillis(100), 1.0, Duration.ofSeconds(5), 0.0, Duration.ZERO);

    /**
     * Canonical constructor validating every parameter combination.
     *
     * @throws DomainEventException if any numeric parameter is out of range
     */
    public RetryPolicy {
        if (initialInterval == null) throw new DomainEventException("The initialInterval cannot be null");
        if (maxInterval == null) throw new DomainEventException("The maxInterval cannot be null");
        if (deadline == null) throw new DomainEventException("The deadline cannot be null");

        if (maxRetries < 0) throw new DomainEventException("The maxRetries cannot be negative");
        if (initialInterval.isNegative()) throw new DomainEventException("The initialInterval cannot be negative");
        if (maxInterval.isNegative()) throw new DomainEventException("The maxInterval cannot be negative");

        if (deadline.isNegative()) throw new DomainEventException("The deadline cannot be negative");
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier) || multiplier < 1.0) {
            throw new DomainEventException("The multiplier must be a finite number >= 1.0");
        }
        if (Double.isNaN(jitterFactor) || jitterFactor < 0.0 || jitterFactor > 1.0) {
            throw new DomainEventException("The jitterFactor must be within [0.0, 1.0]");
        }
        // A zero interval only makes sense for the NONE policy; otherwise the
        // first wait would be meaningless.
        if (maxRetries > 0 && initialInterval.isZero()) {
            throw new DomainEventException("The initialInterval must be positive when retries are allowed");
        }
    }

    /**
     * Returns the shared policy that disables retrying entirely.
     *
     * @return a policy with a zero retry budget
     */
    public static RetryPolicy none() {
        return NONE;
    }

    /**
     * Creates a policy with a constant delay between attempts and no deadline.
     * The multiplier is pinned to {@code 1.0} so the interval truly stays
     * fixed.
     *
     * @param maxRetries number of retries granted after the initial failure
     * @param interval   the fixed wait between attempts
     * @return a new fixed-delay policy
     */
    public static RetryPolicy fixed(int maxRetries, @NonNull Duration interval) {
        return builder()
                .maxRetries(maxRetries)
                .initialInterval(interval)
                .multiplier(1.0)
                .maxInterval(interval)
                .build();
    }

    /**
     * Creates an exponential-backoff policy.
     *
     * @param maxRetries      number of retries granted after the initial failure
     * @param initialInterval base delay; attempt {@code n} waits
     *                        {@code initialInterval * multiplier^(n-1)} (plus jitter),
     *                        capped at {@code maxInterval}
     * @param multiplier      growth factor, {@code >= 1.0}
     * @param maxInterval     upper cap for any single wait
     * @param jitterFactor    randomization ratio in {@code [0, 1]} applied as
     *                        {@code [base*(1-j), base*(1+j)]}; {@code 0} disables jitter
     * @param deadline        overall time bound across all attempts;
     *                        {@link Duration#ZERO} means "no deadline"
     * @return a new exponential-backoff policy
     */
    public static RetryPolicy exponential(int maxRetries,
                                          @NonNull Duration initialInterval,
                                          double multiplier,
                                          @NonNull Duration maxInterval,
                                          double jitterFactor,
                                          @NonNull Duration deadline) {
        return builder()
                .maxRetries(maxRetries)
                .initialInterval(initialInterval)
                .multiplier(multiplier)
                .maxInterval(maxInterval)
                .jitterFactor(jitterFactor)
                .deadline(deadline)
                .build();
    }

    /**
     * Creates a new builder instance.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Whether this policy ever grants a retry.
     *
     * @return {@code true} if at least one retry is allowed, {@code false} otherwise
     */
    public boolean enabled() {
        return maxRetries > 0;
    }

    /**
     * Computes the raw (pre-jitter) backoff interval to wait before the given
     * attempt number, capped at {@code maxInterval}.
     *
     * @param attemptNumber 1-based retry number (1 = first retry after the initial failure)
     * @return the base interval, {@link Duration#ZERO} when disabled
     */
    @NonNull
    public Duration baseIntervalFor(int attemptNumber) {
        if (!enabled() || attemptNumber < 1) return Duration.ZERO;
        if (multiplier == 1.0) return min(initialInterval, maxInterval);
        // Compute in double to avoid overflow, then clamp.
        double factor = Math.pow(multiplier, attemptNumber - 1.0);
        double millis = initialInterval.toMillis() * factor;

        return (Double.isInfinite(millis) || millis > maxInterval.toMillis()) ?
                maxInterval : min(Duration.ofMillis(Math.round(millis)), maxInterval);
    }

    /**
     * Computes the actual wait before the given attempt, applying jitter when
     * configured. Jitter is drawn from {@link ThreadLocalRandom}, so results
     * vary per call by design.
     *
     * @param attemptNumber 1-based retry number
     * @return the interval to sleep/wait before that attempt
     */
    @NonNull
    public Duration nextInterval(@SuppressWarnings("unused") int attemptNumber) {
        Duration base = baseIntervalFor(attemptNumber);

        if (base.isZero() || jitterFactor == 0.0) return base;

        long span = base.toMillis();
        long lower = Math.round(span * (1.0 - jitterFactor));
        long upper = Math.round(span * (1.0 + jitterFactor));
        long jittered = ThreadLocalRandom.current().nextLong(Math.max(0, lower), Math.max(lower, upper) + 1);

        return min(Duration.ofMillis(jittered), maxInterval);
    }

    /**
     * Starts a fresh, mutable execution of this policy.
     *
     * @return a new {@link RetryState} tracking attempts against this budget
     */
    public RetryState newState() {
        return new RetryState(this);
    }

    /**
     * Returns the smaller of two durations.
     *
     * @param a first duration
     * @param b second duration
     * @return the smaller duration
     */
    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    /**
     * Builder for constructing immutable {@link RetryPolicy} instances.
     *
     * @author Mohammad Yazdian
     * @since 0.1.0
     */
    public static final class Builder {

        private int maxRetries = DEFAULT_MAX_RETRIES;
        private Duration initialInterval = Duration.ofMillis(DEFAULT_INITIAL_INTERVAL);
        private double multiplier = DEFAULT_MULTIPLIER;
        private Duration maxInterval = Duration.ofSeconds(DEFAULT_MAX_INTERVAL);
        private double jitterFactor;
        private Duration deadline = Duration.ZERO;

        /**
         * Constructs an empty builder.
         *
         * <p>The constructor is private. use {@link RetryPolicy#builder()} to
         * obtain a builder instance.
         */
        private Builder() {
        }

        /**
         * Sets the maximum number of retries.
         *
         * @param maxRetries the retry budget
         * @return this builder instance for method chaining
         */
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the initial backoff interval.
         *
         * @param initialInterval the initial delay
         * @return this builder instance for method chaining
         */
        public Builder initialInterval(@NonNull Duration initialInterval) {
            this.initialInterval = initialInterval;
            return this;
        }

        /**
         * Sets the backoff multiplier.
         *
         * @param multiplier the growth factor
         * @return this builder instance for method chaining
         */
        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        /**
         * Sets the maximum interval cap.
         *
         * @param maxInterval the upper bound
         * @return this builder instance for method chaining
         */
        public Builder maxInterval(@NonNull Duration maxInterval) {
            this.maxInterval = maxInterval;
            return this;
        }

        /**
         * Sets the jitter factor.
         *
         * @param jitterFactor ratio in {@code [0, 1]}
         * @return this builder instance for method chaining
         */
        public Builder jitterFactor(double jitterFactor) {
            this.jitterFactor = jitterFactor;
            return this;
        }

        /**
         * Sets the overall deadline.
         *
         * @param deadline the time bound
         * @return this builder instance for method chaining
         */
        public Builder deadline(@NonNull Duration deadline) {
            this.deadline = deadline;
            return this;
        }

        /**
         * Builds and returns a new {@link RetryPolicy} instance.
         *
         * @return a new {@link RetryPolicy} instance
         **/
        public RetryPolicy build() {
            return new RetryPolicy(maxRetries, initialInterval, multiplier, maxInterval, jitterFactor, deadline);
        }
    }
}
