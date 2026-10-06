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
import java.util.StringJoiner;

/**
 * Mutable execution state for a single retry sequence.
 *
 * <p>Created via {@link RetryPolicy#newState()} and bound to the policy that
 * produced it. Tracks attempts, granted retries, and deadline exhaustion. All
 * mutating methods are synchronized so a single state can safely be shared
 * across threads if needed.
 *
 * @author Mohammad Yazdian
 * @see RetryPolicy
 * @since 0.1.0
 */
public final class RetryState {

    /**
     * The immutable budget this state executes against.
     */
    private final RetryPolicy policy;

    /**
     * Monotonic start marker (nanoTime) of this retry sequence;
     * used for deadline evaluation. Zero means not started yet.
     */
    private long startedAtNanos;

    /**
     * Number of attempts executed so far (1 = initial attempt).
     */
    private int attemptsExecuted;

    /**
     * Number of retries granted so far (attemptsExecuted - 1,
     * kept separately for clarity).
     */
    private int retriesGranted;

    /**
     * Whether the most recent completed attempt was a failure.
     */
    private boolean lastAttemptFailed;

    /**
     * Whether the retry budget or deadline has been permanently
     * exhausted.
     */
    private boolean exhausted;

    /**
     * Creates a new state bound to the given policy.
     *
     * @param policy the retry policy to execute against
     * @throws DomainEventException if policy is {@code null}
     */
    public RetryState(@NonNull RetryPolicy policy) {
        if (policy == null) throw new DomainEventException("The policy cannot be null");
        this.policy = policy;
    }

    /**
     * Returns the policy this state is bound to.
     *
     * @return the retry policy
     */
    @NonNull
    public RetryPolicy getPolicy() {
        return policy;
    }

    /**
     * Marks the beginning of a new attempt.
     *
     * <p>On the first call, the start timestamp is captured for
     * deadline evaluation.
     */
    public synchronized void beginAttempt() {
        if (startedAtNanos == 0) startedAtNanos = System.nanoTime();

        attemptsExecuted++;
        lastAttemptFailed = false;
    }

    /**
     * Reports that the current attempt failed.
     *
     * <p>If {@code retryRequested} is {@code true} and the budget
     * is not exhausted, a retry is granted and {@code true} is
     * returned.
     *
     * @param retryRequested whether an interceptor asked for retry
     * @return {@code true} if a retry was granted, {@code false} otherwise
     */
    public synchronized boolean onFailure(boolean retryRequested) {
        lastAttemptFailed = true;
        if (!retryRequested) {
            exhausted = false;
            return false;
        }
        if (policy.maxRetries() <= 0 || retriesGranted >= policy.maxRetries() || isDeadlineElapsed()) {
            exhausted = true;
            return false;
        }
        retriesGranted++;
        return true;
    }

    /**
     * Reports that the current attempt failed, implicitly
     * requesting a retry.
     *
     * @return {@code true} if a retry was granted, {@code false} otherwise
     */
    public synchronized boolean onFailure() {
        return onFailure(true);
    }

    /**
     * Clears the per-attempt failure flag.
     *
     * <p>Useful for dispatchers that reset state between attempts.
     */
    public synchronized void clearRetryRequest() {
        lastAttemptFailed = false;
    }

    /**
     * Returns the number of attempts executed so far.
     *
     * @return the attempt count
     */
    public synchronized int getAttemptsExecuted() {
        return attemptsExecuted;
    }

    /**
     * Returns the number of retries granted so far.
     *
     * @return the retry count
     */
    public synchronized int getRetriesGranted() {
        return retriesGranted;
    }

    /**
     * Returns how many retries remain in the budget.
     *
     * @return the remaining retry count, at least zero
     */
    public synchronized int remainingRetries() {
        return Math.max(0, policy.maxRetries() - retriesGranted);
    }

    /**
     * Whether the most recent attempt failed.
     *
     * @return {@code true} if the last attempt failed, {@code false} otherwise
     */
    public synchronized boolean isLastAttemptFailed() {
        return lastAttemptFailed;
    }

    /**
     * Whether the retry budget or deadline has been exhausted.
     *
     * @return {@code true} if no more retries are possible, {@code false} otherwise
     */
    public synchronized boolean isExhausted() {
        return exhausted;
    }

    /**
     * Returns the next interval to wait before the next attempt.
     *
     * @return the wait duration
     */
    @NonNull
    public synchronized Duration nextInterval() {
        return policy.nextInterval(Math.max(1, retriesGranted));
    }

    /**
     * Returns the elapsed time since the first attempt began.
     *
     * @return the elapsed duration, or {@link Duration#ZERO} if
     *         not started yet
     */
    @NonNull
    public synchronized Duration elapsed() {
        if (startedAtNanos == 0) return Duration.ZERO;
        return Duration.ofNanos(System.nanoTime() - startedAtNanos);
    }

    /**
     * Whether the overall deadline has been exceeded.
     *
     * @return {@code true} if the deadline has elapsed, {@code false} otherwise
     */
    public synchronized boolean isDeadlineElapsed() {
        if (policy.deadline().isZero()) return false;
        if (startedAtNanos == 0) return false;
        return elapsed().compareTo(policy.deadline()) >= 0;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RetryState.class.getSimpleName() + "[", "]")
                .add("policy=" + policy)
                .add("startedAtNanos=" + startedAtNanos)
                .add("attemptsExecuted=" + attemptsExecuted)
                .add("retriesGranted=" + retriesGranted)
                .add("lastAttemptFailed=" + lastAttemptFailed)
                .add("exhausted=" + exhausted)
                .toString();
    }
}
