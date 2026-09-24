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
import ir.artanpg.boot.application.port.driven.event.DomainEventBus;
import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
import ir.artanpg.boot.domain.event.EventTopic;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link BeanPostProcessor} that detects methods annotated with
 * {@link DomainEventHandler} and registers them either with the type-based
 * {@link DomainEventMulticaster} or with the topic-based {@link DomainEventBus}.
 *
 * <p>When {@link DomainEventHandler#topic()} is empty the adapter is added to
 * the multicaster. When a topic is specified the adapter is subscribed on the
 * event bus.
 *
 * @author Mohammad Yazdian
 * @see DomainEventHandler
 * @see DomainEventHandlerMethodAdapter
 * @see DomainEventMulticaster
 * @see DomainEventBus
 * @since 0.1.0
 */
public class DomainEventHandlerAnnotationBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DomainEventHandlerAnnotationBeanPostProcessor.class);

    private final DomainEventMulticaster multicaster;

    @Nullable
    private final DomainEventBus eventBus;

    /**
     * Creates a post-processor that supports both type-based and topic-based
     * registration.
     *
     * @param multicaster the multicaster for type-based handlers; must not be {@code null}
     * @param eventBus    the event bus for topic-based handlers; may be {@code null}
     *                    if topic-based handlers are not used
     */
    public DomainEventHandlerAnnotationBeanPostProcessor(@NonNull DomainEventMulticaster multicaster,
                                                         @Nullable DomainEventBus eventBus) {
        this.multicaster = Objects.requireNonNull(multicaster, "multicaster must not be null");
        this.eventBus = eventBus;
    }

    /**
     * Creates a post-processor for type-based registration only.
     *
     * @param multicaster the multicaster; must not be {@code null}
     */
    public DomainEventHandlerAnnotationBeanPostProcessor(@NonNull DomainEventMulticaster multicaster) {
        this(multicaster, null);
    }

    @Override
    public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        List<Method> handlerMethods = new ArrayList<>();
        ReflectionUtils.doWithMethods(targetClass, method -> {
            if (AnnotatedElementUtils.hasAnnotation(method, DomainEventHandler.class)) {
                handlerMethods.add(method);
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);

        for (Method method : handlerMethods) {
            DomainEventHandlerMethodAdapter adapter = new DomainEventHandlerMethodAdapter(bean, method);

            if (adapter.isTopicBased()) {
                EventTopic topic = adapter.getTopic().orElseThrow();
                if (this.eventBus == null) {
                    throw new IllegalStateException(
                            "@DomainEventHandler on " + method + " specifies topic [" + topic.getName()
                                    + "] but no DomainEventBus is available");
                }
                this.eventBus.subscribe(topic, adapter, adapter.getOrder());
                log.debug("Registered @DomainEventHandler method [{}] on bean [{}] to topic [{}]",
                        method.toGenericString(), beanName, topic.getName());
            }
            else {
                this.multicaster.addDomainEventListener(adapter);
                log.debug("Registered @DomainEventHandler method [{}] on bean [{}] as type-based listener [{}]",
                        method.toGenericString(), beanName, adapter.getListenerId());
            }
        }

        return bean;
    }
}
