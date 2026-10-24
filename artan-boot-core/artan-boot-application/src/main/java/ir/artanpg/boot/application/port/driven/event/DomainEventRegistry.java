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
import ir.artanpg.boot.domain.exception.event.DomainEventException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

/**
 * Central catalog that maintains the authoritative mapping between
 * {@link EventType} coordinates and {@link DomainEventDescriptor}s.
 *
 * <p>The registry is the single source of truth for answering two questions:
 * <ul>
 *   <li><strong>Discovery:</strong> which event types are known to the system?</li>
 *   <li><strong>Authorization:</strong> is an incoming event type trusted?</li>
 * </ul>
 *
 * <p>Both the deserialization pipeline and the publishing pipeline consult
 * this registry. Deserialization resolves the concrete target class and its
 * structural types from the wire {@code EventType}; publishing may reject
 * any type that is not registered.
 *
 * <h2>Threading:</h2> implementations must be safe for concurrent use.
 *
 * <h2>Lifecycle:</h2> entries are registered at startup or on plugin
 * installation, and removed only on plugin uninstallation or in tests. There
 * is deliberately no time- or size-based eviction.
 *
 * @author Mohammad Yazdian
 * @see EventType
 * @see DomainEvent
 * @see DomainEventDescriptor
 * @since 0.2.0
 */
public interface DomainEventRegistry {

    /**
     * Registers the given descriptor in this registry.
     *
     * @param descriptor the descriptor to register
     * @throws DomainEventException if the descriptor is {@code null} or the registration is rejected
     */
    void register(@NonNull DomainEventDescriptor descriptor) throws DomainEventException;

    /**
     * Registers a concrete domain event class under the given event type.
     *
     * <p>The registry extracts the payload and identifier types from the
     * generic signature of {@code DomainEvent<I, P>} on the given class,
     * then stores a complete {@link DomainEventDescriptor}.
     *
     * <p>Registering the same {@code eventType} with the same class is
     * idempotent. Registering the same {@code eventType} with a different
     * class is a configuration error and must fail fast.
     *
     * @param eventType  the event type coordinates
     * @param eventClass the concrete event class realizing that type
     * @throws DomainEventException if either argument is {@code null}, the class is abstract/an interface,
     *                              the type is already registered to a different class, or the structural
     *                              types cannot be extracted from the class hierarchy
     */
    void register(@NonNull EventType eventType,
                  @NonNull Class<? extends DomainEvent<?, ?>> eventClass) throws DomainEventException;

    /**
     * Resolves the descriptor registered for the given event type.
     *
     * @param eventType the event type to look up
     * @return the registered descriptor, or {@link Optional#empty()} when the type is unknown
     * @throws DomainEventException if eventType is {@code null}
     */
    Optional<DomainEventDescriptor> findDescriptor(@NonNull EventType eventType);

    /**
     * Resolves the descriptor by raw coordinates.
     *
     * @param name     mandatory event name
     * @param category optional category
     * @param version  optional version
     * @return the registered descriptor, or {@link Optional#empty()} when unknown
     * @throws DomainEventException if the coordinates fail validation
     */
    default Optional<DomainEventDescriptor> findDescriptor(@NonNull String name,
                                                           @Nullable String category,
                                                           @Nullable String version) {
        return findDescriptor(EventType.of(name, category, version));
    }

    /**
     * Resolves only the concrete event class registered for the given type.
     *
     * @param eventType the event type to look up
     * @return the registered class, or {@link Optional#empty()} when unknown
     * @throws DomainEventException if eventType is {@code null}
     */
    default Optional<Class<? extends DomainEvent<?, ?>>> findClass(@NonNull EventType eventType) {
        return findDescriptor(eventType).map(DomainEventDescriptor::domainEvent);
    }

    /**
     * Checks whether the given event type is registered (i.e. authorized).
     *
     * @param eventType the event type to check
     * @return {@code true} when a descriptor is registered for this type, {@code false} otherwise
     * @throws DomainEventException if eventType is {@code null}
     */
    boolean isRegistered(@NonNull EventType eventType);

    /**
     * Checks whether the given coordinates are registered.
     *
     * @param name     mandatory event name
     * @param category optional category
     * @param version  optional version
     * @return {@code true} when a descriptor is registered for these coordinates, {@code false} otherwise
     * @throws DomainEventException if the coordinates fail validation
     */
    default boolean isRegistered(@NonNull String name, @Nullable String category, @Nullable String version) {
        return isRegistered(EventType.of(name, category, version));
    }

    /**
     * Returns an immutable snapshot of every registered event type.
     *
     * @return the set of registered event types
     */
    Set<EventType> registeredTypes();
}
