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

package ir.artanpg.boot.domain.model;

import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.StoredEvent;

import java.util.List;

/**
 * Marker / contract for aggregates that support Event Sourcing.
 *
 * <p>Implementations rehydrate state by replaying {@link StoredEvent}s and
 * track a monotonic {@link #getVersion()} for optimistic concurrency.
 *
 * @param <I> the identifier type
 * @author Mohammad Yazdian
 * @see AbstractEventSourcedAggregateRoot
 * @since 0.1.0
 */
public interface EventSourcedAggregateRoot<I extends Identifier<?>> extends AggregateRoot<I> {

    /**
     * Returns the current version of this aggregate stream.
     *
     * <p>Version {@code 0} means no events have been applied yet (new aggregate).
     *
     * @return the stream version
     */
    long getVersion();

    /**
     * Rehydrates this aggregate by replaying the given stored events in order.
     *
     * <p>After loading, {@link #getVersion()} reflects the last applied event
     * version and uncommitted events remain empty.
     *
     * @param history the ordered history of stored events
     */
    void loadFromHistory(List<StoredEvent> history);
}
