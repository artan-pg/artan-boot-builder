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
import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import ir.artanpg.boot.application.port.driven.event.EventStore;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.StoredEvent;
import ir.artanpg.boot.domain.model.EventSourcedAggregateRoot;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Base repository for event-sourced aggregates.
 *
 * <p>{@link #save} appends uncommitted events to the {@link EventStore} using
 * optimistic concurrency and then publishes them through the configured bus or
 * publisher. {@link #findById} rehydrates the aggregate from the stored stream.
 *
 * @param <T> the aggregate type
 * @param <I> the identifier type
 * @author Mohammad Yazdian
 * @see EventStore
 * @see EventSourcedAggregateRoot
 * @since 0.1.0
 */
public abstract class AbstractEventSourcedRepository<T extends EventSourcedAggregateRoot<I>, I extends Identifier<?>> {

    private final EventStore eventStore;

    private final Consumer<DomainEvent> eventDispatcher;

    /**
     * Creates a repository that publishes through the event bus.
     *
     * @param eventStore the event store; must not be {@code null}
     * @param eventBus   the event bus; must not be {@code null}
     */
    protected AbstractEventSourcedRepository(@NonNull EventStore eventStore, @NonNull DomainEventBus eventBus) {
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore must not be null");
        Objects.requireNonNull(eventBus, "eventBus must not be null");
        this.eventDispatcher = eventBus::publish;
    }

    /**
     * Creates a repository that publishes through the type-based publisher.
     *
     * @param eventStore the event store; must not be {@code null}
     * @param publisher  the publisher; must not be {@code null}
     */
    protected AbstractEventSourcedRepository(@NonNull EventStore eventStore, @NonNull DomainEventPublisher publisher) {
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore must not be null");
        Objects.requireNonNull(publisher, "publisher must not be null");
        this.eventDispatcher = publisher::publish;
    }

    /**
     * Creates a repository that prefers the bus when available.
     *
     * @param eventStore the event store; must not be {@code null}
     * @param publisher  the type-based publisher; must not be {@code null}
     * @param eventBus   optional event bus
     */
    protected AbstractEventSourcedRepository(@NonNull EventStore eventStore,
                                            @NonNull DomainEventPublisher publisher,
                                            @Nullable DomainEventBus eventBus) {
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore must not be null");
        Objects.requireNonNull(publisher, "publisher must not be null");
        this.eventDispatcher = (eventBus != null) ? eventBus::publish : publisher::publish;
    }

    /**
     * Persists uncommitted events and publishes them.
     *
     * <p>Expected version is {@code aggregate.getVersion() - uncommitted.size()}.
     *
     * @param aggregate the aggregate to save; must not be {@code null}
     */
    public void save(@NonNull T aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        List<DomainEvent> uncommitted = aggregate.getDomainEvents();
        if (uncommitted.isEmpty()) {
            return;
        }

        long expectedVersion = aggregate.getVersion() - uncommitted.size();
        this.eventStore.append(aggregate.getId(), uncommitted, expectedVersion);

        for (DomainEvent event : uncommitted) {
            this.eventDispatcher.accept(event);
        }
    }

    /**
     * Loads and rehydrates the aggregate by id.
     *
     * @param id the aggregate identifier
     * @return the aggregate, or empty if no stream exists
     */
    public Optional<T> findById(@NonNull I id) {
        Objects.requireNonNull(id, "id must not be null");
        List<StoredEvent> history = this.eventStore.load(id);
        if (history.isEmpty()) {
            return Optional.empty();
        }
        T aggregate = createInstance(id);
        aggregate.loadFromHistory(history);
        return Optional.of(aggregate);
    }

    /**
     * Factory method used by {@link #findById} to create an empty aggregate
     * instance before rehydration.
     *
     * @param id the aggregate identifier
     * @return a new aggregate instance with the given id
     */
    protected abstract T createInstance(@NonNull I id);

    /**
     * Returns the underlying event store.
     *
     * @return the event store
     */
    protected EventStore getEventStore() {
        return this.eventStore;
    }
}
