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

/**
 * Defines transaction isolation levels.
 *
 * <p>Isolation levels control the visibility of changes made by other
 * concurrent transactions. Lower isolation permits more concurrency but
 * exposes the transaction to a broader class of anomalies; higher isolation
 * prevents more anomalies at the cost of reduced concurrency.
 *
 * <p>The constants are ordered from the weakest to the strongest isolation
 * guarantee, with {@link #DEFAULT} deferring the choice to the underlying
 * resource manager.
 *
 * @author Mohammad Yazdian
 * @see java.sql.Connection
 * @since 0.1.0
 */
public enum TransactionIsolation {

    /**
     * The weakest isolation level.
     *
     * <p>Dirty reads, non-repeatable reads, and phantom reads are all
     * possible. Rarely used in production systems.
     */
    READ_UNCOMMITTED,

    /**
     * Prevents dirty reads.
     *
     * <p>Non-repeatable reads and phantom reads remain possible. This is the
     * default level for many database systems.
     */
    READ_COMMITTED,

    /**
     * Prevents dirty reads and non-repeatable reads.
     *
     * <p>Phantom reads remain possible.
     */
    REPEATABLE_READ,

    /**
     * The strongest isolation level.
     *
     * <p>Prevents dirty reads, non-repeatable reads, and phantom reads.
     * Transactions behave as if they were executed serially.
     */
    SERIALIZABLE,

    /**
     * Uses the database default isolation level.
     *
     * <p>The effective isolation is determined by the underlying resource
     * manager configuration.
     */
    DEFAULT
}
