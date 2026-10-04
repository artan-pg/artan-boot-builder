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
 * Defines the propagation behaviors that control how a transactional method
 * participates in an existing transaction or starts a new one.
 *
 * <p>Propagation determines the relationship between the calling transactional
 * context and the current method. The available options mirror the classic
 * Spring / JTA propagation models.
 *
 * <ul>
 *   <li>{@link #REQUIRED} – join an existing transaction or create a new one
 *       if none exists (most common default).</li>
 *   <li>{@link #SUPPORTS} – join an existing transaction if present; otherwise
 *       execute non-transactionally.</li>
 *   <li>{@link #MANDATORY} – require an existing transaction; throw an
 *       exception if none is active.</li>
 *   <li>{@link #REQUIRES_NEW} – always start a new independent transaction,
 *       suspending any existing one.</li>
 *   <li>{@link #NOT_SUPPORTED} – execute non-transactionally, suspending any
 *       existing transaction.</li>
 *   <li>{@link #NEVER} – execute non-transactionally; throw an exception if a
 *       transaction is already active.</li>
 *   <li>{@link #NESTED} – execute within a nested transaction (savepoint) if a
 *       transaction already exists; otherwise behave like {@link #REQUIRED}.
 *       </li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @see TransactionDefinition
 * @see TransactionIsolation
 * @since 0.1.0
 */
public enum TransactionPropagation {

    /**
     * Support a current transaction; create a new one if none exists.
     *
     * <p>This is the most commonly used propagation and is the default in most
     * frameworks. The method always runs inside a transactional context.
     */
    REQUIRED(0),

    /**
     * Support a current transaction; execute non-transactionally if none
     * exists.
     *
     * <p>Useful for methods that can optionally participate in a larger unit
     * of work but do not require their own transaction.
     */
    SUPPORTS(1),

    /**
     * Support a current transaction; throw an exception if none exists.
     *
     * <p>Guarantees that the method only runs when called from an already
     * transactional context.
     */
    MANDATORY(2),

    /**
     * Create a new transaction, suspending the current transaction if one
     * exists.
     *
     * <p>The new transaction is completely independent; its commit or rollback
     * does not affect the suspended outer transaction.
     */
    REQUIRES_NEW(3),

    /**
     * Execute non-transactionally, suspending the current transaction if one
     * exists.
     *
     * <p>Any existing transaction is suspended for the duration of the method
     * and resumed afterward.
     */
    NOT_SUPPORTED(4),

    /**
     * Execute non-transactionally; throw an exception if a transaction exists.
     *
     * <p>Useful for code that must never run inside a transactional context.
     */
    NEVER(5),

    /**
     * Execute within a nested transaction if a current transaction exists,
     * otherwise behave like {@link #REQUIRED}.
     *
     * <p>Nested transactions are typically implemented via database savepoint.
     * A rollback of the nested transaction only undoes work performed inside
     * it; the outer transaction remains active.
     */
    NESTED(6);

    private final int code;

    TransactionPropagation(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric code associated with this propagation behavior.
     *
     * <p>The codes correspond to the classic Spring / JTA propagation
     * constants.
     *
     * @return the propagation code
     */
    public int getCode() {
        return code;
    }
}
