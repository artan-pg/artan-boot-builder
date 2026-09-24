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

import java.io.Serial;
import java.util.StringJoiner;

/**
 * Abstract base class for aggregate members (entities or value objects) that
 * belong to an aggregate root.
 *
 * <p>This class provides common functionality and state, such as the unique
 * identifier, for objects that are part of an aggregate but are not the
 * aggregate root themselves.
 *
 * @param <I> the type of the identifier for this aggregate member
 * @author Mohammad Yazdian
 * @see AggregateRoot
 * @since 0.1.0
 */
public abstract class AbstractAggregateMember<I extends Identifier<?>> implements AggregateMember<I> {

    @Serial
    private static final long serialVersionUID = -5967794519763090772L;

    private static final String IDENTIFIER_NULL_EXCEPTION = "The identifier cannot be null";

    /**
     * The unique identifier of this aggregate member.
     */
    private final I id;

    /**
     * Constructs a new aggregate member with the specified identifier.
     *
     * <p>The constructor initializes the domain event collection and
     * associates the aggregate member with its unique identifier.
     *
     * @param id the unique identifier for this aggregate
     * @throws DomainException if the id is {@code null}
     */
    protected AbstractAggregateMember(I id) {
        if (id == null) throw new DomainException(IDENTIFIER_NULL_EXCEPTION);

        this.id = id;
    }

    /**
     * Protected constructor for creating an aggregate member instance using a
     * builder.
     *
     * <p>This constructor is designed to be called from concrete aggregate
     * member constructors that receive their corresponding builder instances.
     * It extracts the identifier from the builder and sets it on the aggregate
     * member.
     *
     * @param builder the builder containing the aggregate member's identifier
     * @throws DomainException if the builder is {@code null}
     */
    protected AbstractAggregateMember(AbstractBuilder<I, ?, ?> builder) {
        if (builder == null) throw new DomainException("The builder cannot be null");
        this.id = builder.id;
    }

    @Override
    public I getId() {
        return this.id;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof AbstractAggregateMember<?> that)) return false;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AbstractAggregateMember.class.getSimpleName() + "[", "]")
                .add("id=" + id.value())
                .toString();
    }

    /**
     * Abstract builder base class for constructing domain aggregate member
     * instances.
     *
     * <p>This builder provides a fluent API for building aggregate member
     * domains with type-safe generic parameters. It handles the common pattern
     * of setting an identifier and delegates the actual construction to
     * concrete subclasses.
     *
     * @param <I> the type of identifier used by the aggregate member
     * @param <C> the concrete aggregate member class being built
     * @param <B> the concrete builder class
     * @author Mohammad Yazdian
     * @see AbstractAggregateMember
     * @since 0.1.0
     */
    protected abstract static class AbstractBuilder<I extends Identifier<?>, C extends AbstractAggregateMember<I>,
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
         * Sets the unique identifier for the aggregate member being built.
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
