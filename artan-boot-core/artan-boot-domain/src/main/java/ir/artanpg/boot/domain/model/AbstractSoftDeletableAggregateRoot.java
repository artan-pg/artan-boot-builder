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

import ir.artanpg.boot.domain.exception.BusinessValidationException;
import ir.artanpg.boot.domain.exception.DomainException;

import java.io.Serial;
import java.util.StringJoiner;

/**
 * Abstract base implementation of {@link SoftDeletableAggregateRoot} that
 * provides default behavior for soft deletion.
 *
 * <p>This class extends {@link AbstractAggregateRoot} and adds logical
 * deletion capability. A soft-deleted aggregate remains in the data store but
 * is excluded from standard queries.
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li><b>Idempotency:</b> {@link #markAsDeleted()} is idempotent and can be
 *       called multiple times safely.</li>
 *   <li><b>Restoration:</b> A soft-deleted aggregate can be restored via
 *       {@link #restore()} if {@link #isRestorable()} returns {@code true}.
 *       </li>
 *   <li><b>Identity-Based Equality:</b> Equality is based solely on the
 *       aggregate's identifier, not on deletion state.</li>
 * </ul>
 *
 * @param <I> The type of identifier used by this aggregate root
 * @author Mohammad Yazdian
 * @see SoftDeletableAggregateRoot
 * @see AbstractAggregateRoot
 * @since 0.1.0
 */
public abstract class AbstractSoftDeletableAggregateRoot<I extends Identifier<?>> extends AbstractAggregateRoot<I>
        implements SoftDeletableAggregateRoot<I> {

    @Serial
    private static final long serialVersionUID = 3918274650183726495L;

    /**
     * Flag indicating whether this domain has been soft-deleted.
     */
    private boolean deleted;

    /**
     * Constructs a new soft-deletable aggregate root with the specified
     * identifier.
     *
     * <p>The aggregate is initialized in a non-deleted state.
     *
     * @param id the unique identifier for this aggregate
     * @throws DomainException if the id is {@code null}
     */
    protected AbstractSoftDeletableAggregateRoot(I id) {
        super(id);
        this.deleted = false;
    }

    /**
     * Protected constructor for creating an instance using a builder.
     *
     * @param builder the builder containing configuration
     * @throws DomainException if the builder is {@code null}
     */
    protected AbstractSoftDeletableAggregateRoot(AbstractBuilder<I, ?, ?> builder) {
        super(builder);
        this.deleted = builder.deleted;
    }

    @Override
    public boolean isDeleted() {
        return this.deleted;
    }

    @Override
    public void markAsDeleted() {
        this.deleted = true;
    }

    @Override
    public boolean isRestorable() {
        return this.deleted;
    }

    @Override
    public void restore() {
        if (!isRestorable()) throw new BusinessValidationException("Aggregate is not restorable");
        this.deleted = false;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("id=" + getId().value())
                .add("deleted=" + this.deleted)
                .toString();
    }

    /**
     * Abstract builder for constructing soft-deletable aggregate root instances.
     *
     * @param <I> the type of identifier
     * @param <C> the concrete aggregate root class
     * @param <B> the concrete builder class
     * @author Mohammad Yazdian
     * @since 0.1.0
     */
    public abstract static class AbstractBuilder<
            I extends Identifier<?>,
            C extends AbstractSoftDeletableAggregateRoot<I>,
            B extends AbstractBuilder<I, C, B>>
            extends AbstractAggregateRoot.AbstractBuilder<I, C, B> {

        private boolean deleted;

        /**
         * Default constructor.
         */
        protected AbstractBuilder() {
            super();
        }

        /**
         * Sets the deletion state.
         *
         * @param deleted the deletion state
         * @return this builder for method chaining
         */
        public B deleted(final boolean deleted) {
            this.deleted = deleted;
            return this.self();
        }
    }
}
