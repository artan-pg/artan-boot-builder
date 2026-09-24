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
import ir.artanpg.boot.application.port.driven.event.SnapshotStore;
import ir.artanpg.boot.domain.event.AggregateSnapshot;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.StoredEvent;
import ir.artanpg.boot.domain.model.EventSourcedAggregateRoot;
import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.domain.model.Snapshottable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Base repository for event-sourced aggregates with optional snapshot support.
 *
 * <p>{@link #save} appends uncommitted events and publishes them. When a
 * {@link SnapshotStore} is configured and the aggregate implements
 * {@link Snapshottable}, a snapshot is taken every
 * {@link #getSnapshotThreshold()} events.
 *
 * <p>{@link #findById} restores from the latest snapshot (if any) and then
 * replays only subsequent events.
 *
 * @param <T> the aggregate type
 * @param <I> the identifier type
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public abstract class AbstractEventSourcedRepository<T extends EventSourcedAggregateRoot<I>, I extends Identifier<?>> {

    private final EventStore eventStore;

    private final Consumer<DomainEvent> eventDispatcher;

    @Nullable
    private final SnapshotStore snapshotStore;

    private final int snapshotThreshold;

    protected AbstractEventSourcedRepository(@NonNull EventStore eventStore, @NonNull DomainEventBus eventBus) {
        this(eventStore, eventBus::publish, null, 0);
    }

    protected AbstractEventSourcedRepository(@NonNull EventStore eventStore, @NonNull DomainEventPublisher publisher) {
        this(eventStore, publisher::publish, null, 0);
    }

    protected AbstractEventSourcedRepository(@NonNull EventStore eventStore,
                                            @NonNull DomainEventPublisher publisher,
                                            @Nullable DomainEventBus eventBus) {
        this(eventStore,
                (eventBus != null) ? eventBus::publish : publisher::publish,
                null,
                0);
    }

    protected AbstractEventSourcedRepository(@NonNull EventStore eventStore,
                                            @NonNull Consumer<DomainEvent> eventDispatcher,
                                            @Nullable SnapshotStore snapshotStore,
                                            int snapshotThreshold) {
        this.eventStore = Objects.requireNonNull(eventStore, "eventStore must not be null");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "eventDispatcher must not be null");
        this.snapshotStore = snapshotStore;
        this.snapshotThreshold = Math.max(0, snapshotThreshold);
    }

    protected AbstractEventSourcedRepository(@NonNull EventStore eventStore,
                                            @NonNull DomainEventPublisher publisher,
                                            @Nullable SnapshotStore snapshotStore,
                                            int snapshotThreshold) {
        this(eventStore, publisher::publish, snapshotStore, snapshotThreshold);
    }

    protected AbstractEventSourcedRepository(@NonNull EventStore eventStore,
                                            @NonNull DomainEventBus eventBus,
                                            @Nullable SnapshotStore snapshotStore,
                                            int snapshotThreshold) {
        this(eventStore, eventBus::publish, snapshotStore, snapshotThreshold);
    }

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

        maybeTakeSnapshot(aggregate);
    }

    public Optional<T> findById(@NonNull I id) {
        Objects.requireNonNull(id, "id must not be null");

        T aggregate = createInstance(id);
        long fromVersion = 0L;

        if (this.snapshotStore != null && aggregate instanceof Snapshottable snapshottable) {
            Optional<AggregateSnapshot> snapshot = this.snapshotStore.load(id);
            if (snapshot.isPresent()) {
                AggregateSnapshot snap = snapshot.get();
                snapshottable.restoreFromSnapshotState(snap.getState());
                aggregate.restoreVersion(snap.getVersion());
                fromVersion = snap.getVersion();
            }
        }

        List<StoredEvent> history = this.eventStore.load(id, fromVersion);
        if (fromVersion == 0L && history.isEmpty()) {
            return Optional.empty();
        }

        if (!history.isEmpty()) {
            aggregate.loadFromHistory(history);
        }

        return Optional.of(aggregate);
    }

    private void maybeTakeSnapshot(T aggregate) {
        if (this.snapshotStore == null || this.snapshotThreshold <= 0) {
            return;
        }
        if (!(aggregate instanceof Snapshottable snapshottable)) {
            return;
        }
        if (aggregate.getVersion() > 0 && aggregate.getVersion() % this.snapshotThreshold == 0) {
            AggregateSnapshot snapshot = new AggregateSnapshot(
                    aggregate.getId(),
                    aggregate.getVersion(),
                    snapshottable.createSnapshotState(),
                    Instant.now());
            this.snapshotStore.save(snapshot);
        }
    }

    protected abstract T createInstance(@NonNull I id);

    protected EventStore getEventStore() {
        return this.eventStore;
    }

    protected int getSnapshotThreshold() {
        return this.snapshotThreshold;
    }

    @Nullable
    protected SnapshotStore getSnapshotStore() {
        return this.snapshotStore;
    }
}
