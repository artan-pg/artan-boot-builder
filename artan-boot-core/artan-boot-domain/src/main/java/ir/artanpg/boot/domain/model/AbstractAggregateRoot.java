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
 * Abstract base implementation of {@link AggregateRoot}.
 *
 * <p>Provides default behavior for domain event management
 * in pure domain models.
 *
 * @param <I> The type of identifier used by this aggregate
 * @author Mohammad Yazdian
 * @see AggregateRoot
 * @since 0.1.0
 */
public abstract class AbstractAggregateRoot<I extends Identifier<?>> implements AggregateRoot<I> {

    @Serial
    private static final long serialVersionUID = 6306502304794036938L;

    /**
     * Exception message for null identifier.
     */
    private static final String IDENTIFIER_NULL_EXCEPTION = "The identifier cannot be null";

    /**
     * The unique identifier of this aggregate root.
     */
    private final I id;

    /**
     * The list of registered domain events.
     */
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Constructs a new aggregate root with the specified id.
     *
     * @param id the unique identifier for this aggregate
     * @throws DomainException if the id is {@code null}
     */
    protected AbstractAggregateRoot(I id) {
        if (id == null) throw new DomainException(IDENTIFIER_NULL_EXCEPTION);

        this.id = id;
    }

    /**
     * Constructs an aggregate root using a builder.
     *
     * @param builder the builder containing the identifier
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
     * Abstract builder base class for aggregate root instances.
     *
     * <p>Provides a fluent API with type-safe generic parameters.
     *
     * @param <I> the type of identifier
     * @param <C> the concrete aggregate root class
     * @param <B> the concrete builder class
     * @author Mohammad Yazdian
     * @see AbstractAggregateRoot
     * @since 0.1.0
     */
    protected abstract static class AbstractBuilder<I extends Identifier<?>, C extends AbstractAggregateRoot<I>,
            B extends AbstractBuilder<I, C, B>> {

        /**
         * The identifier for the aggregate root.
         */
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
