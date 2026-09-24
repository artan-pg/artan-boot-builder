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

import ir.artanpg.boot.application.port.driven.event.EventStore;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.StoredEvent;
import ir.artanpg.boot.domain.exception.EventConcurrencyException;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory {@link EventStore} suitable for tests and local
 * development.
 *
 * <p>Streams are keyed by {@code String.valueOf(aggregateId.value())}.
 * Optimistic concurrency is enforced on every {@link #append}.
 *
 * @author Mohammad Yazdian
 * @see EventStore
 * @since 0.1.0
 */
public class InMemoryEventStore implements EventStore {

    private final ConcurrentMap<String, List<StoredEvent>> streams = new ConcurrentHashMap<>();

    private final Clock clock;

    /**
     * Creates a store using the system UTC clock.
     */
    public InMemoryEventStore() {
        this(Clock.systemUTC());
    }

    /**
     * Creates a store with a custom clock (useful for tests).
     *
     * @param clock the clock used for {@code storedAt}; must not be {@code null}
     */
    public InMemoryEventStore(@NonNull Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void append(@NonNull Identifier<?> aggregateId,
                       @NonNull List<DomainEvent> events,
                       long expectedVersion) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(events, "events must not be null");
        if (events.isEmpty()) {
            return;
        }

        String key = streamKey(aggregateId);
        this.streams.compute(key, (k, existing) -> {
            List<StoredEvent> stream = (existing != null) ? new ArrayList<>(existing) : new ArrayList<>();
            long currentVersion = stream.isEmpty() ? 0L : stream.get(stream.size() - 1).getStreamVersion();

            // expectedVersion == current stream version (0 for empty stream)
            // Also accept -1 as alias for "new stream" (currentVersion must be 0)
            long effectiveExpected = (expectedVersion == -1L) ? 0L : expectedVersion;
            if (effectiveExpected != currentVersion) {
                throw new EventConcurrencyException(effectiveExpected, currentVersion);
            }

            Instant storedAt = Instant.now(this.clock);
            long nextVersion = currentVersion;
            for (DomainEvent event : events) {
                nextVersion++;
                stream.add(new StoredEvent(event, nextVersion, storedAt));
            }
            return stream;
        });
    }

    @Override
    @NonNull
    public List<StoredEvent> load(@NonNull Identifier<?> aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        List<StoredEvent> stream = this.streams.get(streamKey(aggregateId));
        return (stream == null) ? List.of() : List.copyOf(stream);
    }

    @Override
    @NonNull
    public List<StoredEvent> load(@NonNull Identifier<?> aggregateId, long fromVersion) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return load(aggregateId).stream()
                .filter(e -> e.getStreamVersion() > fromVersion)
                .toList();
    }

    /**
     * Returns the current version of the stream, or {@code 0} if empty.
     *
     * @param aggregateId the aggregate id
     * @return current stream version
     */
    public long currentVersion(@NonNull Identifier<?> aggregateId) {
        List<StoredEvent> stream = load(aggregateId);
        return stream.isEmpty() ? 0L : stream.get(stream.size() - 1).getStreamVersion();
    }

    private static String streamKey(Identifier<?> aggregateId) {
        return String.valueOf(aggregateId.value());
    }
}
