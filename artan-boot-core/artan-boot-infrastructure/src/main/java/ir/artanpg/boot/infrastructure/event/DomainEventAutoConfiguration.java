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

import ir.artanpg.boot.application.port.driven.event.DomainEventBus;
import ir.artanpg.boot.application.port.driven.event.DomainEventInterceptor;
import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the domain event infrastructure (phase 1 + phase 2).
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
@AutoConfiguration
public class DomainEventAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DomainEventMulticaster.class)
    public DomainEventMulticaster domainEventMulticaster() {
        return new SimpleDomainEventMulticaster();
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class)
    public DomainEventPublisher domainEventPublisher(DomainEventMulticaster multicaster) {
        return new SimpleDomainEventPublisher(multicaster);
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventBus.class)
    public DomainEventBus domainEventBus(DomainEventPublisher publisher) {
        return new SimpleDomainEventBus(publisher);
    }

    @Bean
    @ConditionalOnMissingBean(DomainEventHandlerAnnotationBeanPostProcessor.class)
    public static DomainEventHandlerAnnotationBeanPostProcessor domainEventHandlerAnnotationBeanPostProcessor(
            DomainEventMulticaster multicaster,
            DomainEventBus eventBus) {
        return new DomainEventHandlerAnnotationBeanPostProcessor(multicaster, eventBus);
    }

    @Bean
    @ConditionalOnMissingBean(AggregateDomainEventPublisher.class)
    public AggregateDomainEventPublisher aggregateDomainEventPublisher(
            DomainEventPublisher publisher,
            DomainEventBus eventBus) {
        return new AggregateDomainEventPublisher(publisher, eventBus);
    }

    @Bean
    @ConditionalOnProperty(prefix = "artan.boot.event.logging-interceptor", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(LoggingDomainEventInterceptor.class)
    public DomainEventInterceptor loggingDomainEventInterceptor(DomainEventMulticaster multicaster) {
        LoggingDomainEventInterceptor interceptor = new LoggingDomainEventInterceptor();
        multicaster.addInterceptor(interceptor);
        return interceptor;
    }
}
