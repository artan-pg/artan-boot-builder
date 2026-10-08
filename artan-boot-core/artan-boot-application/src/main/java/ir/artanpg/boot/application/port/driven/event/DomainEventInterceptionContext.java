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

package ir.artanpg.boot.application.port.driven.event;

import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.retry.RetryPolicy;
import ir.artanpg.boot.domain.event.retry.RetryState;
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable context shared by all {@link DomainEventInterceptor}s participating
 * in a single interception phase (publishing or listening).
 *
 * <p>The context solves three problems that plain method parameters cannot:
 * <ul>
 *   <li><strong>Data exchange:</strong> interceptors can stash values
 *       (e.g. a start timestamp set by a profiling interceptor is read back
 *       by the same interceptor in the completion callback).</li>
 *   <li><strong>Scoped veto:</strong> a security/validation interceptor can
 *       call {@link #veto(String)} to abort only the <em>current unit of
 *       work</em>. A veto raised during the publishing phase suppresses the
 *       whole publication; a veto raised inside a per-listener handling chain
 *       skips that single listener and nothing else — when a context is shared
 *       among several listeners, the dispatcher must call {@link #resetVeto()}
 *       before each listener's chain so one listener's veto never suppresses
 *       the others.</li>
 *   <li><strong>Retry coordination:</strong> inside an error callback an
 *       interceptor calls {@link #requestRetry()}; the dispatcher evaluates
 *       the request against the context's {@link RetryPolicy} through the
 *       embedded {@link RetryState} machine, which enforces the attempt
 *       budget, the backoff schedule and the overall deadline, and resets the
 *       pending request at the start of every attempt so a stale flag can
 *       never trigger an extra retry.</li>
 * </ul>
 *
 * <p>Beyond the free-form attribute map, the context carries first-class
 * fields describing the ongoing dispatch: the event itself, the target
 * listener id (only in the per-listener handling phases), the terminal
 * throwable (in error callbacks) and the retry state machine.
 *
 * <p>Instances are thread-safe: attributes live in a
 * {@link ConcurrentHashMap}, flags are atomic, and the retry transitions are
 * synchronized inside {@link RetryState}.
 *
 * @param <E> the specific {@code DomainEvent} subclass to listen to
 * @author Mohammad Yazdian
 * @see DomainEventInterceptor
 * @see RetryPolicy
 * @see RetryState
 * @since 0.1.0
 */
public final class DomainEventInterceptionContext<E extends DomainEvent<?, ?>> {

    /**
     * Attribute key conventionally used by timing interceptors.
     */
    public static final String START_TIME_NANOS = "startTimeNanos";

    /**
     * The domain event being intercepted.
     */
    private final E event;

    /**
     * The id of the target listener for publish-phase contexts.
     */
    private final String listenerId;

    /**
     * The terminal throwable, if any error occurred.
     */
    private final AtomicReference<Throwable> throwable = new AtomicReference<>();

    /**
     * Flag indicating whether a veto has been raised for the CURRENT unit of
     * work (the publishing phase, or one listener's handling chain). The flag is
     * scoped: it must be cleared by {@link #resetVeto()} between units of work
     * when the context is reused across listeners, so one listener's veto can
     * never leak into and suppress another listener's delivery.
     */
    private final AtomicBoolean vetoed = new AtomicBoolean();

    /**
     * The reason for the current veto, if any. Cleared together with the
     * vetoed flag by {@link #resetVeto()}.
     */
    private final AtomicReference<String> vetoReason = new AtomicReference<>();

    /**
     * Whether an interceptor asked for a retry during the CURRENT attempt.
     * Reset automatically by {@link #beginAttempt()} so a leftover request
     * from a previous attempt can never leak into the next evaluation.
     */
    private final AtomicBoolean retryRequested = new AtomicBoolean();

    /**
     * The retry execution state machine bound to this round's policy.
     */
    private final RetryState retryState;

    /**
     * Arbitrary attributes shared among interceptors.
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Creates a context for a per-listener handling phase using the default
     * retry policy ({@link RetryPolicy#none()} — no retries granted).
     *
     * @param event      the event being handled
     * @param listenerId the id of the target listener
     * @throws DomainEventException if event {@code null}
     * @throws DomainEventException if listenerId {@code null} or {@code blank}
     */
    public DomainEventInterceptionContext(@NonNull E event, @NonNull String listenerId) {
        this(event, listenerId, RetryPolicy.none());
    }

    /**
     * Creates a context for a per-listener handling phase with an explicit
     * retry budget.
     *
     * @param event      the event being handled
     * @param listenerId the id of the target listener
     * @param policy     the retry policy honored when interceptors request a retry
     * @throws DomainEventException if event or policy {@code null}
     * @throws DomainEventException if listenerId {@code null} or {@code blank}
     */
    public DomainEventInterceptionContext(@NonNull E event,
                                          @NonNull String listenerId,
                                          @NonNull RetryPolicy policy) {
        if (event == null) throw new DomainEventException("The event cannot be null");
        if (listenerId == null || listenerId.isBlank()) {
            throw new DomainEventException("The listenerId cannot be null or blank");
        }
        if (policy == null) throw new DomainEventException("The retryPolicy cannot be null");

        this.event = event;
        this.listenerId = listenerId;
        this.retryState = policy.newState();
    }

    /**
     * Returns the domain event this interception round concerns.
     *
     * @return the intercepted event, never {@code null}
     */
    @NonNull
    public E getEvent() {
        return event;
    }

    /**
     * Returns the id of the listener whose processing is intercepted.
     *
     * @return the listener id
     */
    @NonNull
    public String getListenerId() {
        return listenerId;
    }

    /**
     * Returns the exception associated with this round, when any.
     *
     * @return the terminal throwable, or {@code null} if no error occurred
     */
    @Nullable
    public Throwable getThrowable() {
        return throwable.get();
    }

    /**
     * Sets the terminal throwable for this round.
     *
     * @param throwable the exception to set
     */
    public void setThrowable(@Nullable Throwable throwable) {
        this.throwable.set(throwable);
    }

    /**
     * Veto the <em>current unit of work</em> without a reason. The dispatcher
     * must honor the veto by skipping the remaining work of that unit only:
     * <ul>
     *   <li>raised in {@code beforePublish} - the whole publication is
     *       suppressed;</li>
     *   <li>raised in {@code beforeHandle}/{@code onError} - only the listener
     *       whose handling chain is running is skipped; other listeners still
     *       receive the event, provided the dispatcher calls
     *       {@link #resetVeto()} before each listener's chain.</li>
     * </ul>
     */
    public void veto() {
        veto(null);
    }

    /**
     * Veto the current phase with an explanatory reason.
     *
     * @param reason human-readable explanation
     */
    public void veto(@Nullable String reason) {
        this.vetoed.set(true);
        this.vetoReason.set(reason);
    }

    /**
     * Reports whether a veto has been raised by any interceptor.
     *
     * @return {@code true} if {@link #veto()} was called, {@code false} otherwise
     */
    public boolean isVetoed() {
        return vetoed.get();
    }

    /**
     * Returns the reason passed to {@link #veto(String)}.
     *
     * @return the veto reason, or {@code null}
     */
    @Nullable
    public String getVetoReason() {
        return vetoReason.get();
    }

    /**
     * Clears any pending veto (flag and reason), delimiting the veto to the
     * unit of work it was raised in. Dispatchers must call this between
     * per-listener handling chains when a context instance is shared across
     * listeners; otherwise a veto raised for one listener would leak into the
     * next one's chain and suppress its delivery as well.
     *
     * @return {@code true} if a veto was present and has been cleared, {@code false} if no veto was pending
     */
    public boolean resetVeto() {
        // Clear the reason first, then the flag: readers that check isVetoed()
        // can never observe the flag set with a stale reason from a previous
        // unit of work.
        boolean wasVetoed = this.vetoed.getAndSet(false);
        this.vetoReason.set(null);
        return wasVetoed;
    }

    /**
     * Requests that the failed handling attempt to be retried by the dispatcher.
     *
     * <p>Only meaningful in error callbacks. The request applies to the
     * <em>current</em> attempt only: it is cleared again by
     * {@link #beginAttempt()} before the next try, so interceptors must ask
     * again on every failure they want retried. Whether the request is
     * actually granted depends on the context's {@link RetryPolicy} — the
     * attempt budget, the backoff schedule and the overall deadline are all
     * evaluated by {@link #evaluateRetry()}.
     */
    public void requestRetry() {
        this.retryRequested.set(true);
    }

    /**
     * Reports whether an interceptor asked for a retry during the current
     * attempt.
     *
     * @return {@code true} if {@link #requestRetry()} was called since the
     *         last {@link #beginAttempt()}, {@code false} otherwise
     */
    public boolean isRetryRequested() {
        return retryRequested.get();
    }

    /**
     * Clears a pending retry request without starting a new attempt. Dispatchers
     * normally rely on the automatic reset inside {@link #beginAttempt()};
     * this hook exists for explicit cleanup paths (e.g. after a veto).
     */
    public void clearRetryRequest() {
        this.retryRequested.set(false);
    }

    /**
     * Starts a new attempt: resets the per-attempt retry-request flag, advances
     * the attempt counter and starts the deadline clock on the first call.
     *
     * @return the 1-based number of this attempt
     */
    public int beginAttempt() {
        this.retryRequested.set(false);
        this.retryState.beginAttempt();
        return this.retryState.getAttemptsExecuted();
    }

    /**
     * Evaluates the outcome of the current attempt against the retry policy.
     * Call this from the dispatcher after running the {@code onError}
     * interceptor callbacks.
     *
     * @return {@code true} if a retry was requested AND the policy grants one
     *         (budget left, deadline not elapsed) — the dispatcher should then
     *         wait {@link #nextRetryInterval()} and call
     *         {@link #beginAttempt()} again; {@code false} if the failure is
     *         terminal, in which case {@code onHandlingFailure} should run
     * @see #isRetryExhausted()
     */
    public boolean evaluateRetry() {
        return this.retryState.onFailure(this.retryRequested.get());
    }

    /**
     * The backoff interval to wait before the next retry attempt, computed by
     * the policy (fixed/exponential, capped, jittered).
     *
     * @return the wait interval; {@link Duration#ZERO} when disabled
     */
    @NonNull
    public Duration nextRetryInterval() {
        return this.retryState.nextInterval();
    }

    /**
     * Returns how many retries have already been attempted in this round.
     *
     * @return the retry count, zero on the first attempt
     */
    public int getRetryCount() {
        return this.retryState.getRetriesGranted();
    }

    /**
     * Total attempts executed so far (initial attempt + retries).
     *
     * @return the attempt count, zero before the first {@link #beginAttempt()}
     */
    public int getAttemptsExecuted() {
        return this.retryState.getAttemptsExecuted();
    }

    /**
     * Remaining retries under the configured budget, ignoring the deadline.
     *
     * @return remaining retries, never negative
     */
    public int remainingRetries() {
        return this.retryState.remainingRetries();
    }

    /**
     * Whether the retry budget or the overall deadline has been exhausted on a
     * requested retry — i.e. the failure is terminal because the framework
     * gave up, as opposed to nobody ever asking for a retry.
     *
     * @return {@code true} if a retry was requested but could not be granted
     */
    public boolean isRetryExhausted() {
        return this.retryState.isExhausted();
    }

    /**
     * Whether the policy's overall deadline has elapsed for this round.
     *
     * @return {@code true} if the deadline is set and has passed
     */
    public boolean isDeadlineElapsed() {
        return this.retryState.isDeadlineElapsed();
    }

    /**
     * The retry policy in effect for this round.
     *
     * @return the immutable policy, never {@code null}
     */
    @NonNull
    public RetryPolicy getRetryPolicy() {
        return this.retryState.getPolicy();
    }

    /**
     * Stores an arbitrary attribute shared among interceptors.
     *
     * @param key   attribute key
     * @param value attribute value
     * @return the previous value associated with {@code key}, or {@code null}
     */
    @Nullable
    public Object setAttribute(@NonNull String key, @Nullable Object value) {
        if (value == null) return attributes.remove(key);
        return attributes.put(key, value);
    }

    /**
     * Reads a previously stored attribute.
     *
     * @param key attribute key
     * @return the stored value, or {@code null} if absent
     */
    @Nullable
    public Object getAttribute(@NonNull String key) {
        return attributes.get(key);
    }

    /**
     * Type-safe convenience accessor for an attribute.
     *
     * @param key  attribute key
     * @param type expected value type
     * @param <V>  the attribute value type
     * @return the stored value cast to {@code type}, or {@code null} if absent or of a different type
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <V> V getAttribute(@NonNull String key, @NonNull Class<V> type) {
        Object value = attributes.get(key);
        return type.isInstance(value) ? (V) value : null;
    }

    /**
     * Removes a stored attribute.
     *
     * @param key attribute key
     * @return the removed value, or {@code null} if absent
     */
    @Nullable
    public Object removeAttribute(@NonNull String key) {
        return attributes.remove(key);
    }

    /**
     * Returns an unmodifiable snapshot view of all attributes.
     *
     * @return live-backed unmodifiable map of attributes
     */
    @NonNull
    public Map<String, Object> getAttributes() {
        return Map.copyOf(attributes);
    }
}
