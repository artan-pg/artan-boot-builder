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

package ir.artanpg.boot.infrastructure.adapter.driven.event;

import ir.artanpg.boot.application.port.driven.event.DomainEventDescriptor;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.event.DomainEventException;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Default immutable implementation of {@link DomainEventDescriptor}.
 *
 * <p>All fields are final and validated at construction time. Instances are
 * safe to share across threads and to use as keys in hash-based collections
 * because equality is based on the four fields.
 *
 * @author Mohammad Yazdian
 * @see DomainEventDescriptor
 * @since 0.1.0
 */
public class DefaultDomainEventDescriptor implements DomainEventDescriptor {

    /**
     * Event type coordinates this descriptor is registered under.
     */
    private final EventType eventType;

    /**
     * Concrete event class that realizes the event type.
     */
    private final Class<? extends DomainEvent<?, ?>> domainEvent;

    /**
     * Class of the payload carried by the event type.
     */
    private final Class<?> payloadType;

    /**
     * Class of the aggregate identifier for the event type.
     */
    private final Class<? extends Identifier<?>> identifierType;

    /**
     * Constructs a descriptor from already validated values.
     *
     * <p>The constructor is private and all values are expected to be
     * non-null. Use {@link #of(EventType, Class, Class, Class)} for
     * the public entry point.
     *
     * @param eventType      the event type coordinates
     * @param domainEvent    the concrete event class
     * @param payloadType    the payload class
     * @param identifierType the aggregate identifier class
     */
    private DefaultDomainEventDescriptor(EventType eventType,
                                         Class<? extends DomainEvent<?, ?>> domainEvent,
                                         Class<?> payloadType,
                                         Class<? extends Identifier<?>> identifierType) {
        this.eventType = eventType;
        this.domainEvent = domainEvent;
        this.payloadType = payloadType;
        this.identifierType = identifierType;
    }

    /**
     * Creates a new descriptor after validating every argument.
     *
     * @param eventType      the event type coordinates
     * @param domainEvent    the concrete event class
     * @param payloadType    the payload class
     * @param identifierType the aggregate identifier class
     * @return a validated immutable descriptor
     * @throws DomainEventException if any argument is {@code null}
     */
    public static DefaultDomainEventDescriptor of(@NonNull EventType eventType,
                                                  @NonNull Class<? extends DomainEvent<?, ?>> domainEvent,
                                                  @NonNull Class<?> payloadType,
                                                  @NonNull Class<? extends Identifier<?>> identifierType) {

        if (eventType == null) throw new DomainEventException("The eventType cannot be null");
        if (domainEvent == null) throw new DomainEventException("The domainEvent class cannot be null");
        if (payloadType == null) throw new DomainEventException("The payloadType cannot be null");
        if (identifierType == null) throw new DomainEventException("The identifierType cannot be null");

        return new DefaultDomainEventDescriptor(eventType, domainEvent, payloadType, identifierType);
    }

    @Override
    @NonNull
    public EventType eventType() {
        return eventType;
    }

    @Override
    @NonNull
    public Class<? extends DomainEvent<?, ?>> domainEvent() {
        return domainEvent;
    }

    @Override
    @NonNull
    public Class<?> payloadType() {
        return payloadType;
    }

    @Override
    @NonNull
    public Class<? extends Identifier<?>> identifierType() {
        return identifierType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultDomainEventDescriptor that)) return false;

        return Objects.equals(eventType, that.eventType)
                && Objects.equals(domainEvent, that.domainEvent)
                && Objects.equals(payloadType, that.payloadType)
                && Objects.equals(identifierType, that.identifierType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, domainEvent, payloadType, identifierType);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DefaultDomainEventDescriptor.class.getSimpleName() + "[", "]")
                .add("eventType=" + eventType)
                .add("domainEvent=" + domainEvent.getName())
                .add("payloadType=" + payloadType.getName())
                .add("identifierType=" + identifierType.getName())
                .toString();
    }
}
