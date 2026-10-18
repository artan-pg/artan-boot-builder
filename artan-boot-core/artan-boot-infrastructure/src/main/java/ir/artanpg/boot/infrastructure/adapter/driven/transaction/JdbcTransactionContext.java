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

package ir.artanpg.boot.infrastructure.adapter.driven.transaction;

import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import org.intellij.lang.annotations.MagicConstant;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;

/**
 * Thread-bound JDBC connection used by resource-aware adapters.
 *
 * <p>{@link JdbcTransactionManager} binds the connection when a transaction
 * begins. Adapters such as {@code JdbcOutboxStore} can then participate in the
 * same unit of work instead of opening an unrelated auto-commit connection.
 *
 * @author Mohammad Yazdian
 * @see JdbcTransactionManager
 * @since 0.1.0
 */
public final class JdbcTransactionContext {

    private static final ThreadLocal<ConnectionState> CURRENT = new ThreadLocal<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private JdbcTransactionContext() {
        throw new UnsupportedOperationException();
    }

    /**
     * Binds a connection to the current thread.
     *
     * @param state connection state
     * @throws TransactionalDomainException if state is {@code null}
     */
    public static void bind(@NonNull ConnectionState state) {
        if (state == null) throw new TransactionalDomainException("The connection state cannot be null");
        CURRENT.set(state);
    }

    /**
     * Returns the connection bound to the current thread.
     *
     * @return the bound connection, or {@code null}
     */
    @Nullable
    public static Connection getConnection() {
        ConnectionState state = CURRENT.get();
        return state != null ? state.connection() : null;
    }

    /**
     * Returns the state bound to the current thread.
     *
     * @return the bound state, or {@code null}
     */
    @Nullable
    public static ConnectionState getState() {
        return CURRENT.get();
    }

    /**
     * Removes the current thread's binding.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * The connection and the state that must be restored before returning it
     * to the pool.
     *
     * @param connection the transaction connection
     * @param autoCommit the original auto-commit flag
     * @param readOnly   the original read-only flag
     * @param isolation  the original transaction isolation level
     */
    public record ConnectionState(@NonNull Connection connection,
                                  boolean autoCommit,
                                  boolean readOnly,
                                  @MagicConstant(intValues = {
                                          Connection.TRANSACTION_NONE,
                                          Connection.TRANSACTION_READ_UNCOMMITTED,
                                          Connection.TRANSACTION_READ_COMMITTED,
                                          Connection.TRANSACTION_REPEATABLE_READ,
                                          Connection.TRANSACTION_SERIALIZABLE
                                  }) int isolation) {
    }
}
