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
import ir.artanpg.boot.application.port.driven.event.DomainEventRegistry;
import ir.artanpg.boot.application.port.driven.event.DomainEventResolver;
import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.event.DomainEventException;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default in-memory implementation of {@link DomainEventRegistry}.
 *
 * <p>The registry keeps a thread-safe mapping from {@link EventType} to
 * {@link DomainEventDescriptor}. Because {@code EventType} already defines
 * a correct {@code equals}/{@code hashCode} over its three coordinates, it
 * is used directly as the map key.
 *
 * <p>On {@link #register(EventType, Class)}, the payload and identifier
 * structural types are extracted once from the event class hierarchy
 * (through the generic signature of {@link AbstractDomainEvent}). This moves
 * the reflection cost out of the hot path (deserialization) and into the
 * cold path (startup / plugin install).
 *
 * <p>The registry intentionally has no eviction policy. An entry remains
 * resolvable indefinitely. When plugin uninstallation is required in the
 * future, an extended interface will provide {@code unregister} capabilities.
 *
 * @author Mohammad Yazdian
 * @see DomainEventRegistry
 * @since 0.1.0
 */
public class DefaultDomainEventRegistry implements DomainEventRegistry, DomainEventResolver {

    private static final String DESCRIPTOR_NULL_MSG = "The descriptor cannot be null";
    private static final String EVENT_TYPE_NULL_MSG = "The eventType cannot be null";

    /**
     * Index of the identifier type argument in {@code AbstractDomainEvent<I, P>}.
     */
    private static final int IDENTIFIER_TYPE_ARGUMENT_INDEX = 0;

    /**
     * Index of the payload type argument in {@code AbstractDomainEvent<I, P>}.
     */
    private static final int PAYLOAD_TYPE_ARGUMENT_INDEX = 1;

    /**
     * Thread-safe mapping from event type coordinates to the registered descriptor.
     */
    private final ConcurrentMap<EventType, DomainEventDescriptor> registry;

    /**
     * Constructs a registry with default settings.
     */
    public DefaultDomainEventRegistry() {
        this.registry = new ConcurrentHashMap<>();
    }

    /**
     * Constructs a registry with the given initial capacity.
     *
     * @param initialCapacity the initial capacity of the backing map
     */
    public DefaultDomainEventRegistry(int initialCapacity) {
        if (initialCapacity < 0) throw new DomainEventException("The initialCapacity cannot be negative");
        this.registry = new ConcurrentHashMap<>(initialCapacity);
    }

    @Override
    public void register(@NonNull DomainEventDescriptor descriptor) throws DomainEventException {
        if (descriptor == null) throw new DomainEventException(DESCRIPTOR_NULL_MSG);

        EventType eventType = descriptor.eventType();
        if (eventType == null) throw new DomainEventException(EVENT_TYPE_NULL_MSG);

        Class<? extends DomainEvent<?, ?>> eventClass = descriptor.domainEvent();
        eventClassValidation(eventClass);

        DomainEventDescriptor existing = registry.putIfAbsent(eventType, descriptor);
        if (existing != null && !existing.domainEvent().equals(eventClass)) {
            throw new DomainEventException(String.format(
                    "The event type [%s] is already registered to class [%s] and cannot be re-registered to [%s]",
                    eventType, existing.domainEvent().getName(), eventClass.getName()));
        }
    }

    @Override
    public void register(@NonNull EventType eventType,
                         @NonNull Class<? extends DomainEvent<?, ?>> eventClass) throws DomainEventException {
        if (eventType == null) throw new DomainEventException(EVENT_TYPE_NULL_MSG);

        eventClassValidation(eventClass);

        Class<?> payloadType = extractPayloadType(eventClass);
        Class<? extends Identifier<?>> identifierType = extractIdentifierType(eventClass);

        DomainEventDescriptor descriptor =
                DefaultDomainEventDescriptor.of(eventType, eventClass, payloadType, identifierType);

        register(descriptor);
    }

    @Override
    public Optional<DomainEventDescriptor> findDescriptor(@NonNull EventType eventType) {
        if (eventType == null) throw new DomainEventException(EVENT_TYPE_NULL_MSG);
        return Optional.ofNullable(registry.get(eventType));
    }

    @Override
    public boolean isRegistered(@NonNull EventType eventType) {
        if (eventType == null) throw new DomainEventException(EVENT_TYPE_NULL_MSG);
        return registry.containsKey(eventType);
    }

    @Override
    public Set<EventType> registeredTypes() {
        return Set.copyOf(registry.keySet());
    }

    @Override
    @NonNull
    public DomainEventDescriptor resolve(@NonNull EventType eventType) throws DomainEventException {
        return findDescriptor(eventType)
                .orElseThrow(() -> new DomainEventException(String.format(
                        "No domain event class registered for event type [%s]. Ensure the event type is registered" +
                                " before deserialization.",
                        eventType)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultDomainEventRegistry that)) return false;

        return Objects.equals(registry, that.registry);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(registry);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DefaultDomainEventRegistry.class.getSimpleName() + "[", "]")
                .add("registeredTypes=" + registry.size())
                .toString();
    }

    // -------------------------------------------------------------------------
    // Structural type extraction
    // -------------------------------------------------------------------------

    private void eventClassValidation(Class<? extends DomainEvent<?, ?>> eventClass) {
        if (eventClass == null) throw new DomainEventException("The eventClass cannot be null");
        if (eventClass.isInterface() || Modifier.isAbstract(eventClass.getModifiers())) {
            throw new DomainEventException(
                    "Cannot register an abstract class or interface as a domain event: " + eventClass.getName());
        }
    }

    /**
     * Extracts the payload class from the generic signature of
     * {@link AbstractDomainEvent} on the given event class hierarchy.
     *
     * @param eventClass the concrete event class
     * @return the payload class
     * @throws DomainEventException if the payload type cannot be resolved
     */
    private static Class<?> extractPayloadType(Class<?> eventClass) {
        Type[] args = resolveTypeArguments(eventClass);
        return typeToClass(args[PAYLOAD_TYPE_ARGUMENT_INDEX]);
    }

    /**
     * Extracts the aggregate identifier class from the generic signature of
     * {@link AbstractDomainEvent} on the given event class hierarchy.
     *
     * @param eventClass the concrete event class
     * @return the identifier class
     * @throws DomainEventException if the identifier type cannot be resolved
     *                              or does not implement {@link Identifier}
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Identifier<?>> extractIdentifierType(Class<?> eventClass) {
        Type[] args = resolveTypeArguments(eventClass);
        Class<?> identifierClass = typeToClass(args[IDENTIFIER_TYPE_ARGUMENT_INDEX]);

        return (Class<? extends Identifier<?>>) identifierClass;
    }

    /**
     * Finds the first parameterized super-type in the class hierarchy whose
     * raw type is a subtype of {@link AbstractDomainEvent}, and returns its
     * actual type arguments.
     *
     * @param eventClass the concrete event class to inspect
     * @return the type arguments array
     * @throws DomainEventException if no such parameterized type is found,
     *                              or the type has fewer than two arguments
     */
    private static Type[] resolveTypeArguments(Class<?> eventClass) {
        ParameterizedType parameterized = findAbstractDomainEventParameterizedType(eventClass);
        return parameterized.getActualTypeArguments();
    }

    /**
     * Walks the class hierarchy looking for the first parameterized
     * super-type whose raw type is a subtype of {@link AbstractDomainEvent}.
     *
     * @param eventClass the starting class
     * @return the matching parameterized type
     * @throws DomainEventException if no such type is found
     */
    private static ParameterizedType findAbstractDomainEventParameterizedType(Class<?> eventClass) {
        Class<?> current = eventClass;

        while (current != Object.class) {
            Type genericSuper = current.getGenericSuperclass();

            if (genericSuper instanceof ParameterizedType parameterized
                    && AbstractDomainEvent.class.isAssignableFrom((Class<?>) parameterized.getRawType())) {
                return parameterized;
            }

            current = current.getSuperclass();
        }

        throw new DomainEventException(
                "Cannot resolve AbstractDomainEvent generic parameters from event class: "
                        + eventClass.getName() + ". The class must extend AbstractDomainEvent<I, P>.");
    }

    /**
     * Converts a reflective {@link Type} to a concrete class.
     *
     * @param type the type to convert
     * @return the concrete class
     * @throws DomainEventException if the type is not a class or a
     *                              parameterized type with a class raw type
     */
    private static Class<?> typeToClass(Type type) {
        if (type instanceof Class<?> clazz) return clazz;
        if (type instanceof ParameterizedType parameterized) return (Class<?>) parameterized.getRawType();

        throw new DomainEventException(
                "Unsupported type argument encountered during event class inspection: " + type.getTypeName());
    }
}
