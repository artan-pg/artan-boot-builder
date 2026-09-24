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

package ir.artanpg.boot.application.port.driven.event;

import ir.artanpg.boot.domain.event.AggregateSnapshot;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

/**
 * Port for storing and loading aggregate snapshots.
 *
 * <p>Snapshots reduce the number of events that must be replayed when
 * rehydrating large streams.
 *
 * @author Mohammad Yazdian
 * @see AggregateSnapshot
 * @since 0.1.0
 */
public interface SnapshotStore {

    /**
     * Saves (or replaces) the snapshot for the aggregate.
     *
     * @param snapshot the snapshot to store
     */
    void save(@NonNull AggregateSnapshot snapshot);

    /**
     * Loads the latest snapshot for the given aggregate, if any.
     *
     * @param aggregateId the aggregate identifier
     * @return the snapshot, or empty
     */
    @NonNull
    Optional<AggregateSnapshot> load(@NonNull Identifier<?> aggregateId);
}
