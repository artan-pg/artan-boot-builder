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

package ir.artanpg.boot.domain.audit;

import ir.artanpg.boot.domain.exception.BusinessValidationException;
import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.domain.model.Identifier;

import java.io.Serial;
import java.time.Instant;
import java.util.StringJoiner;

/**
 * Abstract base implementation of {@link SoftDeletableAuditableAggregateRoot}
 * that combines soft deletion with audit tracking.
 *
 * <p>This class extends {@link AbstractAuditableAggregateRoot} and adds
 * logical deletion capability with deleter tracking. When an aggregate is
 * soft-deleted via {@link #markAsDeleted(String)}, the deleter identity
 * and deletion timestamp are recorded.
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li><b>Idempotency:</b> Deletion methods are idempotent.</li>
 *   <li><b>Deleter Tracking:</b> Records who deleted the aggregate and when.
 *       </li>
 *   <li><b>Restoration:</b> A deleted aggregate can be restored via
 *       {@link #restore()}.</li>
 * </ul>
 *
 * @param <I> The type of identifier used by this aggregate root
 * @author Mohammad Yazdian
 * @see SoftDeletableAuditableAggregateRoot
 * @see AbstractAuditableAggregateRoot
 * @since 0.1.0
 */
public abstract class AbstractSoftDeletableAuditableAggregateRoot<I extends Identifier<?>>
        extends AbstractAuditableAggregateRoot<I>
        implements SoftDeletableAuditableAggregateRoot<String, I> {

    @Serial
    private static final long serialVersionUID = 5029384716250483917L;

    /**
     * Flag indicating whether this domain has been soft-deleted.
     */
    private boolean deleted;

    /**
     * The username or identifier of the user who performed the soft deletion.
     */
    private String deletedBy;

    /**
     * The timestamp when the soft deletion occurred.
     */
    private Instant deletedDate;

    /**
     * Constructs a new instance with identifier and creation info.
     *
     * <p>The aggregate is initialized in a non-deleted state.
     *
     * @param identifier  the unique identifier
     * @param createdBy   the creator
     * @param createdDate the creation timestamp
     * @throws DomainException if any parameter is invalid
     */
    protected AbstractSoftDeletableAuditableAggregateRoot(I identifier, String createdBy, Instant createdDate) {
        super(identifier, createdBy, createdDate);
        this.deleted = false;
    }

    /**
     * Protected constructor for creating an instance using a builder.
     *
     * @param builder the builder containing configuration
     * @throws DomainException if builder or audit fields are invalid
     */
    protected AbstractSoftDeletableAuditableAggregateRoot(AbstractBuilder<I, ?, ?> builder) {
        super(builder);
        this.deleted = builder.deleted;
        this.deletedBy = builder.deletedBy;
        this.deletedDate = builder.deletedDate;
    }

    @Override
    public boolean isDeleted() {
        return this.deleted;
    }

    @Override
    public void markAsDeleted() {
        this.deleted = true;
    }

    /**
     * Marks this aggregate as deleted and records deleter information.
     *
     * <p>This method is idempotent. If the aggregate is already deleted,
     * subsequent calls have no effect.
     *
     * @param deletedBy the user/system who deleted this aggregate
     * @throws DomainException if deletedBy is {@code null} or {@code blank}
     */
    public void markAsDeleted(String deletedBy) {
        if (!this.deleted) {
            validateDeletedBy(deletedBy);
            this.deleted = true;
            this.deletedBy = deletedBy;
            this.deletedDate = Instant.now();
        }
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
    public String getDeletedBy() {
        return this.deletedBy;
    }

    @Override
    public Instant getDeletedDate() {
        return this.deletedDate;
    }

    /**
     * Validates that the deletedBy field is not null or blank.
     *
     * @param deletedBy the value to validate
     * @throws DomainException if deletedBy is {@code null} or {@code blank}
     */
    protected static void validateDeletedBy(String deletedBy) {
        if (deletedBy == null || deletedBy.isBlank()) {
            throw new DomainException("The deletedBy cannot be null or blank");
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("id=" + getId().value())
                .add("createdBy='" + getCreatedBy() + "'")
                .add("createdDate=" + getCreatedDate())
                .add("lastModifiedBy='" + this.getLastModifiedBy() + "'")
                .add("lastModifiedDate=" + this.getLastModifiedDate())
                .add("deleted=" + this.deleted)
                .add("deletedBy='" + this.deletedBy + "'")
                .add("deletedDate=" + this.deletedDate)
                .toString();
    }

    /**
     * Abstract builder for constructing instances.
     *
     * @param <I> the type of identifier
     * @param <C> the concrete aggregate root class
     * @param <B> the concrete builder class
     * @author Mohammad Yazdian
     * @since 0.1.0
     */
    public abstract static class AbstractBuilder<
            I extends Identifier<?>,
            C extends AbstractSoftDeletableAuditableAggregateRoot<I>,
            B extends AbstractBuilder<I, C, B>>
            extends AbstractAuditableAggregateRoot.AbstractBuilder<I, C, B> {

        private boolean deleted;
        private String deletedBy;
        private Instant deletedDate;

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

        /**
         * Sets the deleter identifier.
         *
         * @param deletedBy the deleter
         * @return this builder for method chaining
         */
        public B deletedBy(final String deletedBy) {
            this.deletedBy = deletedBy;
            return this.self();
        }

        /**
         * Sets the deletion timestamp.
         *
         * @param deletedDate the deletion timestamp
         * @return this builder for method chaining
         */
        public B deletedDate(final Instant deletedDate) {
            this.deletedDate = deletedDate;
            return this.self();
        }
    }
}
