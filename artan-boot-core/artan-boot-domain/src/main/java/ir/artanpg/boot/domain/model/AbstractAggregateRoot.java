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

import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.exception.DomainException;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Abstract base implementation of {@link AggregateRoot} that provides
 * default behavior for domain event management.
 *
 * <p>This class handles the collection and lifecycle of domain events
 * produced during business operations. It is designed for <b>pure domain
 * models</b> without any infrastructure concerns.
 *
 * <h2>Responsibilities:</h2>
 * <ul>
 *   <li>Collecting domain events registered during business operations</li>
 *   <li>Providing immutable access to registered events</li>
 *   <li>Clearing events after successful publication (for infrastructure
 *       layer)</li>
 * </ul>
 *
 * @param <I> The type of identifier used by this aggregate root
 * @author Mohammad Yazdian
 * @see AggregateRoot
 * @since 0.1.0
 */
public abstract class AbstractAggregateRoot<I extends Identifier<?>> implements AggregateRoot<I> {

    @Serial
    private static final long serialVersionUID = 6306502304794036938L;

    private static final String IDENTIFIER_NULL_EXCEPTION = "The identifier cannot be null";

    /**
     * The unique identifier of this aggregate root.
     */
    private final I id;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Constructs a new aggregate root with the specified identifier.
     *
     * <p>The constructor initializes the domain event collection and
     * associates the aggregate with its unique identifier.
     *
     * @param id the unique identifier for this aggregate
     * @throws DomainException if the id is {@code null}
     */
    protected AbstractAggregateRoot(I id) {
        if (id == null) throw new DomainException(IDENTIFIER_NULL_EXCEPTION);

        this.id = id;
    }

    /**
     * Protected constructor for creating an aggregate root instance using a
     * builder.
     *
     * <p>This constructor is designed to be called from concrete aggregate
     * root constructors that receive their corresponding builder instances.
     * It extracts the identifier from the builder and sets it on the aggregate
     * root.
     *
     * @param builder the builder containing the aggregate root's identifier
     * @throws DomainException if the builder is {@code null}
     */
    protected AbstractAggregateRoot(AbstractBuilder<I, ?, ?> builder) {
        if (builder == null) throw new DomainException("The builder cannot be null");
        this.id = builder.id;
    }

    @Override
    public I getId() {
        return this.id;
    }

    @Override
    public List<DomainEvent> getDomainEvents() {
        List<DomainEvent> copyOf = List.copyOf(this.domainEvents);
        this.domainEvents.clear();
        return copyOf;
    }

    /**
     * Registers a domain event for later publication.
     *
     * <p>This method should be called by business methods when a significant
     * domain occurrence happens. The event will be collected and made
     * available through {@link #getDomainEvents()}.
     *
     * @param event the domain event to register
     * @throws DomainException if the event is {@code null}
     */
    protected void registerEvent(DomainEvent event) {
        if (event == null) throw new DomainException("The event object cannot be null");
        this.domainEvents.add(event);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof AbstractAggregateRoot<?> that)) return false;

        return getClass() == that.getClass() && id.equals(that.id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AbstractAggregateRoot.class.getSimpleName() + "[", "]")
                .add("id=" + getId().value())
                .toString();
    }

    /**
     * Abstract builder base class for constructing domain aggregate root
     * instances.
     *
     * <p>This builder provides a fluent API for building aggregate root
     * domains with type-safe generic parameters. It handles the common
     * pattern of setting an identifier and delegates the actual construction
     * to concrete subclasses.
     *
     * @param <I> the type of identifier used by the aggregate root
     * @param <C> the concrete aggregate root class being built
     * @param <B> the concrete builder class
     * @author Mohammad Yazdian
     * @see AbstractAggregateRoot
     * @since 0.1.0
     */
    protected abstract static class AbstractBuilder<I extends Identifier<?>, C extends AbstractAggregateRoot<I>,
            B extends AbstractBuilder<I, C, B>> {

        private I id;

        /**
         * Default constructor for the abstract builder.
         *
         * <p>This constructor initializes an empty builder instance with no
         * default values. All fields must be set using the builder's fluent
         * setter methods before calling {@link #build()}.
         */
        protected AbstractBuilder() {
            super();
        }

        /**
         * Sets the unique identifier for the aggregate root being built.
         *
         * @param id the identifier to set
         * @return this builder instance for method chaining
         */
        public B identifier(I id) {
            if (id == null) throw new DomainException(IDENTIFIER_NULL_EXCEPTION);
            this.id = id;
            return this.self();
        }

        /**
         * Returns the current builder instance cast to the concrete builder
         * type.
         *
         * @return the concrete builder instance
         */
        protected abstract B self();

        /**
         * Constructs and returns the final aggregate root instance.
         *
         * @return the constructed aggregate root instance
         */
        public abstract C build();
    }
}
