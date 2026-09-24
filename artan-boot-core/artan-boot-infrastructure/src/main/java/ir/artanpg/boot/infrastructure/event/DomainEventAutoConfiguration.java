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

import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the domain event infrastructure.
 *
 * <p>Registers the following beans when they are not already defined by the
 * application:
 * <ul>
 *   <li>{@link SimpleDomainEventMulticaster} as the default
 *       {@link DomainEventMulticaster}</li>
 *   <li>{@link SimpleDomainEventPublisher} as the default
 *       {@link DomainEventPublisher}</li>
 *   <li>{@link DomainEventHandlerAnnotationBeanPostProcessor} to detect and
 *       register methods annotated with
 *       {@link ir.artanpg.boot.application.annotation.DomainEventHandler}</li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @see DomainEventMulticaster
 * @see DomainEventPublisher
 * @see DomainEventHandlerAnnotationBeanPostProcessor
 * @since 0.1.0
 */
@AutoConfiguration
public class DomainEventAutoConfiguration {

    /**
     * Creates the default synchronous multicaster if none is provided by the
     * application.
     *
     * @return a new {@link SimpleDomainEventMulticaster} instance
     */
    @Bean
    @ConditionalOnMissingBean(DomainEventMulticaster.class)
    public DomainEventMulticaster domainEventMulticaster() {
        return new SimpleDomainEventMulticaster();
    }

    /**
     * Creates the default domain event publisher that delegates to the
     * multicaster.
     *
     * @param multicaster the multicaster to use for event dispatch
     * @return a new {@link SimpleDomainEventPublisher} instance
     */
    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public DomainEventPublisher domainEventPublisher(DomainEventMulticaster multicaster) {
        return new SimpleDomainEventPublisher(multicaster);
    }

    /**
     * Creates the bean post-processor that registers
     * {@code @DomainEventHandler} methods as listeners.
     *
     * @param multicaster the multicaster that will receive the registered listeners
     * @return a new {@link DomainEventHandlerAnnotationBeanPostProcessor} instance
     */
    @Bean
    @ConditionalOnMissingBean(DomainEventHandlerAnnotationBeanPostProcessor.class)
    public static DomainEventHandlerAnnotationBeanPostProcessor domainEventHandlerAnnotationBeanPostProcessor(
            DomainEventMulticaster multicaster) {
        return new DomainEventHandlerAnnotationBeanPostProcessor(multicaster);
    }
}
