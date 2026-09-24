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

import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.model.AggregateRoot;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

/**
 * Helper that publishes all domain events collected by an
 * {@link AggregateRoot}.
 *
 * <p>Typical usage inside an application service after persisting the
 * aggregate:
 * <pre>{@code
 * repository.save(account);
 * aggregateDomainEventPublisher.publishEventsFrom(account);
 * }</pre>
 *
 * <p>Because {@link AggregateRoot#getDomainEvents()} returns a defensive copy
 * and clears the internal collection, calling this method more than once on
 * the same aggregate instance has no additional effect.
 *
 * @author Mohammad Yazdian
 * @see DomainEventPublisher
 * @see AggregateRoot#getDomainEvents()
 * @since 0.1.0
 */
public class AggregateDomainEventPublisher {

    private final DomainEventPublisher publisher;

    /**
     * Creates a new aggregate domain event publisher.
     *
     * @param publisher the publisher used to dispatch events; must not be {@code null}
     */
    public AggregateDomainEventPublisher(@NonNull DomainEventPublisher publisher) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
    }

    /**
     * Publishes all domain events currently registered on the given aggregate.
     *
     * @param aggregate the aggregate whose events should be published; must not be {@code null}
     */
    public void publishEventsFrom(@NonNull AggregateRoot<?> aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        List<DomainEvent> events = aggregate.getDomainEvents();
        for (DomainEvent event : events) {
            this.publisher.publish(event);
        }
    }
}
