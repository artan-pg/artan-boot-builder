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

package ir.artanpg.boot.infrastructure.event;

import ir.artanpg.boot.application.annotation.DomainEventHandler;
import ir.artanpg.boot.application.port.driven.event.DomainEventSmartListener;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventTopic;
import ir.artanpg.boot.domain.exception.DomainException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Adapter that turns a method annotated with {@link DomainEventHandler} into a
 * {@link DomainEventSmartListener}.
 *
 * <p>When the annotation specifies a non-empty {@link DomainEventHandler#topic()},
 * {@link #getTopic()} returns the corresponding {@link EventTopic} so that the
 * registration infrastructure can subscribe the adapter on the event bus.
 *
 * @author Mohammad Yazdian
 * @see DomainEventHandler
 * @see DomainEventSmartListener
 * @since 0.1.0
 */
public class DomainEventHandlerMethodAdapter implements DomainEventSmartListener {

    private final Object bean;

    private final Method method;

    private final Set<Class<? extends DomainEvent>> supportedEventTypes;

    private final int order;

    private final boolean async;

    private final String listenerId;

    @Nullable
    private final EventTopic topic;

    /**
     * Creates a new adapter for the given bean and annotated method.
     *
     * @param bean   the bean instance that owns the handler method; must not be {@code null}
     * @param method the method annotated with {@link DomainEventHandler}; must not be {@code null}
     * @throws DomainException if the method signature is invalid
     */
    @SuppressWarnings("unchecked")
    public DomainEventHandlerMethodAdapter(@NonNull Object bean, @NonNull Method method) {
        this.bean = Objects.requireNonNull(bean, "bean must not be null");
        this.method = Objects.requireNonNull(method, "method must not be null");

        DomainEventHandler annotation = method.getAnnotation(DomainEventHandler.class);
        if (annotation == null) {
            throw new DomainException("Method must be annotated with @DomainEventHandler: " + method);
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new DomainException(
                    "@DomainEventHandler method must have exactly one parameter: " + method);
        }
        if (!DomainEvent.class.isAssignableFrom(parameterTypes[0])) {
            throw new DomainException(
                    "@DomainEventHandler method parameter must be a subtype of DomainEvent: " + method);
        }

        Class<? extends DomainEvent>[] declaredTypes = annotation.value();
        if (declaredTypes.length > 0) {
            this.supportedEventTypes = Arrays.stream(declaredTypes).collect(Collectors.toSet());
        }
        else {
            this.supportedEventTypes = Set.of((Class<? extends DomainEvent>) parameterTypes[0]);
        }

        this.order = annotation.order();
        this.async = annotation.async();
        this.listenerId = bean.getClass().getName() + "#" + method.getName();

        String topicName = annotation.topic();
        this.topic = (topicName == null || topicName.isBlank()) ? null : EventTopic.of(topicName);

        this.method.setAccessible(true);
    }

    @Override
    public void process(@NonNull DomainEvent event) {
        try {
            this.method.invoke(this.bean, event);
        }
        catch (IllegalAccessException ex) {
            throw new DomainException("Failed to access @DomainEventHandler method: " + this.method, ex);
        }
        catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new DomainException("@DomainEventHandler method threw an exception: " + this.method, cause);
        }
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends DomainEvent> eventType) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        return this.supportedEventTypes.stream()
                .anyMatch(supported -> supported.isAssignableFrom(eventType));
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public boolean supportsAsyncExecution() {
        return this.async;
    }

    @Override
    public String getListenerId() {
        return this.listenerId;
    }

    /**
     * Returns the topic this handler is bound to, if any.
     *
     * @return the topic, or empty when the handler is type-based only
     */
    public Optional<EventTopic> getTopic() {
        return Optional.ofNullable(this.topic);
    }

    /**
     * Returns whether this handler is topic-based.
     *
     * @return {@code true} if a topic was specified on the annotation
     */
    public boolean isTopicBased() {
        return this.topic != null;
    }

    public Object getBean() {
        return this.bean;
    }

    public Method getMethod() {
        return this.method;
    }

    @Override
    public String toString() {
        return "DomainEventHandlerMethodAdapter[" +
                "listenerId='" + this.listenerId + '\'' +
                ", order=" + this.order +
                ", async=" + this.async +
                ", topic=" + this.topic +
                "]";
    }
}
