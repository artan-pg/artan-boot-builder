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

import java.io.Serializable;
import java.util.List;

/**
 * The root entity of an aggregate that controls and enforces business rules
 * and invariants for all entities within the aggregate boundary.
 *
 * <p>An aggregate is a cluster of domain objects that can be treated as a
 * single unit. The aggregate root is the only member of the aggregate that
 * external objects can hold references to.
 *
 * <h2>Responsibilities:</h2>
 * <ul>
 *   <li>Enforces business rules and invariants</li>
 *   <li>Controls access to aggregate members</li>
 *   <li>Manages the aggregate's lifecycle</li>
 *   <li>Collects and publishes domain events</li>
 *   <li>Provides transaction boundaries</li>
 * </ul>
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li>Immutable identity (id changes only for new aggregates)</li>
 *   <li>Encapsulates aggregate members</li>
 *   <li>Exposes behavior through business methods</li>
 *   <li>Validates state before changes</li>
 * </ul>
 *
 * @param <I> The type of identifier used by this aggregate root
 * @author Mohammad Yazdian
 * @see Identifier
 * @since 0.1.0
 */
public interface AggregateRoot<I extends Identifier<?>> extends Serializable {

    /**
     * Returns the unique identifier of this aggregate root.
     *
     * <p>The identifier is the primary key that distinguishes this aggregate
     * from all others in the system. It is immutable and never changes during
     * the aggregate's lifecycle.
     *
     * @return the aggregate's unique identifier
     */
    I getId();

    /**
     * Returns a read-only view of the domain events collected so far,
     * <em>without</em> removing them from the aggregate.
     *
     * <p>This method follows the Command-Query Separation (CQS) principle:
     * it is a pure query with no side effects. Useful for inspection,
     * validation, and testing while the events remain registered.
     *
     * @return an unmodifiable view of the currently registered domain events
     */
    List<DomainEvent<?, ?>> peekDomainEvents();

    /**
     * Removes all domain events currently registered on this aggregate.
     *
     * <p>This command performs no return value; it only resets the internal
     * event list. Typically used when the collected events must be discarded
     * (for example, after a failed transaction that will not publish them).
     */
    void clearDomainEvents();

    /**
     * Atomically returns and drains the domain events collected during the
     * current transaction.
     *
     * <p>This method returns an immutable snapshot of all domain events
     * accumulated so far and then clears the internal event list, so
     * subsequent calls will return an empty list unless new events are added.
     * It is the standard way to hand collected events over to an event
     * publisher exactly once.
     *
     * @return an immutable list of the collected domain events
     */
    List<DomainEvent<?, ?>> pullDomainEvents();
}
