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
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable context shared by all {@link DomainEventInterceptor}s participating
 * in a single interception phase (publishing or listening).
 *
 * <p>The context solves two problems that plain method parameters cannot:
 * <ul>
 *   <li><strong>Data exchange:</strong> interceptors can stash values
 *       (e.g. a start timestamp set by a profiling interceptor is read back
 *       by the same interceptor in the completion callback).</li>
 *   <li><strong>Veto:</strong> a security/validation interceptor can call
 *       {@link #veto(String)} to abort the current phase.</li>
 * </ul>
 *
 * <p>Beyond the free-form attribute map, the context carries first-class
 * fields describing the ongoing dispatch: the event itself, the target
 * listener id (only in the per-listener handling phases), the terminal
 * throwable (in error callbacks), the number of retries already attempted
 * and whether the operation should be retried again.
 *
 * <p>Instances are thread-safe: attributes are stored in a
 * {@link ConcurrentHashMap} and the veto flag is volatile, so a context may
 * be shared between concurrent async dispatches of the same publish batch.
 *
 * @param <E> the specific {@code DomainEvent} subclass to listen to
 * @author Mohammad Yazdian
 * @see DomainEventInterceptor
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
     * Flag indicating whether a veto has been raised.
     */
    private final AtomicBoolean vetoed = new AtomicBoolean();

    /**
     * The reason for the veto, if any.
     */
    private final AtomicReference<String> vetoReason = new AtomicReference<>();

    /**
     * The number of retries already attempted.
     */
    private final AtomicInteger retryCount = new AtomicInteger(0);

    /**
     * Flag indicating whether a retry has been requested.
     */
    private final AtomicBoolean retryRequested = new AtomicBoolean();

    /**
     * Arbitrary attributes shared among interceptors.
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Creates a context for a per-listener handling phase.
     *
     * @param event      the event being handled
     * @param listenerId the id of the target listener for publish-phase contexts
     * @throws DomainEventException if event {@code null}
     * @throws DomainEventException if listenerId {@code null} or {@code blank}
     */
    public DomainEventInterceptionContext(@NonNull E event,
                                          @NonNull String listenerId) {
        if (event == null) throw new DomainEventException("The event cannot be null");
        if (listenerId == null || listenerId.isBlank()) {
            throw new DomainEventException("The listenerId cannot be null or blank");
        }

        this.event = event;
        this.listenerId = listenerId;
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
    void setThrowable(@Nullable Throwable throwable) {
        this.throwable.set(throwable);
    }

    /**
     * Veto the current phase without a reason. The dispatcher must honor the
     * veto by skipping the remaining work of that phase.
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
     * Requests that the failed handling attempt to be retried by the dispatcher.
     *
     * <p>Only meaningful in error callbacks. The dispatcher decides how many
     * times to honor the request; each re-invocation increments
     * {@link #getRetryCount()}.
     */
    public void requestRetry() {
        this.retryRequested.set(true);
    }

    /**
     * Reports whether an interceptor asked for a retry.
     *
     * @return {@code true} if {@link #requestRetry()} was called, {@code false} otherwise
     */
    public boolean isRetryRequested() {
        return retryRequested.get();
    }

    /**
     * Returns how many retries have already been attempted in this round.
     *
     * @return the retry count, zero on the first attempt
     */
    public int getRetryCount() {
        return retryCount.get();
    }

    /**
     * Increments the retry count by one.
     */
    public void incrementRetryCount() {
        this.retryCount.incrementAndGet();
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
