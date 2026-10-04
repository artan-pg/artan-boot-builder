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
 * Defines locking strategies for concurrent access control within
 * transactions.
 *
 * <p>This enumeration specifies how resources (typically database rows or
 * entities) should be locked when accessed inside a transactional boundary.
 * The chosen strategy influences concurrency behavior, consistency guarantees,
 * and potential for deadlocks or optimistic locking failures.
 *
 * <p>Supported modes align with common JPA / JDBC locking semantics:
 * <ul>
 *   <li>{@link #NONE} – no explicit lock is requested; the underlying
 *       persistence provider or database uses its default isolation behavior.
 *       </li>
 *   <li>{@link #PESSIMISTIC_READ} – acquires a shared (read) lock that
 *       prevents concurrent writers but still allows other readers.</li>
 *   <li>{@link #PESSIMISTIC_WRITE} – acquires an exclusive (write) lock that
 *       blocks both concurrent readers and writers.</li>
 *   <li>{@link #OPTIMISTIC} – relies on a version column (or equivalent) to
 *       detect concurrent modifications; no database-level lock is taken.</li>
 *   <li>{@link #OPTIMISTIC_FORCE_INCREMENT} – same as optimistic locking but
 *       forces an increment of the version even when the entity itself is not
 *       modified.</li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @see TransactionDefinition
 * @see TransactionIsolation
 * @since 0.1.0
 */
public enum LockMode {

    /**
     * No explicit locking is requested.
     *
     * <p>The persistence provider or database decides the locking behavior
     * according to the current transaction isolation level. This is the least
     * restrictive option and is suitable when concurrency conflicts are rare
     * or handled at a higher level.
     */
    NONE,

    /**
     * Pessimistic read (shared) lock.
     *
     * <p>Prevents concurrent modifications of the locked resource while still
     * allowing other transactions to read it. Useful for scenarios that need a
     * consistent snapshot without blocking readers.
     */
    PESSIMISTIC_READ,

    /**
     * Pessimistic write (exclusive) lock.
     *
     * <p>Blocks both concurrent readers and writers. Provides the strongest
     * consistency guarantee at the cost of reduced concurrency and higher risk
     * of deadlocks.
     */
    PESSIMISTIC_WRITE,

    /**
     * Optimistic locking based on a version field.
     *
     * <p>No database-level lock is acquired. Instead, the entity's version
     * (or equivalent concurrency token) is checked on flush/commit. A
     * concurrent modification results in an optimistic locking failure.
     */
    OPTIMISTIC,

    /**
     * Optimistic locking with forced version increment.
     *
     * <p>Behaves like {@link #OPTIMISTIC} but always increments the version
     * number, even if the entity itself has not been modified. This is useful
     * when related collections or dependent entities must participate in the
     * optimistic check.
     */
    OPTIMISTIC_FORCE_INCREMENT
}
