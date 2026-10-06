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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ir.artanpg.boot.application.port.driven.event.EventTypeCacheStore;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * In-process {@link EventTypeCacheStore} backed by {@code Caffeine}.
 *
 * <p>Provides true interning inside a single JVM: repeated resolutions of the
 * same coordinates return the same instance. Capacity and expiration are
 * configurable through the builder helpers.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public final class CaffeineEventTypeCacheStore implements EventTypeCacheStore {

    /**
     * Shared error message used when a key is {@code null} or {@code blank}.
     */
    private static final String KEY_EXCEPTION_MESSAGE = "The key cannot be null or blank";

    /**
     * Shared error message used when the maximum size is not positive.
     */
    private static final String MAXIMUM_SIZE_EXCEPTION_MESSAGE = "The maximumSize must be positive";

    /**
     * Default maximum number of cached entries used by the no-arg constructor.
     */
    private static final long DEFAULT_MAXIMUM_SIZE = 4096L;

    /**
     * Backing Caffeine cache instance.
     */
    private final Cache<String, EventType> cache;

    /**
     * Creates a store with a default maximum size of
     * {@value #DEFAULT_MAXIMUM_SIZE} entries and no expiration.
     */
    public CaffeineEventTypeCacheStore() {
        this(Caffeine.newBuilder().maximumSize(DEFAULT_MAXIMUM_SIZE).build());
    }

    /**
     * Creates a store with an explicit maximum size and no expiration.
     *
     * @param maximumSize maximum number of cached entries
     * @throws DomainEventException if maximumSize is not positive
     */
    public CaffeineEventTypeCacheStore(long maximumSize) {
        if (maximumSize <= 0) throw new DomainEventException(MAXIMUM_SIZE_EXCEPTION_MESSAGE);
        this.cache = Caffeine.newBuilder().maximumSize(maximumSize).build();
    }

    /**
     * Creates a store with maximum size and write-based expiration.
     *
     * @param maximumSize      maximum number of cached entries
     * @param expireAfterWrite time-to-live after each write
     * @throws DomainEventException if arguments are invalid
     */
    public CaffeineEventTypeCacheStore(long maximumSize, @NonNull Duration expireAfterWrite) {
        if (maximumSize <= 0) throw new DomainEventException(MAXIMUM_SIZE_EXCEPTION_MESSAGE);
        if (expireAfterWrite == null || expireAfterWrite.isZero() || expireAfterWrite.isNegative()) {
            throw new DomainEventException("The expireAfterWrite duration cannot be null, zero, or negative");
        }
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expireAfterWrite)
                .build();
    }

    /**
     * Creates a store around a preconfigured Caffeine cache.
     *
     * @param cache the Caffeine cache
     * @throws DomainEventException if cache is {@code null}
     */
    public CaffeineEventTypeCacheStore(@NonNull Cache<String, EventType> cache) {
        if (cache == null) throw new DomainEventException("The Caffeine cache cannot be null");
        this.cache = cache;
    }

    @Override
    @Nullable
    public EventType getIfPresent(@NonNull String key) {
        if (key == null || key.isBlank()) throw new DomainEventException(KEY_EXCEPTION_MESSAGE);
        return cache.getIfPresent(key);
    }

    @Override
    public @NonNull EventType putIfAbsent(@NonNull String key, @NonNull EventType value) {
        if (key == null || key.isBlank()) throw new DomainEventException(KEY_EXCEPTION_MESSAGE);
        if (value == null) throw new DomainEventException("The value cannot be null");

        EventType previous = cache.asMap().putIfAbsent(key, value);
        return previous != null ? previous : value;
    }

    @Override
    public void invalidate(@NonNull String key) {
        if (key == null || key.isBlank()) throw new DomainEventException(KEY_EXCEPTION_MESSAGE);
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }
}
