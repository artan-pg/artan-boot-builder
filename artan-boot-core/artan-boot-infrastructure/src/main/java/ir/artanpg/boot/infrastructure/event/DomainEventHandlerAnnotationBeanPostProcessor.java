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
import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
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
 * {@link DomainEventHandler} and registers them as listeners with a
 * {@link DomainEventMulticaster}.
 *
 * <p>For each qualifying method a {@link DomainEventHandlerMethodAdapter} is
 * created and added to the multicaster. The processor runs after bean
 * initialization so that the target bean is fully constructed.
 *
 * @author Mohammad Yazdian
 * @see DomainEventHandler
 * @see DomainEventHandlerMethodAdapter
 * @see DomainEventMulticaster
 * @since 0.1.0
 */
public class DomainEventHandlerAnnotationBeanPostProcessor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DomainEventHandlerAnnotationBeanPostProcessor.class);

    private final DomainEventMulticaster multicaster;

    /**
     * Creates a new post-processor that registers handlers with the given
     * multicaster.
     *
     * @param multicaster the multicaster to register listeners with; must not be {@code null}
     */
    public DomainEventHandlerAnnotationBeanPostProcessor(@NonNull DomainEventMulticaster multicaster) {
        this.multicaster = Objects.requireNonNull(multicaster, "multicaster must not be null");
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
            this.multicaster.addDomainEventListener(adapter);
            log.debug("Registered @DomainEventHandler method [{}] on bean [{}] as listener [{}]",
                    method.toGenericString(), beanName, adapter.getListenerId());
        }

        return bean;
    }
}
