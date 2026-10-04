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

import java.util.Objects;

/**
 * Represents a type of domain event, providing a human-readable name.
 *
 * <p>This interface is used to categorize domain events and to reference
 * event types across the system. Two {@code EventType}s are equal if and only
 * if their {@link #getName() names} are equal, regardless of how the
 * instances were created (via {@link #valueOf(String)}, {@link #named(String)}
 * or a custom implementation). This guarantees that comparisons such as
 * {@link DomainEvent#isOfType(EventType)} never depend on object identity.
 *
 * <h2>Creation</h2>
 * <ul>
 *   <li>{@link #valueOf(String)} — canonical, shared-cache lookup keyed by
 *       name; the recommended factory.</li>
 *   <li>{@link #named(String)} — a lightweight named value holder without any
 *       caching; equality still holds by name.</li>
 *   <li>Custom implementations — allowed; they must override
 *       {@link #equals(Object)} / {@link #hashCode()} comparing
 *       {@code getName()}, exactly like {@link NamedEventType} does.</li>
 * </ul>
 *
 * <h2>Registration</h2>
 * In production systems that deserialize events from external channels,
 * consider registering known types with {@link EventTypeRegistry} and enabling
 * strict mode so unknown names fail fast instead of silently producing new
 * types.
 *
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @see EventTypeRegistry
 * @since 0.1.0
 */
public interface EventType {

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
     * @return the event type name; never {@code null}
     */
    String getName();

    /**
     * Creates or retrieves a cached {@code EventType} instance from the given name.
     *
     * <p>The instance is interned per name in a bounded cache managed by
     * {@link EventTypeCache}; interning is a memory optimization only — two
     * non-interned instances with the same name are still {@code equals}.
     *
     * @param name the name of the event type; must not be {@code null} or blank
     * @return a cached {@code EventType} instance for the given name
     * @throws DomainException if name is {@code null}, {@code blank}, or
     *                         malformed according to
     *                         {@link EventTypeRegistry#NAME_PATTERN}
     */
    static EventType valueOf(String name) {
        return EventTypeCache.CACHE.getOrCreate(name);
    }

    /**
     * Creates a non-cached {@code EventType} that derives its name from the
     * given string.
     *
     * <p>Unlike {@link #valueOf(String)}, no interning takes place; use this
     * factory for one-off or programmatic type references. Equality with other
     * instances of the same name is unaffected.
     *
     * @param name the event type name; must not be {@code null} or blank
     * @return a new {@code NamedEventType} instance
     * @throws DomainException if name is {@code null}, {@code blank}, or
     *                         malformed according to
     *                         {@link EventTypeRegistry#NAME_PATTERN}
     */
    static EventType named(String name) {
        return new NamedEventType(name);
    }

    /**
     * Validates an event type name against the shared grammar.
     *
     * @param name the candidate name
     * @throws DomainException if the name is {@code null}, blank or malformed
     */
    private static void validate(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("The name cannot be null or blank");
        }
        if (!name.matches(EventTypeRegistry.NAME_PATTERN)) {
            throw new DomainException(
                    "The name '" + name + "' is not a valid EventType name; expected pattern "
                            + EventTypeRegistry.NAME_PATTERN);
        }
    }

    /**
     * Immutable, name-based {@code EventType} implementation used by the
     * factories above.
     *
     * <p>Equality and hashing are defined solely by {@link #getName()},
     * symmetrically and transitively across all {@code NamedEventType}
     * instances. The contract relies on {@code getName()} never returning
     * {@code null}.
     *
     * @param name the event type name; guaranteed non-null and well-formed
     */
    record NamedEventType(String name) implements EventType {

        /**
         * Canonical constructor; validates the name.
         *
         * @param name the event type name
         * @throws DomainException if the name is invalid
         */
        public NamedEventType {
            validate(name);
        }

        @Override
        public String getName() {
            return name;
        }

        /**
         * Indicates whether some other object is "equal to" this event type.
         *
         * @param o the reference object to compare with
         * @return {@code true} if {@code o} is an {@code EventType} with the same name
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EventType that)) return false;
            return Objects.equals(this.getName(), that.getName());
        }

        /**
         * Returns a hash code consistent with the name-based {@link #equals(Object)}.
         *
         * @return the hash code of the event type name
         */
        @Override
        public int hashCode() {
            return Objects.hashCode(getName());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Holder class for the bounded, thread-safe {@code EventType} cache.
     *
     * <p>The cache interns at most {@value EventTypeCache#MAX_CACHED_TYPES}
     * distinct names (LRU eviction on overflow), so unbounded input can never
     * exhaust heap through interning. Interning is purely an optimization:
     * correctness of equality never depends on getting the same instance back.
     *
     * <p>The lazy initialization pattern (Holder pattern) ensures the cache is
     * only created when first accessed.
     */
    final class EventTypeCache {

        static final int MAX_CACHED_TYPES = 4096;

        /**
         * Accessor used by the rest of this interface. Package-private field
         * access keeps the map itself hidden from outside callers.
         */
        static final EventTypeCache CACHE = new EventTypeCache();

        /**
         * Access-order LRU map guarded by the methods below. Reads vastly
         * outnumber writes in practice, and the small locked footprint is
         * acceptable compared to the previous unbounded growth risk.
         */
        private final java.util.LinkedHashMap<String, EventType> lru =
                new java.util.LinkedHashMap<>(256, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(java.util.Map.Entry<String, EventType> eldest) {
                        return size() > MAX_CACHED_TYPES;
                    }
                };

        private EventTypeCache() {
            // holder singleton
        }

        /**
         * Returns the interned {@code EventType} for the given name, creating
         * and caching it if absent.
         *
         * @param name the event type name
         * @return the cached instance
         * @throws DomainException if the name is invalid
         */
        synchronized EventType getOrCreate(String name) {
            EventType existing = lru.get(name);
            if (existing != null) return existing;
            EventType created = new NamedEventType(name);
            lru.put(name, created);
            return created;
        }

        /**
         * Removes all entries from the cache. Intended for test isolation.
         */
        synchronized void clearCache() {
            lru.clear();
        }
    }
}
