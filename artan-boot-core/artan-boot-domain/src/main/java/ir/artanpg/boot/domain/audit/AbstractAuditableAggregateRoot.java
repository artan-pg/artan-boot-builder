/*
 * Copyright (c) 2026-present the original author or authors.
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

import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.domain.model.AbstractAggregateRoot;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.time.Clock;
import java.time.Instant;
import java.util.StringJoiner;

/**
 * Abstract base implementation of {@link AuditableAggregateRoot} that provides
 * default behavior for audit tracking.
 *
 * <p>This class extends {@link AbstractAggregateRoot} and implements audit
 * functionality by tracking creation and modification information. It uses
 * {@link String} for principal identification and {@link Instant} for
 * timestamps.
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li><b>Immutable Creation Info:</b> {@code createdBy} and
 *       {@code createdDate} are immutable and cannot be changed after
 *       creation.</li>
 *   <li><b>Mutable Modification Info:</b> {@code lastModifiedBy} and
 *       {@code lastModifiedDate} are updated whenever the aggregate is
 *       modified.</li>
 *   <li><b>Identity-Based Equality:</b> Following DDD principles, equality is
 *       based solely on the aggregate's identifier, not on audit fields.</li>
 *   <li><b>Validation:</b> All audit fields are validated to ensure data
 *       integrity.</li>
 * </ul>
 *
 * @param <I> The type of identifier used by this aggregate root
 * @author Mohammad Yazdian
 * @see AuditableAggregateRoot
 * @see AbstractAggregateRoot
 * @since 0.1.0
 */
public abstract class AbstractAuditableAggregateRoot<I extends Identifier<?>>
        extends AbstractAggregateRoot<I> implements AuditableAggregateRoot<String, I> {

    /**
     * Constant for the field name "createdBy".
     *
     * <p>Useful for JPA queries, QueryDSL, and reflection-based operations.
     */
    public static final String CREATED_BY = "createdBy";

    /**
     * Constant for the field name "createdDate".
     *
     * <p>Useful for JPA queries, QueryDSL, and reflection-based operations.
     */
    public static final String CREATED_DATE = "createdDate";

    /**
     * Constant for the field name "lastModifiedBy".
     *
     * <p>Useful for JPA queries, QueryDSL, and reflection-based operations.
     */
    public static final String LAST_MODIFIED_BY = "lastModifiedBy";

    /**
     * Constant for the field name "lastModifiedDate".
     *
     * <p>Useful for JPA queries, QueryDSL, and reflection-based operations.
     */
    public static final String LAST_MODIFIED_DATE = "lastModifiedDate";

    @Serial
    private static final long serialVersionUID = 8720586928464103287L;

    private static final String CREATED_DATE_NULL_EXCEPTION_MSG = "The createdDate cannot be null";
    private static final String LAST_MODIFIED_DATE_NULL_EXCEPTION_MSG = "The lastModifiedDate cannot be null";

    /**
     * The user/system who created this aggregate.
     *
     * <p>This field is immutable and cannot be changed after creation.
     */
    private final String createdBy;

    /**
     * The timestamp when this aggregate was created.
     *
     * <p>This field is immutable and cannot be changed after creation.
     */
    private final Instant createdDate;

    /**
     * The user/system who last modified this aggregate.
     *
     * <p>This field is updated whenever the aggregate is modified.
     */
    private String lastModifiedBy;

    /**
     * The timestamp when this aggregate was last modified.
     *
     * <p>This field is updated whenever the aggregate is modified.
     */
    private Instant lastModifiedDate;

    /**
     * Constructs a new auditable aggregate root with the specified identifier,
     * creator and creation date.
     *
     * <p>Validates that the creator is non-blank and the creation date is not
     * null nor in the future, then stores the instant representation of the
     * date.
     *
     * @param identifier  the unique identifier of the aggregate
     * @param createdBy   the identifier of the user who created the aggregate
     * @param createdDate the clock representing the creation time
     * @throws DomainException if identifier is {@code null}, createdBy is {@code null} or {@code blank},
     *                         or createdDate is {@code null} or in the future
     */
    protected AbstractAuditableAggregateRoot(I identifier, String createdBy, Clock createdDate) {
        super(identifier);

        validateCreatedBy(createdBy);
        validateCreatedDate(createdDate);

        this.createdBy = createdBy;
        this.createdDate = createdDate.instant();
    }

    /**
     * Constructs a new auditable aggregate root with the specified identifier
     * and creation information.
     *
     * <p>This constructor validates the creation information and initializes
     * the audit fields. The modification fields are left {@code null} until
     * the aggregate is first modified.
     *
     * @param identifier  the unique identifier for this aggregate
     * @param createdBy   the user/system who created this aggregate
     * @param createdDate the timestamp when this aggregate was created
     * @throws DomainException if the identifier is {@code null}
     * @throws DomainException if createdBy is {@code null} or {@code blank}
     * @throws DomainException if createdDate is {@code null} or in the future
     */
    protected AbstractAuditableAggregateRoot(I identifier, String createdBy, Instant createdDate) {
        super(identifier);

        validateCreatedBy(createdBy);
        validateCreatedDate(createdDate);

        this.createdBy = createdBy;
        this.createdDate = createdDate;
    }

    /**
     * Protected constructor for creating an auditable aggregate root instance
     * using a builder.
     *
     * <p>This constructor is designed to be called from concrete aggregate
     * root constructors that receive their corresponding builder instances.
     * It extracts audit information from the builder and validates it.
     *
     * @param builder the builder containing the aggregate root's configuration
     * @throws DomainException if the builder is {@code null}
     * @throws DomainException if audit fields are invalid
     */
    protected AbstractAuditableAggregateRoot(AbstractBuilder<I, ?, ?> builder) {
        super(builder);

        validateCreatedBy(builder.createdBy);
        validateCreatedDate(builder.createdDate);

        this.createdBy = builder.createdBy;
        this.createdDate = builder.createdDate;
        this.lastModifiedBy = builder.lastModifiedBy;
        this.lastModifiedDate = builder.lastModifiedDate;
    }

    @Override
    public String getCreatedBy() {
        return this.createdBy;
    }

    @Override
    public Instant getCreatedDate() {
        return this.createdDate;
    }

    @Override
    public String getLastModifiedBy() {
        return this.lastModifiedBy;
    }

    @Override
    public Instant getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    /**
     * Marks this aggregate as modified by the specified user/system.
     *
     * <p>This method updates the {@code lastModifiedBy} and
     * {@code lastModifiedDate} fields. It should be called whenever the
     * aggregate's state is changed.
     *
     * @param modifiedBy the user/system who modified this aggregate
     * @throws DomainException if {@code modifiedBy} is {@code null} or {@code blank}
     */
    public void markAsModified(@NonNull String modifiedBy) {
        markAsModified(modifiedBy, Clock.systemUTC());
    }

    /**
     * Marks the entity as modified by the given user at the specified time,
     * extracting the instant from the provided clock.
     *
     * @param modifiedBy       the identifier of the user who modified the entity
     * @param lastModifiedDate the clock representing the modification time
     */
    public void markAsModified(@NonNull String modifiedBy, @Nullable Clock lastModifiedDate) {
        Clock now = lastModifiedDate == null ? Clock.systemUTC() : lastModifiedDate;
        markAsModified(modifiedBy, now.instant());
    }

    /**
     * Marks this aggregate as modified with an explicit timestamp.
     *
     * <p>Prefer this overload when the modification time is already known
     * (e.g. replaying events or importing historical data).
     *
     * @param modifiedBy       the user/system who modified this aggregate
     * @param lastModifiedDate the modification timestamp
     * @throws DomainException if {@code modifiedBy} is {@code null} or {@code blank}
     * @throws DomainException if {@code modifiedDate} is {@code null}
     */
    public void markAsModified(@NonNull String modifiedBy, @NonNull Instant lastModifiedDate) {
        validateLastModifiedBy(modifiedBy);
        validateLastModifiedDate(lastModifiedDate);

        this.lastModifiedBy = modifiedBy;
        this.lastModifiedDate = lastModifiedDate;
    }

    /**
     * Validates that the {@code createdBy} field is not null or blank.
     *
     * @param createdBy the value to validate
     * @throws DomainException if createdBy is {@code null} or {@code blank}
     */
    protected static void validateCreatedBy(@NonNull String createdBy) {
        if (createdBy == null || createdBy.isBlank()) {
            throw new DomainException("The createdBy cannot be null or blank");
        }
    }

    /**
     * Validates that the created date is not null and not in the
     * future, using the current system time as reference.
     *
     * @param createdDate the date to validate
     * @throws DomainException if createdDate is {@code null} or after the current time
     */
    protected static void validateCreatedDate(@NonNull Clock createdDate) {
        if (createdDate == null) throw new DomainException(CREATED_DATE_NULL_EXCEPTION_MSG);
        validateCreatedDate(createdDate.instant());
    }

    /**
     * Validates that the created date is not null and not in the
     * future, using a custom reference clock.
     *
     * @param createdDate  the date to validate
     * @param referenceNow the reference clock for "now"
     * @throws DomainException if createdDate is {@code null} or after referenceNow
     */
    protected static void validateCreatedDate(@NonNull Clock createdDate, @Nullable Clock referenceNow) {
        if (createdDate == null) throw new DomainException(CREATED_DATE_NULL_EXCEPTION_MSG);

        Clock now = referenceNow == null ? Clock.systemUTC() : referenceNow;
        validateCreatedDate(createdDate.instant(), now.instant());
    }

    /**
     * Validates that the created date is not null and not in the
     * future, using the current system time as reference.
     *
     * @param createdDate the date to validate
     * @throws DomainException if createdDate is {@code null} or after the current time
     */
    protected static void validateCreatedDate(@NonNull Instant createdDate) {
        validateCreatedDate(createdDate, Instant.now());
    }

    /**
     * Validates that the created date is not null and not in the
     * future, using a custom reference instant.
     *
     * <p>This is the core validation method that all other
     * overloads delegate to.
     *
     * @param createdDate  the date to validate
     * @param referenceNow the reference instant for "now"
     * @throws DomainException if createdDate is {@code null} or after referenceNow
     */
    protected static void validateCreatedDate(@NonNull Instant createdDate, @Nullable Instant referenceNow) {
        if (createdDate == null) throw new DomainException(CREATED_DATE_NULL_EXCEPTION_MSG);

        Instant now = referenceNow == null ? Instant.now() : referenceNow;
        if (createdDate.isAfter(now)) throw new DomainException("The createdDate cannot be after now");
    }

    /**
     * Validates that the {@code lastModifiedBy} field is not null or blank.
     *
     * @param lastModifiedBy the value to validate
     * @throws DomainException if lastModifiedBy is {@code null} or {@code blank}
     */
    protected static void validateLastModifiedBy(@NonNull String lastModifiedBy) {
        if (lastModifiedBy == null || lastModifiedBy.isBlank()) {
            throw new DomainException("The lastModifiedBy cannot be null or blank");
        }
    }

    /**
     * Validates that the last modified date is not null and not in the
     * future, using the current system time as reference.
     *
     * @param lastModifiedDate the date to validate
     * @throws DomainException if lastModifiedDate is {@code null} or after the current time
     */
    protected static void validateLastModifiedDate(@NonNull Clock lastModifiedDate) {
        if (lastModifiedDate == null) throw new DomainException(LAST_MODIFIED_DATE_NULL_EXCEPTION_MSG);
        validateLastModifiedDate(lastModifiedDate.instant(), Instant.now());
    }

    /**
     * Validates that the last modified date is not null and not in the
     * future, using a custom reference clock.
     *
     * @param lastModifiedDate the date to validate
     * @param referenceNow     the reference clock for "now"
     * @throws DomainException if lastModifiedDate is {@code null} or after referenceNow
     */
    protected static void validateLastModifiedDate(@NonNull Clock lastModifiedDate, @Nullable Clock referenceNow) {
        if (lastModifiedDate == null) throw new DomainException(LAST_MODIFIED_DATE_NULL_EXCEPTION_MSG);

        Clock now = referenceNow == null ? Clock.systemUTC() : referenceNow;
        validateLastModifiedDate(lastModifiedDate.instant(), now.instant());
    }

    /**
     * Validates that the last modified date is not null and not in the
     * future, using the current system time as reference.
     *
     * @param lastModifiedDate the date to validate
     * @throws DomainException if lastModifiedDate is {@code null} or after the current time
     */
    protected static void validateLastModifiedDate(@NonNull Instant lastModifiedDate) {
        validateLastModifiedDate(lastModifiedDate, Instant.now());
    }

    /**
     * Validates that the last modified date is not null and not in the
     * future, using a custom reference instant.
     *
     * <p>This is the core validation method that all other overloads
     * delegate to.
     *
     * @param lastModifiedDate the date to validate
     * @param referenceNow     the reference instant for "now"
     * @throws DomainException if lastModifiedDate is {@code null} or after referenceNow
     */
    protected static void validateLastModifiedDate(@NonNull Instant lastModifiedDate, @Nullable Instant referenceNow) {
        if (lastModifiedDate == null) throw new DomainException(LAST_MODIFIED_DATE_NULL_EXCEPTION_MSG);

        Instant now = referenceNow == null ? Instant.now() : referenceNow;
        if (lastModifiedDate.isAfter(now)) throw new DomainException("The lastModifiedDate cannot be after now");
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("id=" + getId().value())
                .add("createdBy='" + this.createdBy + "'")
                .add("createdDate=" + this.createdDate)
                .add("lastModifiedBy='" + this.lastModifiedBy + "'")
                .add("lastModifiedDate=" + this.lastModifiedDate)
                .toString();
    }

    /**
     * Abstract builder base class for constructing auditable aggregate root
     * instances.
     *
     * <p>This builder extends {@link AbstractAggregateRoot.AbstractBuilder}
     * and adds audit-related fields. It provides a fluent API for building
     * auditable aggregate root entities with type-safe generic parameters.
     *
     * @param <I> the type of identifier used by the aggregate root
     * @param <C> the concrete aggregate root class being built
     * @param <B> the concrete builder class
     * @author Mohammad Yazdian
     * @see AbstractAuditableAggregateRoot
     * @since 0.1.0
     */
    public abstract static class AbstractBuilder<
            I extends Identifier<?>,
            C extends AbstractAuditableAggregateRoot<I>,
            B extends AbstractBuilder<I, C, B>>
            extends AbstractAggregateRoot.AbstractBuilder<I, C, B> {

        private String createdBy;
        private Instant createdDate;
        private String lastModifiedBy;
        private Instant lastModifiedDate;

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
         * Sets the user/system who created the aggregate.
         *
         * @param createdBy the creator identifier
         * @return this builder instance for method chaining
         */
        public B createdBy(final String createdBy) {
            this.createdBy = createdBy;
            return this.self();
        }

        /**
         * Sets the timestamp when the aggregate was created.
         *
         * @param createdDate the creation timestamp
         * @return this builder instance for method chaining
         */
        public B createdDate(final Instant createdDate) {
            this.createdDate = createdDate;
            return this.self();
        }

        /**
         * Sets the timestamp when the aggregate was created.
         *
         * @param createdDate the creation timestamp
         * @return this builder instance for method chaining
         */
        public B createdDate(final Clock createdDate) {
            this.createdDate = (createdDate == null ? Clock.systemUTC() : createdDate).instant();
            return this.self();
        }

        /**
         * Sets the user/system who last modified the aggregate.
         *
         * @param lastModifiedBy the last modifier identifier
         * @return this builder instance for method chaining
         */
        public B lastModifiedBy(final String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return this.self();
        }

        /**
         * Sets the timestamp when the aggregate was last modified.
         *
         * @param lastModifiedDate the last modification timestamp
         * @return this builder instance for method chaining
         */
        public B lastModifiedDate(final Instant lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this.self();
        }

        /**
         * Sets the timestamp when the aggregate was last modified.
         *
         * @param lastModifiedDate the last modification timestamp
         * @return this builder instance for method chaining
         */
        public B lastModifiedDate(final Clock lastModifiedDate) {
            this.lastModifiedDate = (lastModifiedDate == null ? Clock.systemUTC() : lastModifiedDate).instant();
            return this.self();
        }
    }
}
