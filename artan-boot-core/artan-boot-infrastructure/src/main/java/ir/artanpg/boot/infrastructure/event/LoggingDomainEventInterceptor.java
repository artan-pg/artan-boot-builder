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

import ir.artanpg.boot.application.port.driven.event.DomainEventInterceptor;
import ir.artanpg.boot.application.port.driven.event.DomainEventListener;
import ir.artanpg.boot.domain.event.DomainEvent;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample {@link DomainEventInterceptor} that logs domain event processing at
 * the debug and error levels.
 *
 * <p>This interceptor is useful for monitoring and troubleshooting event
 * handling. It can be registered with a {@link ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster}
 * either manually or via auto-configuration.
 *
 * @author Mohammad Yazdian
 * @see DomainEventInterceptor
 * @since 0.1.0
 */
public class LoggingDomainEventInterceptor implements DomainEventInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventInterceptor.class);

    @Override
    public void beforeHandle(@NonNull DomainEvent event, @NonNull DomainEventListener listener) {
        log.debug("Handling domain event [{}:{}] with listener [{}]",
                event.getEventType().getName(),
                event.getEventId(),
                listener.getClass().getSimpleName());
    }

    @Override
    public void afterHandle(@NonNull DomainEvent event, @NonNull DomainEventListener listener) {
        log.debug("Successfully handled domain event [{}:{}] with listener [{}]",
                event.getEventType().getName(),
                event.getEventId(),
                listener.getClass().getSimpleName());
    }

    @Override
    public void onError(@NonNull DomainEvent event,
                        @NonNull DomainEventListener listener,
                        @NonNull Exception exception) {
        log.error("Error while handling domain event [{}:{}] with listener [{}]",
                event.getEventType().getName(),
                event.getEventId(),
                listener.getClass().getSimpleName(),
                exception);
    }
}
