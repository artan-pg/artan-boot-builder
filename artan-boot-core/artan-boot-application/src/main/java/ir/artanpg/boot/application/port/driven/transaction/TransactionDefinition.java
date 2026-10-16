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

package ir.artanpg.boot.application.port.driven.transaction;

import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.jspecify.annotations.NonNull;

import java.time.Duration;

/**
 * Defines transaction properties and behavior.
 *
 * <p>This class is an immutable value object that carries the configuration
 * used when starting a transaction. Instances are created through the
 * {@link Builder} and validated by construction.
 *
 * <p>Each property has a sensible default so that only the fields that differ
 * from the defaults need to be supplied. Use {@link #withDefaults()} to obtain
 * a definition configured entirely with default values.
 *
 * @author Mohammad Yazdian
 * @see TransactionPropagation
 * @see TransactionIsolation
 * @see Builder
 * @since 0.1.0
 */
public final class TransactionDefinition {

    /**
     * Default transaction timeout applied when no explicit timeout is
     * configured.
     */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Propagation behavior applied when a transaction is started.
     */
    private final TransactionPropagation propagation;

    /**
     * Isolation level applied when a transaction is started.
     */
    private final TransactionIsolation isolation;

    /**
     * Maximum duration allowed for the transaction.
     */
    private final Duration timeout;

    /**
     * Whether the transaction is flagged as read-only.
     */
    private final boolean readOnly;

    /**
     * Human-readable name used for diagnostic purposes.
     */
    private final String name;

    /**
     * Constructs a definition by copying values from the given builder.
     *
     * @param builder the builder supplying the property values
     */
    private TransactionDefinition(Builder builder) {
        this.propagation = builder.propagation;
        this.isolation = builder.isolation;
        this.timeout = builder.timeout;
        this.readOnly = builder.readOnly;
        this.name = builder.name;
    }

    /**
     * Returns the propagation behavior.
     *
     * @return the propagation mode
     */
    public TransactionPropagation getPropagation() {
        return propagation;
    }

    /**
     * Returns the isolation level.
     *
     * @return the isolation level
     */
    public TransactionIsolation getIsolation() {
        return isolation;
    }

    /**
     * Returns the transaction timeout.
     *
     * @return the timeout duration
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Returns whether the transaction is flagged as read-only.
     *
     * @return {@code true} if the transaction is read-only, {@code false} otherwise
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Returns the transaction name.
     *
     * @return the transaction name
     */
    public String getName() {
        return name;
    }

    /**
     * Creates a definition configured entirely with default values.
     *
     * <p>The returned instance uses {@link TransactionPropagation#REQUIRED},
     * {@link TransactionIsolation#DEFAULT}, a thirty-second timeout, a
     * read-write flag, and the name {@code "default"}.
     *
     * @return a default definition
     */
    public static TransactionDefinition withDefaults() {
        return new Builder().build();
    }

    /**
     * Creates a new builder instance.
     *
     * @return a fresh, empty builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing immutable {@link TransactionDefinition}
     * instances.
     *
     * @author Mohammad Yazdian
     * @since 0.1.0
     */
    public static final class Builder {

        private TransactionPropagation propagation = TransactionPropagation.REQUIRED;
        private TransactionIsolation isolation = TransactionIsolation.DEFAULT;
        private Duration timeout = DEFAULT_TIMEOUT;
        private boolean readOnly;
        private String name = "default";

        /**
         * Constructs an empty builder.
         *
         * <p>The constructor is private. Use {@link #builder()} to obtain a
         * builder instance.
         */
        private Builder() {
        }

        /**
         * Sets the propagation behavior.
         *
         * @param propagation the propagation mode
         * @return this builder instance for method chaining
         * @throws TransactionalDomainException if propagation is {@code null}
         */
        public Builder propagation(@NonNull TransactionPropagation propagation) {
            if (propagation == null) throw new TransactionalDomainException("Propagation must not be null");
            this.propagation = propagation;
            return this;
        }

        /**
         * Sets the isolation level.
         *
         * @param isolation the isolation level
         * @return this builder instance for method chaining
         * @throws TransactionalDomainException if isolation is {@code null}
         */
        public Builder isolation(@NonNull TransactionIsolation isolation) {
            if (isolation == null) throw new TransactionalDomainException("Isolation must not be null");
            this.isolation = isolation;
            return this;
        }

        /**
         * Sets the transaction timeout.
         *
         * @param timeout the timeout duration
         * @return this builder instance for method chaining
         * @throws TransactionalDomainException if timeout is {@code null}, zero, or negative
         */
        public Builder timeout(@NonNull Duration timeout) {
            if (timeout == null) throw new TransactionalDomainException("Timeout must not be null");
            if (timeout.isZero() || timeout.isNegative()) {
                throw new TransactionalDomainException("Timeout must be positive");
            }
            this.timeout = timeout;
            return this;
        }

        /**
         * Sets the read-only flag.
         *
         * @param readOnly {@code true} to flag the transaction as read-only, {@code false} otherwise
         * @return this builder instance for method chaining
         */
        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        /**
         * Sets the transaction name.
         *
         * @param name the transaction name
         * @return this builder instance for method chaining
         * @throws TransactionalDomainException if name is {@code null} or {@code blank}
         */
        public Builder name(@NonNull String name) {
            if (name == null || name.isBlank()) {
                throw new TransactionalDomainException("The name cannot be null or blank");
            }
            this.name = name;
            return this;
        }

        /**
         * Builds an immutable {@link TransactionDefinition}.
         *
         * @return the constructed metadata
         */
        public TransactionDefinition build() {
            return new TransactionDefinition(this);
        }
    }
}
