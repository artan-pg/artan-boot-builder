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

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Immutable class describing the type of domain event.
 *
 * <p>An event type is identified by three coordinates:
 * <ul>
 *   <li>{@code name}: mandatory logical name of the event</li>
 *   <li>{@code category}: optional grouping (e.g. bounded context)</li>
 *   <li>{@code version}: optional schema version</li>
 * </ul>
 *
 * <p><b>Character rules:</b> Each non-absent field may contain only ASCII
 * letters, digits and the dash character ({@code A-Z}, {@code a-z},
 * {@code 0-9}, {@code -}).
 *
 * <p>The combined length of {@code name}, {@code category} and {@code version}
 * must not exceed {@value #MAX_TOTAL_LENGTH} characters.
 *
 * <p><b>Immutability and equality:</b> All fields are {@code final}. Equality
 * and hashing are based solely on the three coordinates after normalization
 * ({@code blank} optional fields become {@code null}).
 *
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @since 0.1.0
 */
public final class EventType {

    /**
     * Maximum allowed sum of the lengths of name, category and version.
     */
    public static final int MAX_TOTAL_LENGTH = 64;

    /**
     * Allowed character pattern for each non-blank field.
     *
     * <p>Letters (upper/lower), digits and dash only.
     */
    public static final String FIELD_PATTERN = "[A-Za-z0-9-]+";

    private static final int NAME_INDEX = 0;
    private static final int CATEGORY_INDEX = 1;
    private static final int VERSION_INDEX = 2;

    /**
     * Human-readable description of {@link #FIELD_PATTERN}, used to build
     * validation error messages.
     */
    private static final String FIELD_PATTERN_MSG = "may contain only ASCII letters, digits and '-'";

    /**
     * Expected number of segments in a cache key produced by
     * {@link #toCacheKey()}.
     */
    private static final int CACHE_KEY_SEGMENT = 3;

    /**
     * Mandatory logical name of the event.
     */
    private final String name;

    /**
     * Optional grouping of the event.
     *
     * <p>May be {@code null}. When present, matches {@link #FIELD_PATTERN}.
     */
    private final String category;

    /**
     * Optional schema version of the event.
     *
     * <p>May be {@code null}. When present, matches {@link #FIELD_PATTERN}.
     */
    private final String version;

    /**
     * Constructs a new {@code EventType} with pre-validated coordinates.
     *
     * <p>All arguments must already have been normalized and validated
     * by {@link #of(String, String, String)}. This constructor performs
     * no validation.
     *
     * @param name     the normalized, non-null event name
     * @param category the normalized category, or {@code null}
     * @param version  the normalized version, or {@code null}
     */
    private EventType(String name, @Nullable String category, @Nullable String version) {
        this.name = name;
        this.category = category;
        this.version = version;
    }

    /**
     * Creates an {@code EventType} with only a name.
     *
     * @param name the event name
     * @return a validated event type with no category or version
     * @throws DomainEventException if {@code name} is invalid
     */
    public static EventType of(@NonNull String name) {
        return of(name, null, null);
    }

    /**
     * Creates an {@code EventType} with a name and category.
     *
     * @param name     the event name
     * @param category the optional category
     * @return a validated event type with no version
     * @throws DomainEventException if any argument is invalid
     */
    public static EventType of(@NonNull String name, @Nullable String category) {
        return of(name, category, null);
    }

    /**
     * Creates a fully validated {@code EventType}.
     *
     * <p>Blank optional arguments are normalized to {@code null}.
     * The combined length of the three coordinates is checked
     * against {@link #MAX_TOTAL_LENGTH}.</p>
     *
     * @param name     the event name
     * @param category the optional category
     * @param version  the optional version
     * @return a validated event type
     * @throws DomainEventException if a field is invalid or the total length exceeds the limit
     */
    public static EventType of(@NonNull String name, @Nullable String category, @Nullable String version) {
        String normalizedName = requireValidName(name);
        String normalizedCategory = normalizeOptional(category, "category");
        String normalizedVersion = normalizeOptional(version, "version");

        validateTotalLength(normalizedName, normalizedCategory, normalizedVersion);

        return new EventType(normalizedName, normalizedCategory, normalizedVersion);
    }

    /**
     * Creates a new builder for constructing {@link EventType}.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the mandatory logical name of this event type.
     *
     * @return the event name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the optional category of this event type.
     *
     * @return the category, or {@code null} when absent
     */
    @Nullable
    public String getCategory() {
        return category;
    }

    /**
     * Returns the optional schema version of this event type.
     *
     * @return the version, or {@code null} when absent
     */
    @Nullable
    public String getVersion() {
        return version;
    }

    /**
     * Checks whether a category is present.
     *
     * @return {@code true} if category is set, {@code false} otherwise
     */
    public boolean hasCategory() {
        return category != null;
    }

    /**
     * Checks whether a version is present.
     *
     * @return {@code true} if version is set, {@code false} otherwise
     */
    public boolean hasVersion() {
        return version != null;
    }

    /**
     * Returns a stable cache key for this event type.
     *
     * <p>The key is formed by concatenating the three coordinates
     * separated by the NUL character ({@code '\u0000'}). Absent
     * optional fields are represented as empty segments. The
     * resulting string is suitable for use as a map key and can be
     * parsed back with {@link #fromCacheKey(String)}.
     *
     * @return the cache key
     * @see #fromCacheKey(String)
     */
    @NonNull
    public String toCacheKey() {
        return name + '\u0000' + (category != null ? category : "") + '\u0000' + (version != null ? version : "");
    }

    /**
     * Parses a cache key previously produced by {@link #toCacheKey()}.
     *
     * <p>The key must contain exactly three NUL-separated segments.
     * Empty segments are interpreted as absent optional fields. The
     * parsed coordinates are re-validated by
     * {@link #of(String, String, String)}.
     *
     * @param cacheKey the key
     * @return a validated event type
     * @throws DomainEventException if the key is {@code null}, malformed, or fails validation
     * @see #toCacheKey()
     */
    public static EventType fromCacheKey(@NonNull String cacheKey) {
        if (cacheKey == null) throw new DomainEventException("The cache key cannot be null");

        String[] parts = cacheKey.split("\u0000", -1);
        if (parts.length != CACHE_KEY_SEGMENT) {
            throw new DomainEventException("The cache key is malformed; expected 3 segments");
        }

        String name = parts[NAME_INDEX];
        String category = parts[CATEGORY_INDEX].isEmpty() ? null : parts[CATEGORY_INDEX];
        String version = parts[VERSION_INDEX].isEmpty() ? null : parts[VERSION_INDEX];

        return of(name, category, version);
    }

    /**
     * Validates and returns the mandatory event name.
     *
     * @param name the candidate name
     * @return the validated name
     * @throws DomainEventException if name is {@code null}, {@code blank}, or contains disallowed characters
     */
    private static String requireValidName(@Nullable String name) {
        if (name == null || name.isBlank()) {
            throw new DomainEventException("The event type name cannot be null or blank");
        }
        if (!name.matches(FIELD_PATTERN)) throw new DomainEventException("The event type name " + FIELD_PATTERN_MSG);
        return name;
    }

    /**
     * Normalizes an optional field.
     *
     * <p>{@code null} and {@code blank} values are converted to {@code null}.
     * Non-blank values must match {@link #FIELD_PATTERN}.
     *
     * @param value      the candidate value
     * @param fieldLabel the label used in error messages
     * @return the normalized value, or {@code null}
     * @throws DomainEventException if a non-blank value contains disallowed characters
     */
    private static @Nullable String normalizeOptional(@Nullable String value, String fieldLabel) {
        if (value == null || value.isBlank()) return null;
        if (!value.matches(FIELD_PATTERN)) {
            throw new DomainEventException("The event type " + fieldLabel + " " + FIELD_PATTERN_MSG);
        }
        return value;
    }

    /**
     * Validates the combined length of the three coordinates.
     *
     * @param name     the validated name
     * @param category the normalized category, or {@code null}
     * @param version  the normalized version, or {@code null}
     * @throws DomainEventException if the combined length exceeds {@link #MAX_TOTAL_LENGTH}
     */
    private static void validateTotalLength(String name, @Nullable String category, @Nullable String version) {
        int total = name.length() +
                (category != null ? category.length() : 0) +
                (version != null ? version.length() : 0);

        if (total > MAX_TOTAL_LENGTH) {
            throw new DomainEventException(
                    "The combined length of name, category and version must not exceed " + MAX_TOTAL_LENGTH +
                            " characters (actual: " + total + ")");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventType that)) return false;

        return name.equals(that.name)
                && Objects.equals(category, that.category)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, category, version);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EventType.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("category='" + category + "'")
                .add("version='" + version + "'")
                .toString();
    }

    /**
     * Builder for constructing immutable {@link EventType} instances.
     *
     * @author Mohammad Yazdian
     * @since 0.1.0
     */
    public static final class Builder {

        private String name;
        private String category;
        private String version;

        /**
         * Constructs an empty builder.
         *
         * <p>The constructor is private. use {@link EventType#builder()}
         * to obtain a builder instance.
         */
        private Builder() {
        }

        /**
         * Sets the mandatory name.
         *
         * @param name the event type name
         * @return this builder instance for method chaining
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the optional category.
         *
         * @param category the category
         * @return this builder instance for method chaining
         */
        public Builder category(String category) {
            this.category = category;
            return this;
        }

        /**
         * Sets the optional version.
         *
         * @param version the version
         * @return this builder instance for method chaining
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Builds an immutable {@link EventType}.
         *
         * @return the constructed EventType instance
         * @throws DomainEventException if validation fails
         */
        public EventType build() {
            return EventType.of(name, category, version);
        }
    }
}
