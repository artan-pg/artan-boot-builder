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
import ir.artanpg.boot.application.port.driven.event.EventTypeRegistry;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Default {@link EventTypeRegistry} implementation that delegates storage to an
 * {@link EventTypeCacheStore}.
 *
 * <p>Validation always goes through {@link EventType#of}; interning is
 * performed with {@link EventTypeCacheStore#putIfAbsent}. The concrete
 * cache technology is therefore swappable without touching this class.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public final class DefaultEventTypeRegistry implements EventTypeRegistry {

    /**
     * Backing cache store used for interning and lookup.
     */
    private final EventTypeCacheStore cacheStore;

    /**
     * Creates a registry backed by the given cache store.
     *
     * @param cacheStore the cache adapter; must not be {@code null}
     * @throws DomainEventException if cacheStore is {@code null}
     */
    public DefaultEventTypeRegistry(@NonNull EventTypeCacheStore cacheStore) {
        if (cacheStore == null) throw new DomainEventException("The EventTypeCacheStore cannot be null");
        this.cacheStore = cacheStore;
    }

    @Override
    public EventType get(@NonNull String name, @Nullable String category, @Nullable String version) {
        EventType candidate = EventType.of(name, category, version);
        return intern(candidate);
    }

    @Override
    public EventType intern(@NonNull EventType type) {
        if (type == null) throw new DomainEventException("The event type cannot be null");

        String key = type.toCacheKey();
        EventType existing = cacheStore.getIfPresent(key);

        return (existing != null) ? existing : cacheStore.putIfAbsent(key, type);
    }

    @Override
    public boolean contains(@NonNull String name, @Nullable String category, @Nullable String version) {
        EventType probe = EventType.of(name, category, version);
        return cacheStore.getIfPresent(probe.toCacheKey()) != null;
    }

    @Override
    public void evict(@NonNull String name, @Nullable String category, @Nullable String version) {
        EventType probe = EventType.of(name, category, version);
        cacheStore.invalidate(probe.toCacheKey());
    }

    @Override
    public void clear() {
        cacheStore.invalidateAll();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultEventTypeRegistry that)) return false;
        return Objects.equals(cacheStore, that.cacheStore);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cacheStore);
    }
}
