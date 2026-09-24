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

package ir.artanpg.boot.domain.event;

import ir.artanpg.boot.domain.exception.DomainException;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a type of domain event, providing a human-readable name.
 *
 * <p>This functional interface is used to categorize domain events and provide
 * a type-safe way to reference event types across the system.
 *
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @since 0.1.0
 */
@FunctionalInterface
public interface EventType extends Serializable {

    /**
     * Returns the string representation of this event type.
     *
     * <p>The returned name should be:
     * <ul>
     *   <li>Unique across the domain</li>
     *   <li>Stable and not subject to change</li>
     *   <li>Descriptive and human-readable</li>
     *   <li>Following a consistent naming convention</li>
     * </ul>
     *
     * @return the event type name
     */
    String getName();

    /**
     * Creates or retrieves a cached {@code EventType} instance from the given name.
     *
     * @param name the name of the event type; must not be {@code null} or blank
     * @return a cached or new {@code EventType} instance
     * @throws DomainException if name is {@code null} or {@code blank}
     */
    static EventType valueOf(@NonNull String name) {
        if (name == null || name.isBlank()) throw new DomainException("The name cannot be null or empty");
        return EventTypeCache.CACHE.computeIfAbsent(name, n -> () -> n);
    }

    /**
     * Holder class for the EventType cache.
     *
     * <p>This class exists to encapsulate the mutable cache map and prevent it
     * from being exposed as a public static field, which would violate
     * encapsulation principles and trigger static analysis warnings.
     *
     * <p>The lazy initialization pattern (Holder pattern) ensures the cache is
     * only created when first accessed.
     */
    final class EventTypeCache {

        private static final Map<String, EventType> CACHE = new ConcurrentHashMap<>();

        /**
         * Private constructor to prevent instantiation.
         *
         * @throws UnsupportedOperationException if called via reflection (implicitly)
         */
        private EventTypeCache() {
            throw new UnsupportedOperationException();
        }
    }
}
