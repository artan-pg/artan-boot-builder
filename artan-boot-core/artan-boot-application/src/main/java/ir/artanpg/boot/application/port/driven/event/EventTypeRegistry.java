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

import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Driven port that interns and resolves {@link EventType} instances.
 *
 * <p>The registry guarantees that within the scope of its backing cache,
 * two resolutions with identical coordinates ({@code name},
 * {@code category}, {@code version}) yield a value-equal — and for
 * in-process caches, typically reference-equal — instance.
 *
 * @author Mohammad Yazdian
 * @see EventType
 * @see EventTypeCacheStore
 * @since 0.1.0
 */
public interface EventTypeRegistry {

    /**
     * Returns the canonical event type for the given name only.
     *
     * @param name mandatory name
     * @return the interned event type
     * @throws DomainEventException if validation fails
     */
    default EventType get(@NonNull String name) {
        return get(name, null, null);
    }

    /**
     * Returns the canonical event type for name and category.
     *
     * @param name     mandatory name
     * @param category optional category
     * @return the interned event type
     * @throws DomainEventException if validation fails
     */
    default EventType get(@NonNull String name, @Nullable String category) {
        return get(name, category, null);
    }

    /**
     * Returns the canonical event type for all three coordinates.
     *
     * <p>Coordinates are validated through {@link EventType#of}; the
     * resulting value is then interned via the backing cache store.
     *
     * @param name     mandatory name
     * @param category optional category
     * @param version  optional version
     * @return the interned event type
     * @throws DomainEventException if validation fails
     */
    EventType get(@NonNull String name, @Nullable String category, @Nullable String version);

    /**
     * Interns an already-built event type.
     *
     * <p>If an entry for the same coordinates already exists in the
     * cache, that entry is returned; otherwise {@code type} is stored
     * and returned.
     *
     * @param type the event type to intern
     * @return the canonical cached instance
     * @throws DomainEventException if type is {@code null}
     */
    EventType intern(@NonNull EventType type);

    /**
     * Checks whether the registry currently holds an entry for the
     * given coordinates.
     *
     * @param name     mandatory name
     * @param category optional category
     * @param version  optional version
     * @return {@code true} if present in the cache, {@code false} otherwise
     */
    boolean contains(@NonNull String name, @Nullable String category, @Nullable String version);

    /**
     * Removes the cache entry for the given coordinates, if present.
     *
     * <p>Intended for test isolation and operational eviction. No-op
     * when the key is absent.
     *
     * @param name     mandatory name
     * @param category optional category
     * @param version  optional version
     */
    void evict(@NonNull String name, @Nullable String category, @Nullable String version);

    /**
     * Removes every entry from the backing cache.
     *
     * <p>Intended primarily for tests. Use with care against shared
     * distributed stores.
     */
    void clear();
}
