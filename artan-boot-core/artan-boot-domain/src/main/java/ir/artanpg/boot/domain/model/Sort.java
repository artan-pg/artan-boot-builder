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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A value object representing sorting criteria for queries.
 *
 * <p>The sort criteria are stored as an ordered, immutable list of
 * {@link Order} entries (property + direction). Unlike a plain map-based
 * representation, this model preserves the exact order of the fields.
 *
 * <p>Any modification returns a new {@code Sort} instance.
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

    private static final float DEFAULT_SET_COLLECTION_SIZE = 0.75f;
    /**
     * The ordered, immutable list of sort orders.
     */
    private final List<Order> orders;

    /**
     * Creates a Sort from the given ordered list of sort orders.
     *
     * @param orders the ordered sort orders
     */
    private Sort(List<Order> orders) {
        this.orders = List.copyOf(orders);
    }

    /**
     * Create a Sort instances representing no sorting setup at all.
     *
     * @return an unsorted {@link Sort} instance
     */
    public static Sort unsorted() {
        return new Sort(Collections.emptyList());
    }

    /**
     * Creates a {@link Sort} with ascending order for a single field.
     *
     * @param property the field name to sort by in ascending order
     * @return a new {@link Sort} instance with ascending sorting
     * @throws DomainException if property is {@code null} or {@code blank}
     */
    public static Sort asc(String property) {
        validateProperty(property);
        return new Sort(List.of(new Order(property, Direction.ASC)));
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
        return of(properties, Direction.ASC);
    }

    /**
     * Creates a {@link Sort} with descending order for a single field.
     *
     * @param property the field name to sort by in descending order
     * @return a new {@link Sort} instance with descending sorting
     * @throws DomainException if property is {@code null} or {@code blank}
     */
    public static Sort desc(String property) {
        validateProperty(property);
        return new Sort(List.of(new Order(property, Direction.DESC)));
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
        return of(properties, Direction.DESC);
    }

    /**
     * Builds an ordered {@link Sort} from the given properties, all with the
     * same direction, preserving the iteration order of the varargs array.
     *
     * @param properties the field names (in priority order)
     * @param direction  the direction to apply to every field
     * @return a new {@link Sort} instance
     * @throws DomainException if properties is {@code null} or any property is {@code null} or {@code blank}
     */
    private static Sort of(String[] properties, Direction direction) {
        if (properties == null) throw new DomainException(PROPERTIES_NULL_EXCEPTION);
        if (properties.length == 0) return Sort.unsorted();

        List<Order> list = new ArrayList<>(properties.length);
        Set<String> uniqueProperties =
                LinkedHashSet.newLinkedHashSet((int) (properties.length / DEFAULT_SET_COLLECTION_SIZE) + 1);

        for (String property : properties) {
            if (uniqueProperties.add(property)) {
                validateProperty(property);
                list.add(new Order(property, direction));
            }
        }

        return new Sort(list);
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
        return and(Sort.asc(property));
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
        return and(Sort.desc(property));
    }

    /**
     * Combines this Sort with another {@link Sort}.
     *
     * <p>This method is immutable and returns a new Sort instance containing
     * all sorting criteria from both Sorts. If a field exists in both Sorts,
     * the previous occurrence is removed and the incoming one is appended at
     * the end, so the latest direction and position win (consistent with the
     * previous map-based contract while preserving order semantics).
     *
     * @param sort the Sort to combine with
     * @return a new {@link Sort} instance containing all sorting criteria
     * @throws DomainException if sort is {@code null}
     */
    public Sort and(Sort sort) {
        if (sort == null) throw new DomainException("The sort must not be null");
        if (sort.isEmpty()) return this;
        if (this.isEmpty()) return sort;

        Set<String> incomingProperties = sort.orders.stream()
                .map(Order::property)
                .collect(Collectors.toSet());

        List<Order> combined = new ArrayList<>(this.orders.size() + sort.orders.size());
        for (Order existing : this.orders) {
            if (!incomingProperties.contains(existing.property())) combined.add(existing);
        }
        combined.addAll(sort.orders);

        return new Sort(combined);
    }

    /**
     * Gets the sort direction for a specific field.
     *
     * <p>If the field occurs more than once, the last occurrence wins, which
     * matches the effective behavior of {@link #and(Sort)} overriding.
     *
     * @param property the field name to get the direction for
     * @return the direction of the field, or {@code null} if absent / {@code null} input
     */
    @Nullable
    public Direction getDirection(String property) {
        if (property == null) return null;

        Direction direction = null;
        for (Order order : this.orders) {
            if (order.property().equals(property)) {
                direction = order.direction();
            }
        }
        return direction;
    }

    /**
     * Gets the set of all field names in this Sort.
     *
     * @return an unmodifiable Set of distinct field names
     */
    public Set<String> getProperties() {
        LinkedHashSet<String> properties = this.orders.stream()
                .map(Order::property)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(properties);
    }

    /**
     * Gets the sorting criteria as an ordered, unmodifiable view mapping each
     * property to its direction.
     *
     * <p>The returned map preserves the priority order of the criteria. When
     * the same property occurs more than once, the last occurrence determines
     * the mapped value.
     *
     * @return an unmodifiable Map of field names to their directions
     */
    public Map<String, Direction> getOrders() {
        Map<String, Direction> map = new LinkedHashMap<>();
        for (Order order : this.orders) {
            map.put(order.property(), order.direction());
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns a sequential stream of the sort orders in priority order.
     *
     * <p>This is the canonical way to iterate the full criteria, without
     * losing ordering information.
     *
     * @return a stream of {@link Order} entries in priority order
     */
    public Stream<Order> stream() {
        return this.orders.stream();
    }

    /**
     * Gets the number of sorting criteria in this Sort.
     *
     * @return the number of order entries (duplicated properties counted)
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
        if (property == null) return false;
        return this.orders.stream().anyMatch(order -> order.property().equals(property));
    }

    /**
     * Validates that the given property is neither {@code null} nor blank.
     *
     * @param property the property to validate
     * @throws DomainException if property is {@code null} or {@code blank}
     */
    private static void validateProperty(String property) {
        if (property == null || property.isBlank()) throw new DomainException(PROPERTY_NULL_EXCEPTION);
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
        for (Order order : this.orders) {
            joiner.add("property=" + order.property() + ", direction=" + order.direction());
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

    /**
     * An immutable pair of a property name and its sort direction.
     *
     * @param property  the field name to sort by
     * @param direction the sort direction for the field
     * @author Mohammad Yazdian
     * @since 0.2.0
     */
    public record Order(String property, Direction direction) implements Serializable {

        @Serial
        private static final long serialVersionUID = 7300392721151295599L;

        /**
         * Compact constructor validating the order entry.
         *
         * @param property  the field name to sort by
         * @param direction the sort direction for the field
         * @throws DomainException if property is {@code null} or {@code blank} or direction is {@code null}
         */
        public Order {
            if (property == null || property.isBlank()) throw new DomainException(PROPERTY_NULL_EXCEPTION);
            if (direction == null) throw new DomainException("The direction cannot be null");
        }

        /**
         * Converts this order into a two-element map entry, useful for
         * interoperability with map-based persistence/adapter layers.
         *
         * @return a map entry of property to direction
         */
        public Map.Entry<String, Direction> toEntry() {
            return new AbstractMap.SimpleImmutableEntry<>(this.property, this.direction);
        }
    }
}
