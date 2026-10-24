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

/**
 * Strategy for mapping an {@link EventType} to the concrete
 * {@link DomainEvent} class that represents it.
 *
 * <p>A resolver decouples the runtime event type metadata carried on
 * the wire from the Java class that models the event. Infrastructure
 * components such as outbox publishers and message consumers consult
 * a resolver to obtain the target class before invoking a
 * deserializer.
 *
 * <p>Implementations are expected to be stateless and thread-safe.
 * The resolver is commonly backed by a static registry or a
 * classpath scan and may cache resolved classes for performance.
 *
 * @author Mohammad Yazdian
 * @see EventType
 * @see DomainEvent
 * @since 0.1.0
 */
@FunctionalInterface
public interface DomainEventResolver {

    /**
     * Resolves the descriptor for the given event type.
     *
     * @param eventType the event type whose descriptor should be resolved
     * @return the resolved descriptor containing the event class and structural types
     * @throws DomainEventException if no descriptor can be found for the supplied event type
     */
    @NonNull
    DomainEventDescriptor resolve(@NonNull EventType eventType) throws DomainEventException;
}
