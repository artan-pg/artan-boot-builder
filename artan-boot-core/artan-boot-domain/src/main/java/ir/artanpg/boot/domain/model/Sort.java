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

package ir.artanpg.boot.domain.model;

import ir.artanpg.boot.domain.exception.DomainException;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

/**
 * A value object representing sorting criteria for queries.
 *
 * <p>This class uses a {@link LinkedHashMap} to preserve the order of sort
 * fields and is immutable. Any modification returns a new instance of Sort.
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.
 *
 * @author Mohammad Yazdian
 * @see Page
 * @see Pageable
 * @since 0.1.0
 */
public class Sort implements Serializable {

    @Serial
    private static final long serialVersionUID = 3763682904975642052L;

    private static final String PROPERTY_NULL_EXCEPTION = "The property cannot be null or blank";
    private static final String PROPERTIES_NULL_EXCEPTION = "The properties cannot be null or blank";

    /**
     * The map of sort orders where the key is the property name and the value
     * is the sort direction.
     */
    private final Map<String, Direction> orders;

    /**
     * Creates a Sort with ascending order for a single field.
     *
     * @param orders mapping of fields to sort directions
     */
    private Sort(Map<String, Direction> orders) {
        this.orders = Collections.unmodifiableMap(orders);
    }

    /**
     * Create a Sort instances representing no sorting setup at all.
     *
     * @return an unsorted {@link Sort} instance
     */
    public static Sort unsorted() {
        return new Sort(new LinkedHashMap<>());
    }

    /**
     * Creates a {@link Sort} with ascending order for a single field.
     *
     * @param property the field name to sort by in ascending order
     * @return a new {@link Sort} instance with ascending sorting
     * @throws DomainException if property is {@code null} or {@code blank}
     */
    public static Sort asc(String property) {
        if (property == null || property.isBlank()) throw new DomainException(PROPERTY_NULL_EXCEPTION);
        return new Sort(Collections.singletonMap(property, Direction.ASC));
    }

    /**
     * Creates a {@link Sort} with ascending order for multiple fields.
     *
     * <p>The fields will be sorted in the order they are provided.
     *
     * @param properties the field names to sort by in ascending order
     * @return a new {@link Sort} instance with ascending sorting for all fields, or an
     *         empty {@link Sort} if no properties are provided
     * @throws DomainException if properties is {@code null} or any property is {@code null} or {@code blank}
     */
    public static Sort asc(String... properties) {
        if (properties == null) throw new DomainException(PROPERTIES_NULL_EXCEPTION);
        if (properties.length == 0) return Sort.unsorted();

        Map<String, Direction> map = new LinkedHashMap<>();
        for (String property : properties) {
            if (property == null || property.isBlank()) throw new DomainException(PROPERTY_NULL_EXCEPTION);
            map.put(property, Direction.ASC);
        }

        return new Sort(map);
    }

    /**
     * Creates a {@link Sort} with descending order for a single field.
     *
     * @param property the field name to sort by in descending order
     * @return a new {@link Sort} instance with descending sorting
     * @throws DomainException if property is {@code null} or {@code blank}
     */
    public static Sort desc(String property) {
        if (property == null || property.isBlank()) throw new DomainException(PROPERTY_NULL_EXCEPTION);
        return new Sort(Collections.singletonMap(property, Direction.DESC));
    }

    /**
     * Creates a {@link Sort} with descending order for multiple fields.
     *
     * <p>The fields will be sorted in the order they are provided.
     *
     * @param properties the field names to sort by in descending order
     * @return a new {@link Sort} instance with descending sorting for all fields, or
     *         an empty {@link Sort} if no properties are provided
     * @throws DomainException if properties is {@code null} or any property is {@code null} or {@code blank}
     */
    public static Sort desc(String... properties) {
        if (properties == null) throw new DomainException(PROPERTIES_NULL_EXCEPTION);
        if (properties.length == 0) return Sort.unsorted();

        Map<String, Direction> map = new LinkedHashMap<>();
        for (String property : properties) {
            if (property == null || property.isBlank()) throw new DomainException(PROPERTY_NULL_EXCEPTION);
            map.put(property, Direction.DESC);
        }

        return new Sort(map);
    }

    /**
     * Adds a new field with ascending order to the current sorting criteria.
     *
     * <p>This method is immutable and returns a new Sort instance. The new
     * field will be added after all existing fields.
     *
     * @param property the field name to add with ascending order
     * @return a new {@link Sort} instance with the additional ascending field
     * @throws DomainException if property is {@code null} or {@code blank}
     */
    public Sort andAsc(String property) {
        if (property == null || property.isBlank()) throw new DomainException(PROPERTY_NULL_EXCEPTION);

        Map<String, Direction> newMap = new LinkedHashMap<>(this.orders);
        newMap.put(property, Direction.ASC);

        return new Sort(newMap);
    }

    /**
     * Adds a new field with descending order to the current sorting criteria.
     *
     * <p>This method is immutable and returns a new Sort instance. The new
     * field will be added after all existing fields.
     *
     * @param property the field name to add with descending order
     * @return a new {@link Sort} instance with the additional descending field
     * @throws DomainException if property is {@code null} or {@code blank}
     */
    public Sort andDesc(String property) {
        if (property == null || property.isBlank()) throw new DomainException(PROPERTY_NULL_EXCEPTION);

        Map<String, Direction> newMap = new LinkedHashMap<>(this.orders);
        newMap.put(property, Direction.DESC);

        return new Sort(newMap);
    }

    /**
     * Combines this Sort with another {@link Sort}.
     *
     * <p>This method is immutable and returns a new Sort instance containing
     * all sorting criteria from both Sorts. If a field exists in both Sorts,
     * the direction from the provided Sort will override the current one.
     *
     * <p>The order of fields will be: fields from this Sort first, followed by
     * fields from the provided Sort.
     *
     * @param sort the Sort to combine with
     * @return a new {@link Sort} instance containing all sorting criteria
     * @throws DomainException if sort is {@code null}
     */
    public Sort and(Sort sort) {
        if (sort == null) throw new DomainException("The sort must not be null");

        Map<String, Direction> combined = new LinkedHashMap<>(this.orders);
        combined.putAll(sort.orders);

        return new Sort(combined);
    }

    /**
     * Gets the sort direction for a specific field.
     *
     * @param property the field name to get the direction for
     * @return the direction of the field
     */
    @Nullable
    public Direction getDirection(String property) {
        if (property == null) return null;
        return this.orders.get(property);
    }

    /**
     * Gets the set of all field names in this Sort.
     *
     * @return an unmodifiable Set of field names
     */
    public Set<String> getProperties() {
        return this.orders.keySet();
    }

    /**
     * Gets the internal map of field-direction pairs.
     *
     * @return an unmodifiable Map of field names to their directions
     */
    public Map<String, Direction> getOrders() {
        return this.orders;
    }

    /**
     * Gets the number of sorting criteria in this Sort.
     *
     * @return the number of fields with sorting criteria
     */
    public int size() {
        return this.orders.size();
    }

    /**
     * Checks if this Sort contains any sorting criteria.
     *
     * @return {@code true}, if there are no sorting criteria, {@code false} otherwise
     */
    public boolean isEmpty() {
        return this.orders.isEmpty();
    }

    /**
     * Checks if this Sort contains a specific field.
     *
     * @param property the field name to check
     * @return {@code true}, if the field exists in this Sort, {@code false} otherwise
     */
    public boolean contains(String property) {
        return this.orders.containsKey(property);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sort that)) return false;
        return Objects.equals(this.orders, that.orders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.orders);
    }

    @Override
    public String toString() {
        if (this.orders.isEmpty()) return Sort.class.getSimpleName() + "[empty]";

        StringJoiner joiner = new StringJoiner(", ", Sort.class.getSimpleName() + "[", "]");
        for (Map.Entry<String, Direction> entry : this.orders.entrySet()) {
            joiner.add("property=" + entry.getKey() + ", direction=" + entry.getValue());
        }

        return joiner.toString();
    }

    /**
     * Enumeration of sort directions.
     *
     * @author Mohammad Yazdian
     */
    public enum Direction {
        /**
         * Ascending order direction.
         *
         * <p>Sorts values from lowest to highest:
         * <ul>
         *   <li>Numbers: 1, 2, 3, 4, ...</li>
         *   <li>Strings: A, B, C, D, ...</li>
         *   <li>Dates: Oldest to newest</li>
         * </ul>
         */
        ASC,

        /**
         * Descending order direction.
         *
         * <p>Sorts values from highest to lowest:
         * <ul>
         *   <li>Numbers: 10, 9, 8, 7, ...</li>
         *   <li>Strings: Z, Y, X, W, ...</li>
         *   <li>Dates: Newest to oldest</li>
         * </ul>
         */
        DESC;

        /**
         * Checks if this direction is ascending.
         *
         * @return {@code true}, if the direction is ASC, {@code false} otherwise
         */
        public boolean isAscending() {
            return this == ASC;
        }

        /**
         * Checks if this direction is descending.
         *
         * @return {@code true}, if the direction is DESC, {@code false} otherwise
         */
        public boolean isDescending() {
            return this == DESC;
        }
    }
}
