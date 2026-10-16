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
 * Defines the propagation behavior of transactions.
 *
 * <p>Propagation controls how a transactional unit of work relates to any
 * transaction that may already be active. The constants mirror the semantics
 * defined by the standard transaction propagation model in enterprise Java.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public enum TransactionPropagation {

    /**
     * Uses the current transaction if one exists; otherwise creates a new
     * transaction.
     *
     * <p>This is the most common propagation mode and is treated as the
     * default by most transaction managers.
     */
    REQUIRED,

    /**
     * Always creates a new transaction.
     *
     * <p>If a transaction is already active, it is suspended for the duration
     * of the new transaction and resumed afterward.
     */
    REQUIRES_NEW,

    /**
     * Executes non-transactionally.
     *
     * <p>If a transaction is already active, it is suspended for the duration
     * of the call.
     */
    NOT_SUPPORTED,

    /**
     * Uses the current transaction if one exists; otherwise executes
     * non-transactionally.
     */
    SUPPORTS,

    /**
     * Uses the current transaction; throws an exception if no transaction is
     * active.
     *
     * <p>Useful for enforcing that a transactional context must already be
     * present.
     */
    MANDATORY,

    /**
     * Executes non-transactionally; throws an exception if a transaction is
     * active.
     *
     * <p>Used to enforce that no transaction is present when the method is
     * invoked.
     */
    NEVER
}
