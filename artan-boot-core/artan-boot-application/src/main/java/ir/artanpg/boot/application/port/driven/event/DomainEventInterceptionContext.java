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
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
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
 * fields describing the ongoing dispatch: the event itself, the explicit
 * lifecycle {@link InterceptionPhase}, the target listener id (only in the
 * per-listener handling phases), the terminal throwable (in error callbacks)
 * and the retry state machine. Correlation/causation/tenant are surfaced as
 * typed accessors over the event's immutable metadata, and an optional
 * parent-context link plus a round timeout make nested dispatch chains (an
 * event published while handling another event) and end-to-end budgets
 * observable.
 *
 * <p>Instances are thread-safe: attributes live in a lazily allocated
 * {@link ConcurrentHashMap} published through an {@link AtomicReference}
 * (created on first write only, races resolved with CAS), flags are atomic,
 * and the retry transitions are synchronized inside {@link RetryState}.
 *
 * @param <E> the specific {@code DomainEvent} subclass to listen to
 * @author Mohammad Yazdian
 * @see DomainEventInterceptor
 * @see InterceptionPhase
 * @see RetryPolicy
 * @see RetryState
 * @since 0.1.0
 */
public final class DomainEventInterceptionContext<E extends DomainEvent<?, ?>> implements NestedInterceptionContext {

    /**
     * Attribute key conventionally used by timing interceptors. Namespaced
     * with the framework prefix to avoid collisions with application keys.
     */
    public static final String START_TIME_NANOS = "artan.startTimeNanos";

    /**
     * Attribute key under which the idempotency/deduplication key of the
     * current delivery round is stored when explicitly set via
     * {@link #setIdempotencyKey(String)}.
     */
    public static final String IDEMPOTENCY_KEY = "artan.idempotencyKey";

    private static final String EVENT_NULL_EXCEPTION = "The event cannot be null";
    private static final String PHASE_NULL_EXCEPTION = "The phase cannot be null";
    private static final String RETRY_POLICY_NULL_EXCEPTION = "The retryPolicy cannot be null";

    /**
     * The domain event being intercepted.
     */
    private final E event;

    /**
     * The lifecycle phase this context belongs to. Explicit rather than
     * inferred from {@code listenerId == null}, so hooks can validate they
     * received the right kind of context.
     */
    private final InterceptionPhase phase;

    /**
     * The id of the target listener for per-listener handling-phase contexts;
     * {@code null} in publish-phase contexts, where no single listener is
     * targeted yet.
     */
    private final String listenerId;

    /**
     * The context of the enclosing dispatch round this round is nested in, if
     * any. Set when handling an event that was published while another event
     * was still being dispatched; enables tracing the causal chain across
     * rounds without abusing the attribute map.
     */
    @Nullable
    private final NestedInterceptionContext parent;

    /**
     * Overall wall-clock budget for this interception round, measured from
     * construction with a monotonic clock. {@code null} means unbounded;
     * independent of (and complementary to) the retry policy's deadline,
     * which only bounds the retry cycle.
     */
    @Nullable
    private final Duration roundTimeout;

    /**
     * Monotonic creation instant backing {@link #roundElapsed()}.
     */
    private final long createdAtNanos;

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
    private final AtomicReference<Map<String, Object>> attributesRef = new AtomicReference<>();

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
        this(event, InterceptionPhase.HANDLE, requireListenerId(listenerId), policy, null, null);
    }

    /**
     * Full canonical constructor shared by the public constructors
     * and the static factories/builder.
     *
     * <p>Validates the phase/listener-id invariant: handling-phase
     * contexts MUST carry a non-blank listener id, publish-phase
     * contexts MUST NOT carry one.
     *
     * @param event        the intercepted event
     * @param phase        the lifecycle phase
     * @param listenerId   the target listener id, or {@code null} for publish-phase contexts
     * @param policy       the retry policy for this round
     * @param parent       the enclosing dispatch context, or {@code null} for top-level rounds
     * @param roundTimeout the wall-clock budget, or {@code null} for unbounded rounds
     * @throws DomainEventException if event, phase, or policy is {@code null},
     *                              if the phase/listener-id invariant is violated,
     *                              or if roundTimeout is negative
     */
    private DomainEventInterceptionContext(E event,
                                           InterceptionPhase phase,
                                           @Nullable String listenerId,
                                           RetryPolicy policy,
                                           @Nullable NestedInterceptionContext parent,
                                           @Nullable Duration roundTimeout) {
        if (event == null) throw new DomainEventException(EVENT_NULL_EXCEPTION);
        if (phase == null) throw new DomainEventException(PHASE_NULL_EXCEPTION);
        if (policy == null) throw new DomainEventException(RETRY_POLICY_NULL_EXCEPTION);
        if (phase.isHandlingPhase() && (listenerId == null || listenerId.isBlank())) {
            throw new DomainEventException("A handling-phase context requires a non-blank listenerId");
        }
        if (phase.isPublishPhase() && listenerId != null) {
            throw new DomainEventException("A publish-phase context must not carry a listenerId");
        }
        if (roundTimeout != null && roundTimeout.isNegative()) {
            throw new DomainEventException("The roundTimeout cannot be negative");
        }

        this.event = event;
        this.phase = phase;
        this.listenerId = listenerId;
        this.retryState = policy.newState();
        this.parent = parent;
        this.roundTimeout = roundTimeout;
        this.createdAtNanos = System.nanoTime();
    }

    /**
     * Factory for a <strong>publish-phase</strong> context: wraps the event
     * before it is routed to any listener, hence carries no listener id
     * ({@link #getListenerId()} returns {@code null}) and {@link #getPhase()}
     * is {@link InterceptionPhase#PUBLISH}. A veto raised on such a context
     * suppresses the entire publication.
     *
     * @param event the event about to be published
     * @param <E>   the concrete event type
     * @return a fresh publish-phase context with no retry budget and no timeout
     */
    public static <E extends DomainEvent<?, ?>> DomainEventInterceptionContext<E> forPublish(@NonNull E event) {
        return builder(event).phase(InterceptionPhase.PUBLISH).build();
    }

    /**
     * Factory for a <strong>publish-phase</strong> context with an explicit
     * retry budget (useful when the publisher itself re-attempts publication).
     *
     * @param event  the event about to be published
     * @param policy the retry policy honored when interceptors request a retry
     * @param <E>    the concrete event type
     * @return a fresh publish-phase context bound to {@code policy}
     */
    public static <E extends DomainEvent<?, ?>> DomainEventInterceptionContext<E> forPublish(
            @NonNull E event, @NonNull RetryPolicy policy) {
        return builder(event).phase(InterceptionPhase.PUBLISH).retryPolicy(policy).build();
    }

    /**
     * Factory for a <strong>per-listener handling-phase</strong> context with
     * an explicit retry budget. Equivalent to the public constructor but reads
     * better at dispatcher call sites and pairs with {@link #forPublish}.
     *
     * @param event      the event being handled
     * @param listenerId the id of the target listener
     * @param policy     the retry policy honored when interceptors request a retry
     * @param <E>        the concrete event type
     * @return a fresh handling-phase context bound to {@code policy}
     */
    public static <E extends DomainEvent<?, ?>> DomainEventInterceptionContext<E> forHandling(
            @NonNull E event, @NonNull String listenerId, @NonNull RetryPolicy policy) {
        return builder(event).phase(InterceptionPhase.HANDLE)
                .listenerId(listenerId).retryPolicy(policy).build();
    }

    /**
     * Returns a fluent builder for contexts of the given event type. Seeded
     * with the safest defaults: {@link InterceptionPhase#PUBLISH}, no retry
     * budget, no parent, no timeout.
     *
     * @param event the intercepted event, must not be null
     * @param <E>   the concrete event type
     * @return a fresh builder
     */
    public static <E extends DomainEvent<?, ?>> @NonNull Builder<E> builder(@NonNull E event) {
        return new Builder<>(event);
    }

    /**
     * Derives a builder for a <strong>nested child round</strong>: a handler
     * synchronously published {@code childEvent} while this round was running.
     * The child inherits the parent link (and thus the causal chain visible
     * through {@link #getDepth()}/{@link #getRootRound()}) while starting
     * fresh veto/retry state. The caller must still supply the child's
     * listener id — the builder defaults to a handling-phase context.
     *
     * @param childEvent the event of the nested round
     * @param <F>        the child context's event type
     * @return a pre-seeded builder with {@code parent(this)} set
     */
    public <F extends DomainEvent<?, ?>> @NonNull Builder<F> deriveChild(F childEvent) {
        return new Builder<>(childEvent)
                .phase(InterceptionPhase.HANDLE)
                .parent(this);
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
     * Returns the lifecycle phase of this context. Prefer branching on this
     * over {@code getListenerId() == null} — the phase is the authoritative,
     * constructor-validated discriminator between publish-level and
     * handle-level rounds.
     *
     * @return the phase, never {@code null}
     */
    @NonNull
    public InterceptionPhase getPhase() {
        return phase;
    }

    /**
     * Convenience predicate: {@code getPhase().isPublishPhase()}.
     *
     * @return {@code true} if this context belongs to the publishing side, {@code false} otherwise
     */
    public boolean isPublishPhase() {
        return phase.isPublishPhase();
    }

    /**
     * Convenience predicate: {@code getPhase().isHandlingPhase()}.
     *
     * @return {@code true} if this context belongs to a per-listener round, {@code false} otherwise
     */
    public boolean isHandlingPhase() {
        return phase.isHandlingPhase();
    }

    /**
     * Returns the id of the listener whose processing is intercepted, or
     * {@code null} in publish-phase contexts (guaranteed consistent with
     * {@link #getPhase()} by the constructor validation).
     *
     * @return the listener id, or {@code null}
     */
    @Nullable
    public String getListenerId() {
        return listenerId;
    }

    @Override
    public NestedInterceptionContext getParent() {
        return parent;
    }

    @Override
    public int getDepth() {
        int depth = 0;
        NestedInterceptionContext p = this.parent;
        while (p != null) {
            depth++;
            p = p.getParent();
        }
        return depth;
    }

    @Override
    public NestedInterceptionContext getRootRound() {
        NestedInterceptionContext current = this;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    /**
     * Returns the correlation id from the event metadata.
     *
     * @return the correlation id, or {@code null}
     */
    @Nullable
    public String getCorrelationId() {
        return event.getMetadata().getCorrelationId();
    }

    /**
     * Returns the causation id from the event metadata.
     *
     * @return the causation id, or {@code null}
     */
    @Nullable
    public String getCausationId() {
        return event.getMetadata().getCausationId();
    }

    /**
     * Returns the tenant from the event metadata.
     *
     * @return the tenant, or {@code null}
     */
    @Nullable
    public String getTenant() {
        return event.getMetadata().getTenant();
    }

    /**
     * Sets the idempotency / deduplication key for this delivery round. The
     * dispatcher should consult {@link #getIdempotencyKey()} before invoking
     * handlers and skip already-processed deliveries (at-least-once transports
     * rely on this to become effectively once). Falls back to the event's
     * {@code eventId} — the natural per-fact dedup key — when never set.
     *
     * @param idempotencyKey the dedup key; {@code null} or {@code blank} clears the override and
     *                       restores the {@code eventId} fallback
     */
    public void setIdempotencyKey(@Nullable String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            attributesOrEmpty().remove(IDEMPOTENCY_KEY);
            return;
        }
        attributesOrCreate().put(IDEMPOTENCY_KEY, idempotencyKey);
    }

    /**
     * The effective idempotency key of this round: the explicitly assigned key
     * if present, otherwise the event's unique {@code eventId}.
     *
     * @return a non-blank dedup key
     */
    @NonNull
    public String getIdempotencyKey() {
        Object override = attributesOrEmpty().get(IDEMPOTENCY_KEY);
        if (override instanceof String idempotencyKey && !idempotencyKey.isBlank()) return idempotencyKey;
        return event.getEventId();
    }

    /**
     * The configured wall-clock budget for this round, if any.
     *
     * @return the timeout, or {@code null} when the round is unbounded
     */
    @Nullable
    public Duration getRoundTimeout() {
        return roundTimeout;
    }

    /**
     * Monotonic time elapsed since this context was created.
     *
     * @return elapsed duration, always non-negative
     */
    @NonNull
    public Duration roundElapsed() {
        return Duration.ofNanos(Math.max(0, System.nanoTime() - createdAtNanos));
    }

    /**
     * Cooperative timeout check: {@code true} once the round budget has been
     * consumed. Dispatchers must poll this at safe points (before each
     * listener, before each retry attempt) and stop gracefully when it flips.
     *
     * @return {@code true} if a timeout is configured and has elapsed, {@code false} otherwise
     */
    public boolean isRoundTimedOut() {
        if (roundTimeout == null || roundTimeout.isZero()) return false;
        return System.nanoTime() - createdAtNanos >= roundTimeout.toNanos();
    }

    /**
     * Remaining time in the round budget, useful for deriving per-step
     * timeouts (e.g. clamping a listener's own deadline to what's left).
     *
     * @return the remaining duration, or {@link Duration#ZERO} when expired;
     *         also {@code ZERO} when no budget is configured — check
     *         {@link #getRoundTimeout()} to distinguish "no budget"
     */
    @NonNull
    public Duration roundRemaining() {
        if (roundTimeout == null) return Duration.ZERO;
        long remaining = roundTimeout.toNanos() - (System.nanoTime() - createdAtNanos);
        return Duration.ofNanos(Math.max(0, remaining));
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
     * @throws DomainEventException if key is {@code null} or {@code blank}
     */
    public void setAttribute(@NonNull String key, @Nullable Object value) {
        if (key == null || key.isBlank()) throw new DomainEventException("The key cannot be null or blank");
        if (value == null) {
            removeAttribute(key);
        } else {
            attributesOrCreate().put(key, value);
        }
    }

    /**
     * Reads a previously stored attribute.
     *
     * @param key attribute key
     * @return the stored value, or {@code null} if absent
     */
    @Nullable
    public Object getAttribute(String key) {
        return attributesOrEmpty().get(key);
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
        Object value = getAttribute(key);
        return type.isInstance(value) ? (V) value : null;
    }

    /**
     * Removes a stored attribute.
     *
     * @param key attribute key
     */
    public void removeAttribute(String key) {
        Map<String, Object> map = attributesOrEmpty();
        if (!map.isEmpty()) map.remove(key);
    }

    /**
     * Returns a live, unmodifiable view of the attributes.
     *
     * <p>Changes made via setAttribute/removeAttribute will be reflected in
     * this view immediately.
     *
     * @return a live unmodifiable view of the current state
     */
    @NonNull
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributesOrEmpty());
    }

    /**
     * Returns an immutable snapshot of the current attributes.
     *
     * <p>Use this if you need a detached copy that won't change even if
     * attributes are modified later.
     *
     * @return a detached, immutable copy
     */
    @NonNull
    public Map<String, Object> attributesSnapshot() {
        return Map.copyOf(attributesOrEmpty());
    }

    private static String requireListenerId(String listenerId) {
        if (listenerId == null || listenerId.isBlank()) {
            throw new DomainEventException("The listenerId cannot be null or blank");
        }
        return listenerId;
    }

    /**
     * Returns the attributes map, or an empty map if not initialized.
     *
     * <p>This method guarantees a non-null return value for safe iteration
     * and access without requiring null checks.
     *
     * @return the attributes map, never {@code null}
     */
    private Map<String, Object> attributesOrEmpty() {
        Map<String, Object> map = attributesOrCreate();
        return (map != null) ? map : Collections.emptyMap();
    }

    /**
     * Lazily initializes and returns the attributes map.
     *
     * <p>Uses a compare-and-set (CAS) operation to ensure thread-safe
     * initialization without synchronization overhead.
     *
     * @return the current attributes map
     */
    private Map<String, Object> attributesOrCreate() {
        Map<String, Object> map = attributesRef.get();
        if (map == null) {
            // Create a new ConcurrentHashMap candidate
            map = new ConcurrentHashMap<>();
            // Atomically set it if no other thread has already done so
            if (!attributesRef.compareAndSet(null, map)) {
                // If CAS failed, another thread won the race; use their map
                map = attributesRef.get();
            }
        }
        return map;
    }

    @Override
    public @NonNull String toString() {
        return new StringJoiner(", ", "DomainEventInterceptionContext[", "]")
                .add("event=" + event.getEventId())
                .add("phase=" + phase)
                .add("listenerId=" + (listenerId == null ? "<none>" : "'" + listenerId + "'"))
                .add("depth=" + getDepth())
                .add("correlationId=" + getCorrelationId())
                .add("idempotencyKey=" + getIdempotencyKey())
                .add("vetoed=" + vetoed.get())
                .add("roundTimedOut=" + isRoundTimedOut())
                .add("retryState=" + retryState)
                .toString();
    }

    /**
     * The lifecycle phase of an interception round carried by a
     * {@link DomainEventInterceptionContext}.
     *
     * @author Mohammad Yazdian
     * @since 0.1.0
     */
    public enum InterceptionPhase {

        /**
         * Event is being published: no target listener; veto suppresses whole
         * publication.
         */
        PUBLISH,

        /**
         * Event is being delivered to a single identified listener; veto skips
         * only that listener.
         */
        HANDLE;

        /**
         * Checks if this phase is the publishing phase.
         *
         * @return {@code true} if this is {@link #PUBLISH}, {@code false} otherwise
         */
        public boolean isPublishPhase() {
            return this == PUBLISH;
        }

        /**
         * Checks if this phase is the handling phase.
         *
         * @return {@code true} if this is {@link #HANDLE}, {@code false} otherwise
         */
        public boolean isHandlingPhase() {
            return this == HANDLE;
        }
    }

    /**
     * Fluent builder covering the full cross-product of phase, listener id,
     * retry policy, parent link and round timeout without exploding into
     * constructor overloads.
     *
     * @param <E> the concrete event type
     */
    public static final class Builder<E extends DomainEvent<?, ?>> {

        private final E event;
        private InterceptionPhase phase = InterceptionPhase.PUBLISH;
        private String listenerId;
        private RetryPolicy retryPolicy = RetryPolicy.none();
        private NestedInterceptionContext parent;
        private Duration roundTimeout;

        /**
         * Creates a builder seeded with the given event.
         *
         * @param event the intercepted event
         * @throws DomainEventException if event is {@code null}
         */
        public Builder(@NonNull E event) {
            if (event == null) throw new DomainEventException(EVENT_NULL_EXCEPTION);
            this.event = event;
        }

        /**
         * Sets the lifecycle phase.
         *
         * @param phase the phase
         * @return this builder instance for method chaining
         * @throws DomainEventException if phase is {@code null}
         */
        public Builder<E> phase(@NonNull InterceptionPhase phase) {
            if (phase == null) throw new DomainEventException(PHASE_NULL_EXCEPTION);
            this.phase = phase;
            return this;
        }

        /**
         * Sets the listener id.
         *
         * @param listenerId the listener id
         * @return this builder instance for method chaining
         */
        public Builder<E> listenerId(@Nullable String listenerId) {
            this.listenerId = listenerId;
            return this;
        }

        /**
         * Sets the retry policy.
         *
         * @param retryPolicy the retry policy
         * @return this builder instance for method chaining
         * @throws DomainEventException if policy is {@code null}
         */
        public Builder<E> retryPolicy(@NonNull RetryPolicy retryPolicy) {
            if (retryPolicy == null) throw new DomainEventException(RETRY_POLICY_NULL_EXCEPTION);
            this.retryPolicy = retryPolicy;
            return this;
        }

        /**
         * Sets the parent context for a nested round.
         *
         * @param parent the parent context
         * @return this builder instance for method chaining
         */
        public Builder<E> parent(@Nullable NestedInterceptionContext parent) {
            this.parent = parent;
            return this;
        }

        /**
         * Caps the total wall-clock budget of this interception
         * round. Enforced cooperatively.
         *
         * @param roundTimeout the timeout
         * @return this builder instance for method chaining
         */
        public Builder<E> roundTimeout(@Nullable Duration roundTimeout) {
            this.roundTimeout = roundTimeout;
            return this;
        }

        /**
         * Builds the interception context.
         *
         * @return a new context instance
         */
        public DomainEventInterceptionContext<E> build() {
            return new DomainEventInterceptionContext<>(event, phase, listenerId, retryPolicy, parent, roundTimeout);
        }
    }
}
