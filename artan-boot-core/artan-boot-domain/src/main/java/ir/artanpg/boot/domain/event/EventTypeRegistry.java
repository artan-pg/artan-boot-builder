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
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 * <h2>Usage</h2>
 * Register your event types once during application bootstrap:
 * <pre>{@code
 * EventTypeRegistry registry = EventTypeRegistry.shared();
 * registry.register(EventType.valueOf("OrderPlacedEvent"));
 * registry.register(EventType.valueOf("order.shipped.v2"));
 * }</pre>
 *
 * <p>By default the shared registry operates in non-strict mode, meaning
 * {@link #resolve(String)} falls back to caching arbitrary (but well-formed)
 * names. Call {@link #setStrictMode(boolean)} to reject anything that has not
 * been registered explicitly.
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

    private static final int MAX_REGISTERED_TYPES = 4096;

    /**
     * The process-wide shared registry instance.
     */
    private static final EventTypeRegistry SHARED = new EventTypeRegistry();

    private final Set<String> registeredNames = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, EventType> cachedTypes = new ConcurrentHashMap<>();
    private volatile boolean strictMode = false;

    /**
     * Creates a new, empty registry instance.
     *
     * <p>Useful for tests or isolated subsystems; most applications should
     * use {@link #shared()} instead.
     */
    public EventTypeRegistry() {
        // empty on purpose
    }

    /**
     * Returns the process-wide shared registry.
     *
     * @return the shared registry instance; never {@code null}
     */
    public static EventTypeRegistry shared() {
        return SHARED;
    }

    /**
     * Registers an event type as known to the application.
     *
     * <p>Registration is idempotent; registering the same name twice has no
     * additional effect. Registered names are also placed in the internal
     * cache so {@link #resolve(String)} returns the very same instance.
     *
     * @param eventType the event type to register; must not be {@code null}
     * @return the canonical (cached) {@code EventType} for the given name
     * @throws DomainException if {@code eventType} is {@code null}, its name
     *                         is invalid, or the registry exceeded
     *                         {@value #MAX_REGISTERED_TYPES} entries
     */
    public EventType register(@NonNull EventType eventType) {
        if (eventType == null) throw new DomainException("The eventType cannot be null");
        String name = validateAndNormalize(eventType.getName());

        if (!cachedTypes.containsKey(name) && registeredNames.size() >= MAX_REGISTERED_TYPES) {
            throw new DomainException("The EventType registry exceeded " + MAX_REGISTERED_TYPES + " entries");
        }
        registeredNames.add(name);
        return cachedTypes.computeIfAbsent(name, EventType::named);
    }

    /**
     * Registers an event type by name.
     *
     * @param name the event type name; must match {@value #NAME_PATTERN}
     * @return the canonical (cached) {@code EventType} for the given name
     * @throws DomainException if the name is invalid or the registry is full
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
     * @param name the event type name to unregister; may be {@code null} (no-op)
     * @return {@code true} if the name was registered and has been removed
     */
    public boolean unregister(@Nullable String name) {
        if (name == null) return false;
        cachedTypes.remove(name);
        return registeredNames.remove(name);
    }

    /**
     * Resolves an event type by name, honouring the current strictness mode.
     *
     * <ul>
     *   <li>If the name is registered, the canonical cached instance is
     *       returned.</li>
     *   <li>In non-strict mode, a well-formed but unregistered name is cached
     *       and returned (same behaviour as {@link EventType#valueOf(String)}).</li>
     *   <li>In strict mode, an unregistered name throws
     *       {@link DomainException}.</li>
     * </ul>
     *
     * @param name the event type name to resolve
     * @return the resolved {@code EventType} instance
     * @throws DomainException if the name is {@code null}, blank, malformed,
     *                         or unregistered while strict mode is enabled
     */
    public EventType resolve(@Nullable String name) {
        String normalized = validateAndNormalize(name);

        EventType cached = cachedTypes.get(normalized);
        if (cached != null) return cached;

        if (strictMode && !registeredNames.contains(normalized)) {
            throw new DomainException(
                    "Unknown EventType '" + normalized + "'. Register it via EventTypeRegistry.register() first"
                            + " or disable strict mode.");
        }
        return cachedTypes.computeIfAbsent(normalized, EventType::named);
    }

    /**
     * Checks whether the given name is explicitly registered.
     *
     * @param name the event type name to check; may be {@code null}
     * @return {@code true} if a type with that exact name is registered
     */
    public boolean isRegistered(@Nullable String name) {
        return name != null && registeredNames.contains(name);
    }

    /**
     * Checks whether the given event type is registered (by name).
     *
     * @param eventType the event type to check; may be {@code null}
     * @return {@code true} if the type's name is registered
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
     * @param strictMode {@code true} to require registration, {@code false}
     *                   to allow lazy caching of well-formed names
     */
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    /**
     * Returns whether strict mode is currently enabled.
     *
     * @return {@code true} if unregistered names are rejected on resolve
     */
    public boolean isStrictMode() {
        return strictMode;
    }

    /**
     * Returns an unmodifiable snapshot of all registered event type names.
     *
     * @return the set of registered names; never {@code null}
     */
    public Set<String> registeredNames() {
        return Collections.unmodifiableSet(Set.copyOf(registeredNames));
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
        strictMode = false;
    }

    private static String validateAndNormalize(@Nullable String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("The name cannot be null or blank");
        }
        if (!name.matches(NAME_PATTERN)) {
            throw new DomainException(
                    "The name '" + name + "' is not a valid EventType name; expected pattern " + NAME_PATTERN);
        }
        return name;
    }
}
