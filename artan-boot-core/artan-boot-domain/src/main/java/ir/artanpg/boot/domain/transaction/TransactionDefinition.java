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

package ir.artanpg.boot.domain.transaction;

import ir.artanpg.boot.domain.transaction.exception.TransactionException;
import ir.artanpg.boot.domain.model.ValueObject;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Immutable value object that describes the characteristics of a transactional
 * boundary.
 *
 * <p>A {@code TransactionDefinition} encapsulates the core attributes that
 * control how a unit of work should be executed:
 * <ul>
 *   <li>Propagation behavior ({@link TransactionPropagation})</li>
 *   <li>Isolation level ({@link TransactionIsolation})</li>
 *   <li>Timeout duration</li>
 *   <li>Read-only flag</li>
 *   <li>Optional logical name (useful for monitoring and debugging)</li>
 * </ul>
 *
 * <p>Instances are created exclusively through the fluent {@link Builder}.
 * Validation is performed at build time; any missing or invalid attribute
 * results in a {@link TransactionException}.
 *
 * <p>This class implements {@link ValueObject} and is therefore expected to be
 * compared by value rather than by identity. It is also serializable.
 *
 * @author Mohammad Yazdian
 * @see TransactionPropagation
 * @see TransactionIsolation
 * @see LockMode
 * @since 0.1.0
 */
public final class TransactionDefinition implements ValueObject {

    @Serial
    private static final long serialVersionUID = 2805283655943241369L;

    /**
     * Default timeout in seconds used when no explicit timeout is supplied.
     */
    private static final Long TIMEOUT_DEFAULT = 30L;

    /**
     * The propagation behavior that controls how this transaction relates to
     * an existing transactional context (join, create new, suspend, etc.).
     */
    private final TransactionPropagation propagation;

    /**
     * The isolation level that determines the visibility of concurrent changes
     * made by other transactions.
     */
    private final TransactionIsolation isolation;

    /**
     * The maximum duration the transaction is allowed to run before it is
     * automatically timed out and rolled back.
     */
    private final Duration timeout;

    /**
     * Indicates whether the transaction is intended to be read-only.
     * A {@code true} value may enable optimizations in the underlying
     * persistence provider (for example skipping dirty checking).
     */
    private final boolean readOnly;

    /**
     * Logical name of the transaction, primarily used for logging, metrics and
     * debugging purposes.
     */
    private final String name;

    private TransactionDefinition(Builder builder) {
        this.propagation = builder.propagation;
        this.isolation = builder.isolation;
        this.timeout = builder.timeout;
        this.readOnly = builder.readOnly;
        this.name = builder.name;
    }

    /**
     * Returns the configured transaction propagation behavior.
     *
     * @return the propagation mode
     */
    public TransactionPropagation getPropagation() {
        return propagation;
    }

    /**
     * Returns the configured transaction isolation level.
     *
     * @return the isolation level
     */
    public TransactionIsolation getIsolation() {
        return isolation;
    }

    /**
     * Returns the maximum duration the transaction is allowed to run.
     *
     * @return the timeout
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Indicates whether the transaction is intended to be read-only.
     *
     * <p>A read-only transaction may enable certain optimizations in the
     * underlying persistence provider (for example, skipping dirty checking or
     * using a read-only connection).
     *
     * @return {@code true} if the transaction is read-only, {@code false} otherwise
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Returns the logical name of the transaction.
     *
     * <p>The name is primarily used for logging, metrics, and debugging
     * purposes.
     *
     * @return the transaction name
     */
    public String getName() {
        return name;
    }

    /**
     * Creates a {@link TransactionDefinition} with sensible defaults.
     *
     * <p>The default values are:
     * <ul>
     *   <li>Propagation: {@link TransactionPropagation#REQUIRED}</li>
     *   <li>Isolation: {@link TransactionIsolation#DEFAULT}</li>
     *   <li>Timeout: 30 seconds</li>
     *   <li>Read-only: {@code false}</li>
     *   <li>Name: {@code "default"}</li>
     * </ul>
     *
     * @return a new definition instance with default values
     */
    public static TransactionDefinition withDefaults() {
        return new Builder().build();
    }

    /**
     * Returns a new builder for constructing a custom
     * {@link TransactionDefinition}.
     *
     * @return a fresh builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransactionDefinition that = (TransactionDefinition) o;
        return readOnly == that.readOnly &&
                propagation == that.propagation &&
                isolation == that.isolation &&
                Objects.equals(timeout, that.timeout) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(propagation);
        result = 31 * result + Objects.hashCode(isolation);
        result = 31 * result + Objects.hashCode(timeout);
        result = 31 * result + Boolean.hashCode(readOnly);
        result = 31 * result + Objects.hashCode(name);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TransactionDefinition.class.getSimpleName() + "[", "]")
                .add("propagation=" + propagation)
                .add("isolation=" + isolation)
                .add("timeout=" + timeout)
                .add("readOnly=" + readOnly)
                .add("name='" + name + "'")
                .toString();
    }

    /**
     * Fluent builder for creating immutable {@link TransactionDefinition}
     * instances.
     *
     * <p>All attributes have reasonable defaults. The {@link #build()} method
     * performs validation and throws {@link TransactionException} when any
     * required attribute is missing or invalid.
     */
    public static final class Builder {
        private TransactionPropagation propagation = TransactionPropagation.REQUIRED;
        private TransactionIsolation isolation = TransactionIsolation.DEFAULT;
        private Duration timeout = Duration.ofSeconds(TIMEOUT_DEFAULT);
        private boolean readOnly;
        private String name = "default";

        /**
         * Private constructor to enforce use of {@link #builder()}.
         */
        private Builder() {
        }

        /**
         * Sets the transaction propagation behavior.
         *
         * @param propagation the desired propagation mode
         * @return this builder for method chaining
         */
        public Builder propagation(@NonNull TransactionPropagation propagation) {
            this.propagation = propagation;
            return this;
        }

        /**
         * Sets the transaction isolation level.
         *
         * @param isolation the desired isolation level
         * @return this builder for method chaining
         */
        public Builder isolation(@NonNull TransactionIsolation isolation) {
            this.isolation = isolation;
            return this;
        }

        /**
         * Sets the maximum duration the transaction may run.
         *
         * @param timeout the timeout duration
         * @return this builder for method chaining
         */
        public Builder timeout(@NonNull Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Marks the transaction as read-only or read-write.
         *
         * @param readOnly {@code true} for a read-only transaction
         * @return this builder for method chaining
         */
        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        /**
         * Sets a logical name for the transaction (useful for monitoring).
         *
         * @param name the transaction name
         * @return this builder for method chaining
         */
        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Builds and returns a fully validated {@link TransactionDefinition}.
         *
         * <p>Validation rules:
         * <ul>
         *   <li>{@code propagation} must not be {@code null}</li>
         *   <li>{@code isolation} must not be {@code null}</li>
         *   <li>{@code timeout} must not be {@code null}</li>
         *   <li>{@code name} must not be {@code null} or blank</li>
         * </ul>
         *
         * @return a new immutable {@code TransactionDefinition}
         * @throws TransactionException if any validation rule is violated
         */
        public TransactionDefinition build() {
            if (propagation == null) throw new TransactionException("The propagation of transaction cannot be null");
            if (isolation == null) throw new TransactionException("The isolation of transaction cannot be null");
            if (timeout == null) throw new TransactionException("The timeout of transaction cannot be null");
            if (name == null || name.isBlank()) {
                throw new TransactionException("The name of transaction cannot be null or blank");
            }
            return new TransactionDefinition(this);
        }
    }
}
