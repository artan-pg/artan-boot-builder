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
 * Defines locking strategies for concurrent access to resources within a
 * transaction.
 *
 * <p>Each constant describes how a resource should be locked while it is being
 * read or modified. The exact semantics depend on the underlying persistence
 * provider, but the categories are aligned with the standard JPA lock modes.
 *
 * <p>Not all modes are supported by every resource manager. When a mode is not
 * supported, the provider may either ignore it or fail with an exception,
 * depending on its configuration.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public enum LockMode {

    /**
     * No explicit locking.
     *
     * <p>The database default behavior is used. Concurrency control is left
     * entirely to the underlying resource manager.
     */
    NONE,

    /**
     * Pessimistic read lock.
     *
     * <p>Prevents other transactions from writing to the locked resource while
     * allowing them to read it. Shared locks are typically acquired at the
     * database level.
     */
    PESSIMISTIC_READ,

    /**
     * Pessimistic write lock.
     *
     * <p>Prevents other transactions from both reading and writing to the
     * locked resource. Exclusive locks are typically acquired at the database
     * level.
     */
    PESSIMISTIC_WRITE,

    /**
     * Optimistic lock using a version field.
     *
     * <p>Conflicts are detected at commit time by comparing a version value
     * rather than by holding a database lock. This mode is efficient for
     * low-contention workloads.
     */
    OPTIMISTIC,

    /**
     * Optimistic lock with forced version increment.
     *
     * <p>Behaves like {@link #OPTIMISTIC} but additionally forces the version
     * field to be incremented even when no fields were modified. Useful when
     * the mere act of reading should be reflected in the version.
     */
    OPTIMISTIC_FORCE_INCREMENT
}
