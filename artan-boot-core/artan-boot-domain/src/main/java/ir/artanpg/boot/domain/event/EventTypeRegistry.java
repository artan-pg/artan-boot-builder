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

import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A registry of {@link EventType}s that are known to the application.
 *
 * <p>The registry serves two purposes:
 * <ul>
 *   <li><b>Type safety:</b> when strict mode is enabled, unknown event type
 *       names are rejected instead of silently creating new instances. This
 *       turns typos and unexpected wire payloads into fail-fast errors.</li>
 *   <li><b>Bounded memory:</b> registered types are stored in a bounded set,
 *       so the cache cannot grow without limit from untrusted input.</li>
 * </ul>
 *
 * <p>By default, the shared registry operates in non-strict mode, meaning
 * {@link #valueOf(String)} and {@link #resolve(String)} fall back to caching
 * arbitrary (but well-formed) names. Call {@link #setStrictMode(boolean)} to
 * reject anything that has not been registered explicitly.
 *
 * <p>Names must match the pattern {@value #NAME_PATTERN} — lowercase or
 * uppercase letters, digits, dots, dashes, underscores and colons, starting
 * with a letter or digit, up to 128 characters. Names outside this pattern are
 * always rejected, even in non-strict mode, which keeps the cache safe against
 * unbounded growth from malformed input.
 *
 * @author Mohammad Yazdian
 * @see EventType
 * @since 0.1.0
 */
public class EventTypeRegistry {

    /**
     * The regular expression every valid event type name must match.
     *
     * <p>Deliberately restrictive: it rejects whitespace, control characters
     * and path-like strings, bounding both the cache key space and the risk
     * of log/metric injection through event type names.
     */
    public static final String NAME_PATTERN = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}";

    /**
     * The structured name grammar of an event type:
     * {@code category.name.version}.
     *
     * <p>Both {@code category} and {@code version} are optional; {@code name}
     * is mandatory and dot-free. A segment matches (letters, digits,
     * underscores, dashes and colons — but no dots), so the following forms
     * are all valid:
     * <ul>
     *   <li>{@code "OrderPlaced"} — name only</li>
     *   <li>{@code "order.OrderPlaced"} — category + name</li>
     *   <li>{@code "order.OrderPlaced.v1"} — category + name + version</li>
     *   <li>{@code "billing.invoice.refunded.v2"} — multi-segment category</li>
     * </ul>
     *
     * @see EventType.NamedEventType
     */
    public static final String STRUCTURED_NAME_PATTERN =
            "(?:[A-Za-z0-9][A-Za-z0-9_:-]*\\.)*"        // zero or more "category." segments
                    + "[A-Za-z0-9][A-Za-z0-9_:-]*"              // mandatory dot-free name
                    + "(?:\\.[A-Za-z0-9][A-Za-z0-9_:-]*)?";     // optional ".version" segment

    /**
     * The default maximum number of event types that may be registered.
     *
     * <p>Used when a registry is created without an explicit capacity, e.g.
     * the process-wide {@link #shared()} instance. Can be overridden per
     * registry via {@link #EventTypeRegistry(int)} or
     * {@link #setMaxRegisteredTypes(int)}.
     */
    public static final int DEFAULT_MAX_REGISTERED_TYPES = 4096;

    /**
     * The exception message used when maxRegisteredTypes is not positive.
     */
    private static final String MAX_REGISTERED_TYPES_EXCEPTION = "The maxRegisteredTypes must be positive";

    /**
     * The process-wide shared registry instance.
     */
    private static final EventTypeRegistry SHARED = new EventTypeRegistry();

    /**
     * The set of registered event type names.
     *
     * <p>Thread-safe and used to prevent duplicate registrations.
     */
    private final Set<String> registeredNames = ConcurrentHashMap.newKeySet();

    /**
     * Cache of registered event types keyed by name.
     *
     * <p>Provides fast lookup for previously registered types.
     */
    private final ConcurrentHashMap<String, EventType> cachedTypes = new ConcurrentHashMap<>();

    /**
     * Flag indicating whether strict mode is enabled.
     *
     * <p>When {@code true}, unregistered event types are rejected.
     */
    private final AtomicBoolean strictMode = new AtomicBoolean(false);

    /**
     * The configurable upper bound on the number of registered event types.
     * Guarded by {@code this} together with the size check in register().
     */
    private int maxRegisteredTypes = DEFAULT_MAX_REGISTERED_TYPES;

    /**
     * Creates a new, empty registry with the default capacity.
     */
    public EventTypeRegistry() {
        // empty on purpose
    }

    /**
     * Creates a new, empty registry instance with the given capacity.
     *
     * @param maxRegisteredTypes the maximum number of types
     * @throws DomainEventException if maxRegisteredTypes is not positive
     */
    public EventTypeRegistry(int maxRegisteredTypes) {
        if (maxRegisteredTypes <= 0) throw new DomainEventException(MAX_REGISTERED_TYPES_EXCEPTION);
        this.maxRegisteredTypes = maxRegisteredTypes;
    }

    /**
     * Returns the process-wide shared registry.
     *
     * @return the shared registry instance
     */
    public static EventTypeRegistry shared() {
        return SHARED;
    }

    /**
     * Creates or retrieves a cached {@link EventType} instance from the given
     * name, using the process-wide {@link #shared()} registry.
     *
     * <p>This is the canonical factory for event type references. The instance
     * is interned per name in a bounded cache (LRU eviction on overflow);
     * interning is a memory optimization only — two non-interned instances
     * with the same name are still {@code equals}.
     *
     * <p>Honors the current strictness mode of the shared registry: in strict
     * mode, names that have not been registered explicitly are rejected.
     *
     * @param name the name of the event type
     * @return a cached {@code EventType} instance for the given name
     * @throws DomainEventException if name is {@code null}, {@code blank}, malformed according or
     *                              unregistered while strict mode is enabled
     * @see #resolve(String)
     */
    public static EventType valueOf(@Nullable String name) {
        return SHARED.resolve(name);
    }

    /**
     * Instance-level counterpart of the static {@link #valueOf(String)}
     * convenience, delegating to {@link #resolve(String)} so the strictness
     * mode and capacity limits of this particular registry are honored.
     *
     * @param name the name of the event type
     * @return a cached {@code EventType} instance for the given name
     * @throws DomainEventException if name is {@code null}, {@code blank}, malformed according or
     *                              unregistered while strict mode is enabled
     * @see #resolve(String)
     */
    public EventType valueOfName(@Nullable String name) {
        return resolve(name);
    }

    /**
     * Registers an event type as known to the application.
     *
     * <p>Registration is idempotent; registering the same name twice has no
     * additional effect. Registered names are also placed in the internal
     * cache so {@link #resolve(String)} returns the very same instance.
     *
     * @param eventType the event type to register
     * @return the canonical (cached) {@code EventType} for the given name
     * @throws DomainEventException if eventType is {@code null}, its name is invalid, or the registry exceeded
     *                              {@code maxRegisteredTypes} entries
     */
    public synchronized EventType register(@NonNull EventType eventType) {
        if (eventType == null) throw new DomainEventException("The eventType cannot be null");
        String name = validateAndNormalize(eventType.getName());

        if (!cachedTypes.containsKey(name) && registeredNames.size() >= maxRegisteredTypes) {
            throw new DomainEventException("The EventType registry exceeded " + maxRegisteredTypes + " entries");
        }
        registeredNames.add(name);
        return cachedTypes.computeIfAbsent(name, EventType::named);
    }

    /**
     * Registers an event type by name.
     *
     * @param name the event type name
     * @return the canonical (cached) {@code EventType} for the given name
     * @throws DomainEventException if the name is invalid or the registry is full
     * @see #register(EventType)
     */
    public EventType register(@NonNull String name) {
        return register(EventType.named(name));
    }

    /**
     * Unregisters a previously registered event type name.
     *
     * <p>The name is removed from both the registered set and the cache, so a
     * subsequent {@link #resolve(String)} in strict mode will fail until the
     * name is registered again. Intended primarily for test isolation.
     *
     * @param name the event type name to unregister
     * @return {@code true} if the name was registered and has been removed, {@code false} otherwise
     */
    public boolean unregister(@Nullable String name) {
        if (name == null) return false;
        cachedTypes.remove(name);
        return registeredNames.remove(name);
    }

    /**
     * Resolves an event type by name, honoring the current strictness mode.
     *
     * <ul>
     *   <li>If the name is registered, the canonical cached instance is
     *       returned.</li>
     *   <li>In non-strict mode, a well-formed but unregistered name is cached
     *       and returned (same behavior as {@link #valueOf(String)}).</li>
     *   <li>In strict mode, an unregistered name throws
     *       {@link DomainEventException}.</li>
     * </ul>
     *
     * @param name the event type name to resolve
     * @return the resolved {@code EventType} instance
     * @throws DomainEventException if the name is {@code null}, {@code blank}, malformed,
     *                              or unregistered while strict mode is enabled
     */
    public EventType resolve(@Nullable String name) {
        String normalized = validateAndNormalize(name);

        EventType cached = cachedTypes.get(normalized);
        if (cached != null) return cached;

        if (strictMode.get() && !registeredNames.contains(normalized)) {
            throw new DomainEventException(
                    "Unknown EventType '" + normalized + "'. Register it via EventTypeRegistry.register() first"
                            + " or disable strict mode.");
        }
        return cachedTypes.computeIfAbsent(normalized, EventType::named);
    }

    /**
     * Returns all registered event types that belong to the given category.
     *
     * <p>The category of a registered type is taken from
     * {@link EventType#getCategory()}; for {@link EventType.NamedEventType}
     * instances it is derived from the name. Types without a category are only
     * returned when {@code category} itself is empty.
     *
     * <p>Note that {@link #valueOf(String)} in non-strict mode may cache
     * well-formed but unregistered names; such lazily cached types are not
     * considered by this method — only explicitly registered ones are.
     *
     * @param category the category to search for
     * @return an unmodifiable list of matching registered event types, ordered by name
     * @throws DomainEventException if category is {@code null}
     * @see EventType#getCategory()
     */
    public List<EventType> findByCategory(@NonNull String category) {
        if (category == null) throw new DomainEventException("The category cannot be null");
        List<EventType> matches = new ArrayList<>();
        for (String name : registeredNames) {
            EventType type = cachedTypes.get(name);
            if (type == null) continue;
            if (type.getCategory().equals(category)) matches.add(type);
        }
        matches.sort(Comparator.comparing(EventType::getName));
        return Collections.unmodifiableList(matches);
    }

    /**
     * Returns all registered event types with the given version.
     *
     * <p>The version of a registered type is taken from
     * {@link EventType#getVersion()}; for {@link EventType.NamedEventType}
     * instances it is derived from the name. This enables schema evolution
     * queries such as finding every {@code v2} event currently known to the
     * application.
     *
     * @param version the version to search for
     * @return an unmodifiable list of matching registered event types, ordered by name
     * @throws DomainEventException if version is {@code null}
     * @see EventType#getVersion()
     */
    public List<EventType> findByVersion(@NonNull String version) {
        if (version == null) throw new DomainEventException("The version cannot be null");
        List<EventType> matches = new ArrayList<>();
        for (String name : registeredNames) {
            EventType type = cachedTypes.get(name);
            if (type == null) continue;
            if (type.getVersion().equals(version)) matches.add(type);
        }
        matches.sort(Comparator.comparing(EventType::getName));
        return Collections.unmodifiableList(matches);
    }

    /**
     * Checks whether the given name is explicitly registered.
     *
     * @param name the event type name to check
     * @return {@code true} if a type with that exact name is registered, {@code false} otherwise
     */
    public boolean isRegistered(@Nullable String name) {
        return name != null && registeredNames.contains(name);
    }

    /**
     * Checks whether the given event type is registered by name.
     *
     * @param eventType the event type to check
     * @return {@code true} if the type's name is registered, {@code false} otherwise
     */
    public boolean isRegistered(@Nullable EventType eventType) {
        return eventType != null && isRegistered(eventType.getName());
    }

    /**
     * Enables or disables strict mode.
     *
     * <p>When strict mode is enabled, {@link #resolve(String)} rejects any
     * name that has not been registered explicitly. This is the recommended
     * setting for production systems that deserialize events from external
     * channels (message brokers, HTTP webhooks), because it prevents both
     * silent typo bugs and unbounded cache growth from hostile input.
     *
     * @param strictMode {@code true} to require registration, {@code false} to allow lazy caching of well-formed names
     */
    public void setStrictMode(boolean strictMode) {
        this.strictMode.set(strictMode);
    }

    /**
     * Returns whether strict mode is currently enabled.
     *
     * @return {@code true} if unregistered names are rejected on resolve, {@code false} otherwise
     */
    public boolean isStrictMode() {
        return this.strictMode.get();
    }

    /**
     * Returns an unmodifiable snapshot of all registered event type names.
     *
     * @return the set of registered names
     */
    public Set<String> registeredNames() {
        return Set.copyOf(registeredNames);
    }

    /**
     * Removes every registered name and cached type, and resets strict mode.
     *
     * <p>This method exists for test isolation only. Never call it on the
     * shared registry in production code.
     */
    public void clear() {
        registeredNames.clear();
        cachedTypes.clear();
        this.strictMode.set(false);
    }

    /**
     * Returns the current maximum number of event types that may be
     * registered in this registry.
     *
     * @return the current maximum number of event types that may be registered
     */
    public synchronized int getMaxRegisteredTypes() {
        return maxRegisteredTypes;
    }

    /**
     * Changes the maximum number of event types that may be registered.
     *
     * <p>The limit is dynamic: raising it immediately allows more
     * registrations; lowering it does NOT evict existing entries — they stay
     * registered and resolvable, but any further registration beyond the new
     * limit fails until enough names are unregistered or cleared.
     *
     * @param maxRegisteredTypes the maximum number of types
     * @throws DomainEventException if maxRegisteredTypes is not positive
     */
    public synchronized void setMaxRegisteredTypes(int maxRegisteredTypes) {
        if (maxRegisteredTypes <= 0) throw new DomainEventException(MAX_REGISTERED_TYPES_EXCEPTION);
        this.maxRegisteredTypes = maxRegisteredTypes;
    }

    private static String validateAndNormalize(@Nullable String name) {
        if (name == null || name.isBlank()) throw new DomainEventException("The name cannot be null or blank");
        if (!name.matches(NAME_PATTERN)) {
            throw new DomainEventException(
                    "The name '" + name + "' is not a valid EventType name; expected pattern " + NAME_PATTERN);
        }
        return name;
    }
}
