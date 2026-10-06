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
 * SPI for the physical cache that backs {@link EventTypeRegistry}.
 *
 * <p>Implementations adapt concrete technologies (Caffeine, Redis,
 * Hazelcast, Memcached, …) without leaking those APIs into the
 * application or domain layers.
 *
 * <p>Keys are opaque strings produced by {@link EventType#toCacheKey()}.
 * Values are immutable {@link EventType} instances.
 *
 * @author Mohammad Yazdian
 * @see EventTypeRegistry
 * @see EventType#toCacheKey()
 * @since 0.1.0
 */
public interface EventTypeCacheStore {

    /**
     * Returns the cached event type for {@code key}, or {@code null}
     * when absent.
     *
     * @param key the cache key
     * @return the cached value, or {@code null}
     * @throws DomainEventException if key is {@code null} or {@code blank}
     */
    @Nullable
    EventType getIfPresent(@NonNull String key);

    /**
     * Associates {@code value} with {@code key} if no mapping exists.
     *
     * <p>Returns the existing value when the key is already present
     * (compare-and-set semantics), otherwise stores and returns
     * {@code value}. Implementations must be thread-safe.
     *
     * @param key   the cache key
     * @param value the event type to store
     * @return the canonical value now associated with the key
     * @throws DomainEventException if key is {@code null} or {@code blank}
     * @throws DomainEventException if value is {@code null}
     */
    @NonNull
    EventType putIfAbsent(@NonNull String key, @NonNull EventType value);

    /**
     * Removes the mapping for {@code key}, if present.
     *
     * @param key the cache key
     * @throws DomainEventException if key is {@code null} or {@code blank}
     */
    void invalidate(@NonNull String key);

    /**
     * Removes all mappings from this store.
     */
    void invalidateAll();
}
