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

import ir.artanpg.boot.application.port.driven.event.EventTypeCacheStore;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Zero-dependency in-memory {@link EventTypeCacheStore} based on
 * {@link ConcurrentHashMap}.
 *
 * <p>Useful for unit tests and environments that do not want a Caffeine
 * dependency. Unlike Caffeine this store has no built-in eviction
 * policy; callers must bound growth themselves if needed.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public final class ConcurrentMapEventTypeCacheStore implements EventTypeCacheStore {

    /**
     * Shared error message used when a key is {@code null} or {@code blank}.
     */
    private static final String KEY_EXCEPTION_MESSAGE = "The key cannot be null or blank";

    /**
     * Backing concurrent map used for storage.
     */
    private final ConcurrentMap<String, EventType> cache;

    /**
     * Creates an empty store backed by a new {@link ConcurrentHashMap}.
     */
    public ConcurrentMapEventTypeCacheStore() {
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Creates a store around the given concurrent map.
     *
     * <p>Use this constructor to share a map with other components
     * or to supply a pre-populated map.
     *
     * @param map the backing map
     * @throws DomainEventException if {@code map} is {@code null}
     */
    public ConcurrentMapEventTypeCacheStore(@NonNull ConcurrentMap<String, EventType> map) {
        if (map == null) throw new DomainEventException("The map cannot be null");
        this.cache = map;
    }

    @Override
    @Nullable
    public EventType getIfPresent(@NonNull String key) {
        if (key == null || key.isBlank()) throw new DomainEventException(KEY_EXCEPTION_MESSAGE);
        return cache.get(key);
    }

    @Override
    @NonNull
    public EventType putIfAbsent(@NonNull String key, @NonNull EventType value) {
        if (key == null || key.isBlank()) throw new DomainEventException(KEY_EXCEPTION_MESSAGE);
        if (value == null) throw new DomainEventException("The value cannot be null");

        EventType previous = cache.putIfAbsent(key, value);
        return previous != null ? previous : value;
    }

    @Override
    public void invalidate(@NonNull String key) {
        if (key == null || key.isBlank()) throw new DomainEventException(KEY_EXCEPTION_MESSAGE);
        cache.remove(key);
    }

    @Override
    public void invalidateAll() {
        cache.clear();
    }
}
