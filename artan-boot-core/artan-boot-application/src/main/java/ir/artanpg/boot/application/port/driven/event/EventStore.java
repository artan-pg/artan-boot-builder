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

import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.StoredEvent;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Port for persisting and replaying domain events (Event Sourcing foundation).
 *
 * <p>Implementations append events atomically for a given aggregate stream and
 * support loading the full or partial history for rehydration.
 *
 * @author Mohammad Yazdian
 * @see StoredEvent
 * @since 0.1.0
 */
public interface EventStore {

    /**
     * Appends the given events to the stream of the aggregate.
     *
     * <p>The {@code expectedVersion} is used for optimistic concurrency control.
     * If the current stream version does not match, the implementation should
     * fail (e.g. throw a concurrency exception).
     *
     * @param aggregateId     the aggregate identifier
     * @param events          the events to append (in order)
     * @param expectedVersion the version expected before append ({@code -1} for a new stream)
     */
    void append(@NonNull Identifier<?> aggregateId,
                @NonNull List<DomainEvent> events,
                long expectedVersion);

    /**
     * Loads the full event stream for the given aggregate.
     *
     * @param aggregateId the aggregate identifier
     * @return ordered list of stored events (empty if none)
     */
    @NonNull
    List<StoredEvent> load(@NonNull Identifier<?> aggregateId);

    /**
     * Loads events for the aggregate starting after the given version.
     *
     * @param aggregateId   the aggregate identifier
     * @param fromVersion   exclusive lower bound (events with version &gt; fromVersion)
     * @return ordered list of stored events
     */
    @NonNull
    List<StoredEvent> load(@NonNull Identifier<?> aggregateId, long fromVersion);
}
