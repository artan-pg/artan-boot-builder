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

/**
 * Defines the isolation levels that control the visibility of changes made by
 * concurrent transactions.
 *
 * <p>Isolation levels determine the degree to which a transaction is isolated
 * from the effects of other concurrent transactions. Higher isolation levels
 * provide stronger consistency guarantees but usually reduce concurrency.
 *
 * <p>The numeric codes correspond to the standard JDBC isolation constants
 * (except {@link #DEFAULT}, which signals that the underlying data source's
 * default isolation should be used).
 *
 * <ul>
 *   <li>{@link #READ_UNCOMMITTED} – lowest isolation; dirty reads,
 *       non-repeatable reads and phantom reads are possible.</li>
 *   <li>{@link #READ_COMMITTED} – prevents dirty reads; non-repeatable reads
 *       and phantom reads may still occur.</li>
 *   <li>{@link #REPEATABLE_READ} – prevents dirty and non-repeatable reads;
 *       phantom reads remain possible.</li>
 *   <li>{@link #SERIALIZABLE} – highest isolation; completely isolates the
 *       transaction, preventing dirty, non-repeatable and phantom reads.</li>
 *   <li>{@link #DEFAULT} – uses the isolation level configured on the
 *       underlying connection / data source.</li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @see TransactionDefinition
 * @see TransactionPropagation
 * @since 0.1.0
 */
public enum TransactionIsolation {

    /**
     * Allows a transaction to read uncommitted changes made by other
     * transactions (dirty reads). Also permits non-repeatable reads and
     * phantom reads.
     *
     * <p>Corresponds to JDBC {@code Connection.TRANSACTION_READ_UNCOMMITTED}.
     */
    READ_UNCOMMITTED(1),

    /**
     * Prevents dirty reads. A transaction can only see changes that have been
     * committed by other transactions. Non-repeatable reads and phantom reads
     * are still possible.
     *
     * <p>Corresponds to JDBC {@code Connection.TRANSACTION_READ_COMMITTED}.
     * This is the default isolation level for most relational databases.
     */
    READ_COMMITTED(2),

    /**
     * Prevents dirty reads and non-repeatable reads. Once a row has been read,
     * subsequent reads within the same transaction are guaranteed to return
     * the same data. Phantom reads may still occur.
     *
     * <p>Corresponds to JDBC {@code Connection.TRANSACTION_REPEATABLE_READ}.
     */
    REPEATABLE_READ(4),

    /**
     * Highest isolation level. Completely serializes access so that concurrent
     * transactions appear to execute one after another. Prevents dirty reads,
     * non-repeatable reads and phantom reads.
     *
     * <p>Corresponds to JDBC {@code Connection.TRANSACTION_SERIALIZABLE}.
     */
    SERIALIZABLE(8),

    /**
     * Indicates that the isolation level of the underlying data source /
     * connection should be used. No explicit isolation is requested by the
     * framework.
     *
     * <p>The numeric code {@code -1} is a sentinel value that does not map to
     * a JDBC constant.
     */
    DEFAULT(-1);

    private final int code;

    TransactionIsolation(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric code associated with this isolation level.
     *
     * <p>For the standard levels the code matches the corresponding JDBC
     * constant. {@link #DEFAULT} returns {@code -1}.
     *
     * @return the isolation code
     */
    public int getCode() {
        return code;
    }
}
