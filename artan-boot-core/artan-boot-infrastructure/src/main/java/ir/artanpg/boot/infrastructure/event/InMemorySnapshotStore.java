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

import ir.artanpg.boot.application.port.driven.event.SnapshotStore;
import ir.artanpg.boot.domain.event.AggregateSnapshot;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory {@link SnapshotStore}.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public class InMemorySnapshotStore implements SnapshotStore {

    private final ConcurrentMap<String, AggregateSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public void save(@NonNull AggregateSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        this.snapshots.put(key(snapshot.getAggregateId()), snapshot);
    }

    @Override
    @NonNull
    public Optional<AggregateSnapshot> load(@NonNull Identifier<?> aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        return Optional.ofNullable(this.snapshots.get(key(aggregateId)));
    }

    private static String key(Identifier<?> aggregateId) {
        return String.valueOf(aggregateId.value());
    }
}
