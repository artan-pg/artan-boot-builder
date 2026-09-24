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

/**
 * Represents a type of domain event, providing a human-readable name.
 *
 * <p>This functional interface is used to categorize domain events and provide
 * a type-safe way to reference event types across the system.
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
     * @return the event type name
     */
    @NonNull
    String getName();

    /**
     * Returns the category of this event type.
     *
     * <p>Categories group related event types for routing and
     * filtering purposes. For {@link NamedEventType} the category is derived
     * from the structured name syntax {@code category.name.version}, where
     * both {@code category} and {@code version} are optional and {@code name}
     * is mandatory.
     *
     * <p>Custom implementations may return any value; the default is the empty
     * string (uncategorized).
     *
     * @return the event category
     */
    default String getCategory() {
        return "";
    }

    /**
     * Returns the version of this event type.
     *
     * <p>Versions allow schema evolution of events while keeping
     * backward compatibility. For {@link NamedEventType} the version is the
     * final segment of the structured name syntax
     * {@code category.name.version} and is present only when the name has at
     * least two dots, otherwise it is the empty string. Names with a single
     * dot such as {@code "customer.registered"} are interpreted as
     * {@code category.name} without a version.
     *
     * <p>Custom implementations may return any value; the default is the empty
     * string (unversioned).
     *
     * @return the event version
     */
    default String getVersion() {
        return "";
    }

    /**
     * Creates a non-cached {@code EventType} that derives its name from the
     * given string.
     *
     * @param name the event type name; must not be {@code null} or blank
     * @return a new {@code NamedEventType} instance
     * @throws DomainEventException if name is {@code null}, {@code blank}, or malformed according to
     *                              {@link EventTypeRegistry#NAME_PATTERN}
     */
    static EventType named(String name) {
        return new NamedEventType(name);
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
     * <p>When the name follows the {@code category.eventName.version} convention
     * (at least two dot-separated segments, e.g. {@code order.placed.v1}), the
     * leading segments are exposed as {@link #getCategory()} and the final
     * segment as {@link #getVersion()}; otherwise both default to the empty
     * string.
     *
     * @param name the event type name; guaranteed non-null and well-formed
     */
    record NamedEventType(String name) implements EventType {

        /**
         * Canonical constructor; validates the name.
         *
         * @param name the event type name
         * @throws DomainEventException if the name is invalid
         */
        public NamedEventType {
            if (name == null || name.isBlank()) throw new DomainEventException("The name cannot be null or blank");

            // To resolve the [MultipleStringLiterals] rule violation in Checkstyle
            String prefixName = "The name '" + name;
            if (!name.matches(EventTypeRegistry.NAME_PATTERN)) {
                throw new DomainEventException(
                        prefixName + "' is not a valid EventType name; expected pattern "
                                + EventTypeRegistry.NAME_PATTERN);
            }
            if (!name.matches(EventTypeRegistry.STRUCTURED_NAME_PATTERN)) {
                throw new DomainEventException(
                        prefixName + "' does not follow the required 'category.name.version' syntax"
                                + " (name mandatory, category and version optional)");
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getCategory() {
            int lastDot = name.lastIndexOf('.');
            if (lastDot <= 0) return "";
            int secondLastDot = name.lastIndexOf('.', lastDot - 1);

            // Exactly two segments: no version, so the left part is the
            // category ("customer" in "customer.registered").
            if (secondLastDot <= 0) return name.substring(0, lastDot);

            // Three or more segments: the trailing segment is the version, so
            // the category is everything before the name segment.
            return name.substring(0, secondLastDot);
        }

        @Override
        public String getVersion() {
            int lastDot = name.lastIndexOf('.');
            if (lastDot <= 0) return "";
            return name.lastIndexOf('.', lastDot - 1) > 0 ? name.substring(lastDot + 1) : "";
        }

        /**
         * Returns the {@code name} component of the
         * {@code category.name.version} syntax.
         *
         * <p>Because {@code category} and {@code version} are optional, the
         * parsing depends on how many dot-separated segments the name has:
         * <ul>
         *   <li>one segment ({@code "OrderPlacedEvent"}) the whole name is
         *       the mandatory {@code name} part</li>
         *   <li>two segments ({@code "customer.registered"}) the version is
         *       absent, so the second segment is the {@code name} part and the
         *       first one is the {@code category}</li>
         *   <li>three or more segments
         *       ({@code "order.OrderPlaced.v1"}) the middle segment (between
         *       the second-to-last and the last dot) is the {@code name}
         *       part</li>
         * </ul>
         *
         * @return the parsed event type name component
         */
        public String getSimpleName() {
            int lastDot = name.lastIndexOf('.');
            if (lastDot <= 0) return name;
            int secondLastDot = name.lastIndexOf('.', lastDot - 1);
            if (secondLastDot > 0) return name.substring(secondLastDot + 1, lastDot);

            // Only one dot: no version segment, so everything before the dot
            // is "category.name" — but with a single dot the left part is the
            // category and the right part is the mandatory name.
            return name.substring(lastDot + 1);
        }
    }
}
