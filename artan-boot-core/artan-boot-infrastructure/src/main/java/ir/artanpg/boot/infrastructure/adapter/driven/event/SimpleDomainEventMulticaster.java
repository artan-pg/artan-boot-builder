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

package ir.artanpg.boot.infrastructure.adapter.driven.event;

import ir.artanpg.boot.application.port.driven.event.support.DomainEventInterceptionContext;
import ir.artanpg.boot.application.port.driven.event.DomainEventInterceptor;
import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
import ir.artanpg.boot.application.port.driving.event.DomainEventListener;
import ir.artanpg.boot.application.port.driving.event.DomainEventSmartListener;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.retry.RetryPolicy;
import ir.artanpg.boot.domain.exception.event.DomainEventException;
import ir.artanpg.boot.domain.exception.event.InterceptorVetoException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * In-process {@link DomainEventMulticaster} that maintains a dynamic set of
 * listeners and fans out each published {@link DomainEvent}.
 *
 * <p>Thread-safe for concurrent registration and concurrent multicast calls.
 *
 * <p>Unchecked casts are isolated in a few private invoker helpers so that
 * {@link DomainEventInterceptionContext} and {@link DomainEventInterceptor}
 * remain generically typed at the public API boundary.
 *
 * @author Mohammad Yazdian
 * @see DomainEventMulticaster
 * @see DomainEventListener
 * @see DomainEventInterceptor
 * @since 0.1.0
 */
public final class SimpleDomainEventMulticaster implements DomainEventMulticaster {

    private final Map<String, DomainEventListener<?>> listenersById = new ConcurrentHashMap<>();

    private final CopyOnWriteArrayList<DomainEventListener<?>> listeners = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<DomainEventInterceptor<?>> interceptors = new CopyOnWriteArrayList<>();

    @Nullable
    private final Executor asyncExecutor;

    private final RetryPolicy defaultRetryPolicy;

    /**
     * Creates a multicaster that always dispatches on the calling thread and
     * does not retry failed handling.
     */
    public SimpleDomainEventMulticaster() {
        this(null, RetryPolicy.none());
    }

    /**
     * Creates a multicaster with an optional async executor and no retries.
     *
     * @param asyncExecutor executor for listeners that support async execution;
     *                      {@code null} forces synchronous dispatch
     */
    public SimpleDomainEventMulticaster(@Nullable Executor asyncExecutor) {
        this(asyncExecutor, RetryPolicy.none());
    }

    /**
     * Creates a multicaster with an optional async executor and retry policy.
     *
     * @param asyncExecutor      executor for async listeners; {@code null} forces sync
     * @param defaultRetryPolicy retry budget applied to each handling context
     * @throws DomainEventException if {@code defaultRetryPolicy} is {@code null}
     */
    public SimpleDomainEventMulticaster(@Nullable Executor asyncExecutor, @NonNull RetryPolicy defaultRetryPolicy) {
        if (defaultRetryPolicy == null) throw new DomainEventException("The defaultRetryPolicy cannot be null");

        this.asyncExecutor = asyncExecutor;
        this.defaultRetryPolicy = defaultRetryPolicy;
    }

    @Override
    public boolean addListener(@NonNull DomainEventListener<?> listener) {
        if (listener == null) throw new DomainEventException("The listener cannot be null");

        String id = resolveListenerId(listener);
        DomainEventListener<?> previous = listenersById.putIfAbsent(id, listener);
        if (previous != null) return false;

        listeners.add(listener);
        return true;
    }

    @Override
    public boolean removeListener(DomainEventListener<?> listener) {
        if (listener == null) return false;

        String id = resolveListenerId(listener);
        DomainEventListener<?> removed = listenersById.remove(id);
        if (removed == null) return false;

        listeners.remove(removed);
        return true;
    }

    @Override
    public boolean removeListener(String listenerId) {
        if (listenerId == null || listenerId.isBlank()) return false;

        DomainEventListener<?> removed = listenersById.remove(listenerId);
        if (removed == null) return false;

        listeners.remove(removed);
        return true;
    }

    @Override
    public void removeAllListeners() {
        listenersById.clear();
        listeners.clear();
    }

    @Override
    public <E extends DomainEvent<?, ?>> boolean addInterceptor(@NonNull DomainEventInterceptor<E> interceptor) {
        if (interceptor == null) throw new DomainEventException("The interceptor cannot be null");
        return interceptors.addIfAbsent(interceptor);
    }

    @Override
    public <E extends DomainEvent<?, ?>> boolean removeInterceptor(@NonNull DomainEventInterceptor<E> interceptor) {
        return interceptors.remove(interceptor);
    }

    /**
     * Returns a snapshot of currently registered listeners (registration order).
     *
     * @return unmodifiable snapshot
     */
    public List<DomainEventListener<?>> getListeners() {
        return List.copyOf(listeners);
    }

    /**
     * Returns a snapshot of registered interceptors.
     *
     * @return unmodifiable snapshot
     */
    public List<DomainEventInterceptor<?>> getInterceptors() {
        return List.copyOf(interceptors);
    }

    // -------------------------------------------------------------------------
    // Multicast
    // -------------------------------------------------------------------------

    @Override
    public void multicastEvent(@NonNull DomainEvent<?, ?> event) {
        if (event == null) throw new DomainEventException("The event cannot be null");

        DomainEventInterceptionContext<DomainEvent<?, ?>> publishContext =
                DomainEventInterceptionContext.forPublish(event);

        try {
            invokeBeforePublish(publishContext);
            if (publishContext.isVetoed()) {
                String reason = publishContext.getVetoReason() != null ?
                        publishContext.getVetoReason() : "Publication vetoed by interceptor";
                throw new InterceptorVetoException(reason);
            }

            for (DomainEventListener<?> listener : resolveMatchingListeners(event)) {
                dispatchToListener(event, listener);
            }
        } catch (InterceptorVetoException veto) {
            publishContext.setThrowable(veto);
            throw veto;
        } finally {
            invokeAfterPublish(publishContext);
        }
    }

    // -------------------------------------------------------------------------
    // Matching & ordering
    // -------------------------------------------------------------------------

    private List<DomainEventListener<?>> resolveMatchingListeners(DomainEvent<?, ?> event) {
        List<DomainEventListener<?>> matched = new ArrayList<>();
        for (DomainEventListener<?> listener : listeners) {
            if (matches(listener, event)) matched.add(listener);
        }

        matched.sort(Comparator.comparingInt(DomainEventListener::getOrder));
        return matched;
    }

    private boolean matches(DomainEventListener<?> listener, DomainEvent<?, ?> event) {
        if (listener instanceof DomainEventSmartListener<?> smart) {
            if (!smart.supportsEventType(event.getEventType())) return false;
            Predicate<DomainEvent<?, ?>> filter = smart.getFilter();
            return filter == null || filter.test(event);
        }

        Class<?> declared = resolveDeclaredEventType(listener);
        return declared == null || declared == DomainEvent.class || declared.isInstance(event);
    }

    private static Class<?> resolveDeclaredEventType(DomainEventListener<?> listener) {
        Class<?> clazz = listener.getClass();
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            Class<?> resolved = extractEventTypeArgument(genericInterface);
            if (resolved != null) return resolved;
        }
        return extractEventTypeArgument(clazz.getGenericSuperclass());
    }

    private static Class<?> extractEventTypeArgument(Type type) {
        if (!(type instanceof ParameterizedType parameterized)) return null;

        Type raw = parameterized.getRawType();
        if (!(raw instanceof Class<?> rawClass) || !DomainEventListener.class.isAssignableFrom(rawClass)) return null;

        Type arg = parameterized.getActualTypeArguments()[0];

        if (arg instanceof Class<?> eventClass) return eventClass;

        if (arg instanceof ParameterizedType nested && nested.getRawType() instanceof Class<?> nestedClass) {
            return nestedClass;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Per-listener dispatch
    // -------------------------------------------------------------------------

    private void dispatchToListener(DomainEvent<?, ?> event, DomainEventListener<?> listener) {
        String listenerId = resolveListenerId(listener);
        if (listener.supportsAsyncExecution() && asyncExecutor != null) {
            asyncExecutor.execute(() -> invokeListener(event, listener, listenerId));
        } else {
            invokeListener(event, listener, listenerId);
        }
    }

    private void invokeListener(DomainEvent<?, ?> event,
                                DomainEventListener<?> listener,
                                String listenerId) {
        DomainEventInterceptionContext<DomainEvent<?, ?>> context =
                DomainEventInterceptionContext.forHandling(event, listenerId, defaultRetryPolicy);

        invokeBeforeHandle(context);
        if (context.isVetoed()) return;

        while (true) {
            context.beginAttempt();
            try {
                processListener(listener, event);
                invokeAfterHandle(context);
                return;
            } catch (Exception ex) {
                context.setThrowable(ex);
                invokeOnError(context, ex);

                if (context.evaluateRetry()) {
                    sleepQuietly(context.nextRetryInterval());
                    continue;
                }

                try {
                    listener.exceptionHandler().handleException(ex, event);
                } catch (Exception _) {
                    // exception handler failures must not escape dispatch
                }
                return;
            }
        }
    }

    static void sleepQuietly(Duration wait) {
        if (wait == null || wait.isZero() || wait.isNegative()) return;

        try {
            Thread.sleep(wait.toMillis());
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    // -------------------------------------------------------------------------
    // Interceptor phase orchestration (no unchecked casts here)
    // -------------------------------------------------------------------------

    private void invokeBeforePublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
        DomainEvent<?, ?> event = context.getEvent();
        for (DomainEventInterceptor<?> interceptor : orderedInterceptors(true)) {
            if (!interceptor.appliesTo(event)) continue;

            try {
                callBeforePublish(interceptor, context);
            } catch (RuntimeException ex) {
                context.veto(ex.getMessage());
                context.setThrowable(ex);
                return;
            }
            if (context.isVetoed()) return;
        }
    }

    private void invokeAfterPublish(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
        DomainEvent<?, ?> event = context.getEvent();
        for (DomainEventInterceptor<?> interceptor : orderedInterceptors(false)) {
            if (!interceptor.appliesTo(event)) continue;
            try {
                callAfterPublish(interceptor, context);
            } catch (RuntimeException _) {
                // after-hooks must not break the publishing path
            }
        }
    }

    private void invokeBeforeHandle(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
        DomainEvent<?, ?> event = context.getEvent();
        for (DomainEventInterceptor<?> interceptor : orderedInterceptors(true)) {
            if (!interceptor.appliesTo(event)) continue;

            try {
                callBeforeHandle(interceptor, context);
            } catch (RuntimeException ex) {
                context.veto(ex.getMessage());
                context.setThrowable(ex);
                return;
            }
            if (context.isVetoed()) {
                return;
            }
        }
    }

    private void invokeAfterHandle(DomainEventInterceptionContext<DomainEvent<?, ?>> context) {
        DomainEvent<?, ?> event = context.getEvent();
        for (DomainEventInterceptor<?> interceptor : orderedInterceptors(false)) {
            if (!interceptor.appliesTo(event)) continue;

            try {
                callAfterHandle(interceptor, context);
            } catch (RuntimeException _) {
                // after-hooks must not break the handle path
            }
        }
    }

    private void invokeOnError(DomainEventInterceptionContext<DomainEvent<?, ?>> context, Throwable throwable) {
        DomainEvent<?, ?> event = context.getEvent();
        for (DomainEventInterceptor<?> interceptor : orderedInterceptors(true)) {
            if (!interceptor.appliesTo(event)) continue;

            try {
                callOnError(interceptor, context, throwable);
            } catch (RuntimeException _) {
                // onError failures must not mask the original exception path
            }
        }
    }

    private List<DomainEventInterceptor<?>> orderedInterceptors(boolean ascending) {
        List<DomainEventInterceptor<?>> copy = new ArrayList<>(interceptors);
        Comparator<DomainEventInterceptor<?>> byOrder = Comparator.comparingInt(DomainEventInterceptor::getOrder);

        if (!ascending) byOrder = byOrder.reversed();
        copy.sort(byOrder);

        return copy;
    }

    // -------------------------------------------------------------------------
    // Unchecked bridge helpers — the only place SuppressWarnings is allowed
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <E extends DomainEvent<?, ?>> void callBeforePublish(
            DomainEventInterceptor<?> interceptor, DomainEventInterceptionContext<DomainEvent<?, ?>> context) {

        DomainEventInterceptor<E> typedInterceptor = (DomainEventInterceptor<E>) interceptor;
        DomainEventInterceptionContext<E> typedContext = (DomainEventInterceptionContext<E>) context;
        typedInterceptor.beforePublish(typedContext);
    }

    @SuppressWarnings("unchecked")
    private static <E extends DomainEvent<?, ?>> void callAfterPublish(
            DomainEventInterceptor<?> interceptor, DomainEventInterceptionContext<DomainEvent<?, ?>> context) {

        DomainEventInterceptor<E> typedInterceptor = (DomainEventInterceptor<E>) interceptor;
        DomainEventInterceptionContext<E> typedContext = (DomainEventInterceptionContext<E>) context;
        typedInterceptor.afterPublish(typedContext);
    }

    @SuppressWarnings("unchecked")
    private static <E extends DomainEvent<?, ?>> void callBeforeHandle(
            DomainEventInterceptor<?> interceptor, DomainEventInterceptionContext<DomainEvent<?, ?>> context) {

        DomainEventInterceptor<E> typedInterceptor = (DomainEventInterceptor<E>) interceptor;
        DomainEventInterceptionContext<E> typedContext = (DomainEventInterceptionContext<E>) context;
        typedInterceptor.beforeHandle(typedContext);
    }

    @SuppressWarnings("unchecked")
    private static <E extends DomainEvent<?, ?>> void callAfterHandle(
            DomainEventInterceptor<?> interceptor, DomainEventInterceptionContext<DomainEvent<?, ?>> context) {

        DomainEventInterceptor<E> typedInterceptor = (DomainEventInterceptor<E>) interceptor;
        DomainEventInterceptionContext<E> typedContext = (DomainEventInterceptionContext<E>) context;
        typedInterceptor.afterHandle(typedContext);
    }

    @SuppressWarnings("unchecked")
    private static <E extends DomainEvent<?, ?>> void callOnError(
            DomainEventInterceptor<?> interceptor,
            DomainEventInterceptionContext<DomainEvent<?, ?>> context,
            Throwable throwable) {

        DomainEventInterceptor<E> typedInterceptor = (DomainEventInterceptor<E>) interceptor;
        DomainEventInterceptionContext<E> typedContext = (DomainEventInterceptionContext<E>) context;
        typedInterceptor.onError(typedContext, throwable);
    }

    @SuppressWarnings("unchecked")
    private static void processListener(DomainEventListener<?> listener, DomainEvent<?, ?> event) {
        ((DomainEventListener<DomainEvent<?, ?>>) listener).process(event);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String resolveListenerId(DomainEventListener<?> listener) {
        String id = listener.getListenerId();
        return (id == null || id.isBlank()) ? listener.getClass().getName() : id;
    }
}
