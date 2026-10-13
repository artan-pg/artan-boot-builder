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

import ir.artanpg.boot.application.port.driven.event.DomainEventMulticaster;
import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;

/**
 * {@link DomainEventPublisher} that delegates publication to a
 * {@link DomainEventMulticaster}.
 *
 * <p>Typical in-process adapter: application services call
 * {@link #publish(DomainEvent)} after a successful transaction commit; the
 * multicaster fans the event out to registered listeners.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public final class SimpleDomainEventPublisher implements DomainEventPublisher {

    private final DomainEventMulticaster multicaster;

    /**
     * Creates a publisher backed by the given multicaster.
     *
     * @param multicaster the observer multicaster
     * @throws DomainEventException if {@code multicaster} is {@code null}
     */
    public SimpleDomainEventPublisher(@NonNull DomainEventMulticaster multicaster) {
        if (multicaster == null) throw new DomainEventException("The DomainEventMulticaster cannot be null");
        this.multicaster = multicaster;
    }

    @Override
    public void publish(@NonNull DomainEvent<?, ?> event) {
        if (event == null) throw new DomainEventException("The event cannot be null");
        multicaster.multicastEvent(event);
    }
}
