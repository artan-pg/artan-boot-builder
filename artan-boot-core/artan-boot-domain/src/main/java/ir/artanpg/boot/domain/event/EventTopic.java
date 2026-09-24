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
import ir.artanpg.boot.domain.model.ValueObject;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a logical topic (channel) used by the domain event bus for
 * routing events to subscribers.
 *
 * <p>Topics are immutable value objects. The default topic for a domain event
 * is derived from {@link EventType#getName()}.
 *
 * @author Mohammad Yazdian
 * @see EventType
 * @see DomainEvent
 * @since 0.1.0
 */
public final class EventTopic implements ValueObject, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Map<String, EventTopic> CACHE = new ConcurrentHashMap<>();

    private final String name;

    private EventTopic(String name) {
        this.name = name;
    }

    /**
     * Returns a cached topic for the given name.
     *
     * @param name the topic name; must not be {@code null} or blank
     * @return the topic instance
     * @throws DomainException if name is {@code null} or blank
     */
    public static EventTopic of(@NonNull String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException("The topic name cannot be null or blank");
        }
        return CACHE.computeIfAbsent(name.trim(), EventTopic::new);
    }

    /**
     * Returns the default topic for the given event type.
     *
     * <p>The topic name is {@link EventType#getName()}.
     *
     * @param eventType the event type; must not be {@code null}
     * @return the corresponding topic
     */
    public static EventTopic of(@NonNull EventType eventType) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        return of(eventType.getName());
    }

    /**
     * Returns the default topic for the given domain event.
     *
     * @param event the domain event; must not be {@code null}
     * @return the corresponding topic
     */
    public static EventTopic of(@NonNull DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return of(event.getEventType());
    }

    /**
     * Returns the topic name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EventTopic that)) {
            return false;
        }
        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return "EventTopic[" + this.name + "]";
    }
}
