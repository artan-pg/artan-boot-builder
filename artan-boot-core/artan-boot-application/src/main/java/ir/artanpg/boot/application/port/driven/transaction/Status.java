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
 * Represents the lifecycle state of a transaction.
 *
 * <p>Each constant carries a numeric code that is compatible with the status
 * codes defined by the JTA specification.
 *
 * @author Mohammad Yazdian
 * @see TransactionSynchronization
 * @since 0.1.0
 */
public enum Status {

    /**
     * The transaction is currently active and has not yet been marked for
     * rollback or prepared.
     */
    ACTIVE(0),

    /**
     * The transaction has been marked for rollback.
     *
     * <p>Any attempt to commit the transaction will result in a rollback
     * instead.
     */
    MARKED_ROLLBACK(1),

    /**
     * The transaction has been committed successfully.
     */
    COMMITTED(3),

    /**
     * The transaction has been rolled back.
     */
    ROLLED_BACK(4),

    /**
     * The transaction status is unknown.
     *
     * <p>This status is typically used when the transaction manager cannot
     * determine the actual state of the transaction.
     */
    UNKNOWN(5),

    /**
     * No transaction is currently associated with the calling thread.
     */
    NO_TRANSACTION(6);

    /**
     * Numeric status code associated with this constant.
     */
    private final int code;

    /**
     * Constructs a status constant with the given numeric code.
     *
     * @param code the numeric code to associate with this constant
     */
    Status(int code) {
        this.code = code;
    }

    /**
     * Returns the numeric code associated with this status.
     *
     * @return the numeric status code
     */
    public int getCode() {
        return code;
    }
}
