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
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;

/**
 * An immutable descriptor that binds a {@link EventType} to the concrete
 * {@link DomainEvent} class realizing it, together with the structural type
 * information required to serialize and deserialize instances efficiently.
 *
 * <p>This descriptor is the unit of registration inside the
 * {@link DomainEventRegistry}. It is a <em>type-level</em> carrier: every
 * field describes a property that is true for <em>all</em> instances of
 * the event type, not for a particular occurrence. For per-occurrence data
 * (event id, timestamp, payload, aggregate id) see the persistence layer.
 *
 * <h2>Immutability:</h2> implementations must be immutable and safe to
 * share across threads.
 *
 * @author Mohammad Yazdian
 * @see DomainEventRegistry
 * @since 0.1.0
 */
public interface DomainEventDescriptor {

    /**
     * Returns the event type coordinates this descriptor is registered under.
     *
     * @return the event type
     */
    @NonNull
    EventType eventType();

    /**
     * Returns the concrete class that realizes this event type.
     *
     * @return the event class
     */
    @NonNull
    Class<? extends DomainEvent<?, ?>> domainEvent();

    /**
     * Returns the class of the payload carried by instances of this event
     * type.
     *
     * <p>This is the {@code P} parameter of {@code DomainEvent<I, P>}.
     * Deserializers use it to convert the wire payload directly into the
     * correct target type, avoiding reflective type resolution on every call.
     *
     * @return the payload class
     */
    @NonNull
    Class<?> payloadType();


    /**
     * Returns the class of the aggregate identifier for this event type.
     *
     * <p>This is the {@code I} parameter of {@code DomainEvent<I, P>}.
     * Deserializers use it to decode the wire aggregate id into the
     * correct target type without resolving the type reflectively on
     * every call.
     *
     * @return the identifier class
     */
    @NonNull
    Class<? extends Identifier<?>> identifierType();
}
