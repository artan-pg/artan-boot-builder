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

import ir.artanpg.boot.domain.exception.InterceptorVetoException;
import org.jspecify.annotations.NonNull;

/**
 * Cross-cutting hook around the publication and per-listener handling of
 * domain events.
 *
 * <p>Unlike a passive observer, an interceptor participates in the dispatch
 * pipeline and can influence its outcome through two cooperative mechanisms
 * carried by {@link DomainEventInterceptionContext}:
 * <ul>
 *   <li><strong>Veto:</strong> {@code context.veto(reason)} aborts the current
 *       phase — remaining interceptors are skipped and delivery is suppressed.
 *       The fail-fast alternative is throwing
 *       {@link InterceptorVetoException}.</li>
 *   <li><strong>Retry request:</strong> inside an error callback,
 *       {@code context.requestRetry()} asks the dispatcher to re-attempt the
 *       failed handling step (subject to the dispatcher's retry budget).</li>
 * </ul>
 *
 * <p>Interceptors exchange state (e.g. profiling timestamps, correlation
 * data) via the context attribute map instead of thread-locals, so they remain
 * correct across asynchronous dispatches.
 *
 * <h2>Phases</h2>
 * <pre>
 * publish(event)
 *   |- beforePublish(context)            [vetoable]
 *   |    |- dispatch to each listener:
 *   |    |     |- beforeHandle(context)  [vetoable - skips THIS listener]
 *   |    |     |- listener.process(event)
 *   |    |     |     |- success -&gt; afterHandle(context)
 *   |    |     |     \- failure -&gt; onError(context)  [may requestRetry()]
 *   |    |                     \- retries exhausted -&gt; onHandlingFailure(context)
 *   |    \- batch bookkeeping
 *   |- afterPublish(context)             [always runs, even if vetoed/failed]
 *        \- onTermination(context)       [once per phase, success or failure]
 * </pre>
 *
 * <h2>Ordering</h2>
 * Implement {@link OrderedDomainEventInterceptor} to control position within a
 * phase chain; unordered interceptors run after ordered ones and keep their
 * registration order relative to each other.
 *
 * <h2>Error policy</h2>
 * An exception escaping any interceptor callback is treated as an interceptor
 * bug: the dispatcher logs it, converts it into a
 * {@link DomainEventInterceptionContext#veto(String) veto} of the current
 * phase when thrown from a "before" callback, and otherwise ignores it so one
 * faulty interceptor cannot break event delivery.
 *
 * <p>All methods have default empty implementations so that implementors only
 * override the callbacks they care about.
 *
 * @author Mohammad Yazdian
 * @see DomainEventInterceptionContext
 * @see OrderedDomainEventInterceptor
 * @see InterceptorVetoException
 * @since 0.1.0
 */
public interface DomainEventInterceptor {

    /**
     * Invoked once per {@code publish}/{@code publishAll} call, before the
     * event is routed to any listener.
     *
     * <p>Vetoing here suppresses the entire publication.
     *
     * @param context the interception context for the publishing phase
     */
    default void beforePublish(@NonNull DomainEventInterceptionContext context) {
    }

    /**
     * Invoked once per publication after all matched listeners have been
     * dispatched, regardless of individual successes or failures. Inspect
     * {@link DomainEventInterceptionContext#getThrowable()} to learn whether
     * the round ended in an error.
     *
     * @param context the interception context for the publishing phase
     */
    default void afterPublish(@NonNull DomainEventInterceptionContext context) {
    }

    /**
     * Invoked immediately before a listener processes the event.
     *
     * <p>Vetoing here skips only this listener; other listeners still receive
     * the event.
     *
     * @param context the interception context carrying event and listener id
     */
    default void beforeHandle(@NonNull DomainEventInterceptionContext context) {
    }

    /**
     * Invoked after a listener processed the event successfully.
     *
     * @param context the interception context carrying event and listener id
     */
    default void afterHandle(@NonNull DomainEventInterceptionContext context) {
    }

    /**
     * Invoked when a listener throws while processing the event, before the
     * listener's own exception handler runs.
     *
     * <p>Call {@link DomainEventInterceptionContext#requestRetry()} to ask the
     * dispatcher to re-attempt this listener invocation.
     *
     * @param context the interception context
     * @see DomainEventInterceptionContext#getThrowable()
     * @see DomainEventInterceptionContext#getRetryCount()
     */
    default void onError(@NonNull DomainEventInterceptionContext context) {
    }

    /**
     * Invoked once a listener invocation has definitively failed — after the
     * retry budget was exhausted or no retry was requested.
     *
     * <p>Typical uses: dead-letter routing, alerting, audit records.
     *
     * @param context the interception context of the terminal failure
     */
    default void onHandlingFailure(@NonNull DomainEventInterceptionContext context) {
    }

    /**
     * Invoked exactly once when the current phase terminates, whether it
     * completed normally, was vetoed, or failed. Runs after the corresponding
     * {@code after*} callback and is the right place for releasing resources
     * acquired in a {@code before*} callback (timers, spans, counters).
     *
     * @param context the interception context of the terminating phase
     */
    default void onTermination(@NonNull DomainEventInterceptionContext context) {
    }
}
