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
import ir.artanpg.boot.domain.event.DomainEvent;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Simple implementation of {@link DomainEventPublisher} that delegates event
 * publication to a {@link DomainEventMulticaster}.
 *
 * <p>This class is intentionally thin: its only responsibility is to forward
 * the event to the multicaster. All listener resolution, ordering, interceptor
 * application and exception handling is performed by the multicaster.
 *
 * @author Mohammad Yazdian
 * @see DomainEventPublisher
 * @see DomainEventMulticaster
 * @see SimpleDomainEventMulticaster
 * @since 0.1.0
 */
public class SimpleDomainEventPublisher implements DomainEventPublisher {

    private final DomainEventMulticaster multicaster;

    /**
     * Creates a new publisher that uses the given multicaster.
     *
     * @param multicaster the multicaster that will dispatch events; must not be {@code null}
     */
    public SimpleDomainEventPublisher(@NonNull DomainEventMulticaster multicaster) {
        this.multicaster = Objects.requireNonNull(multicaster, "multicaster must not be null");
    }

    @Override
    public void publish(@NonNull DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        this.multicaster.multicastEvent(event);
    }
}
