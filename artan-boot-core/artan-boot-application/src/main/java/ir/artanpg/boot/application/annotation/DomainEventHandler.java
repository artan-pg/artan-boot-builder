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

package ir.artanpg.boot.application.annotation;

import ir.artanpg.boot.domain.event.DomainEvent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method as a domain event handler.
 *
 * <p>Methods annotated with {@code @DomainEventHandler} are automatically
 * registered as listeners for the specified domain event types. The method
 * must accept a single parameter that is a subtype of {@link DomainEvent}.
 *
 * <h2>Method Requirements:</h2>
 * <ul>
 *   <li>Must be a public method</li>
 *   <li>Must have exactly one parameter of a type that implements
 *       {@link DomainEvent}</li>
 *   <li>Return type is ignored (typically {@code void})</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @DomainEventHandler
 * public void onAccountOpened(AccountOpenedEvent event) {
 *     // handle the event
 * }
 *
 * @DomainEventHandler(order = 10, async = false)
 * public void onHighPriorityEvent(PaymentCompletedEvent event) {
 *     // handle with higher priority
 * }
 * }</pre>
 *
 * @author Mohammad Yazdian
 * @see ir.artanpg.boot.application.port.driven.event.DomainEventListener
 * @see ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster
 * @since 0.1.0
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DomainEventHandler {

    /**
     * The domain event types that this handler supports.
     *
     * <p>If left empty (the default), the type of the method parameter is used
     * to determine which events this handler will receive.
     *
     * @return the supported event types
     */
    Class<? extends DomainEvent>[] value() default {};

    /**
     * The order of this handler relative to other handlers for the same event.
     *
     * <p>Handlers with a lower order value have higher priority and are
     * invoked earlier. The default value is {@link Integer#MAX_VALUE},
     * meaning the lowest priority.
     *
     * @return the order value
     */
    int order() default Integer.MAX_VALUE;

    /**
     * Whether this handler supports asynchronous execution.
     *
     * <p>When {@code true}, the multicaster may execute this handler on a
     * different thread.
     *
     * @return {@code true} if asynchronous execution is supported, {@code false} otherwise
     */
    boolean async() default true;
}
