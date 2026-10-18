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

import ir.artanpg.boot.application.port.driven.transaction.TransactionDefinition;
import ir.artanpg.boot.application.port.driven.transaction.TransactionIsolation;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import ir.artanpg.boot.infrastructure.adapter.driven.transaction.JdbcTransactionContext.ConnectionState;
import org.intellij.lang.annotations.MagicConstant;
import org.jspecify.annotations.NonNull;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

/**
 * JDBC resource-aware transaction manager.
 *
 * <p>Acquires one connection per transaction, disables auto-commit, applies
 * isolation and read-only settings, and binds the connection to
 * {@link JdbcTransactionContext}. Resource adapters such as
 * {@code JdbcOutboxStore} therefore append and update rows on the same
 * connection and in the same transaction as the aggregate change.
 *
 * @author Mohammad Yazdian
 * @see JdbcTransactionContext
 * @since 0.1.0
 */
public class JdbcTransactionManager extends AbstractResourceTransactionManager {

    private static final System.Logger LOGGER = System.getLogger(JdbcTransactionManager.class.getName());

    /**
     * Connection factory.
     */
    private final DataSource dataSource;

    /**
     * Creates a manager for the given data source.
     *
     * @param dataSource connection factory
     * @throws TransactionalDomainException if dataSource is {@code null}
     */
    public JdbcTransactionManager(@NonNull DataSource dataSource) {
        if (dataSource == null) throw new TransactionalDomainException("The dataSource must not be null");

        this.dataSource = dataSource;
    }

    @Override
    protected void doBegin(TransactionDefinition definition, SimpleTransactionStatus status) {
        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            boolean autoCommit = connection.getAutoCommit();
            boolean readOnly = connection.isReadOnly();
            int isolation = connection.getTransactionIsolation();

            connection.setAutoCommit(false);
            if (definition.isReadOnly()) connection.setReadOnly(true);
            int jdbcIsolation = toJdbcIsolation(definition.getIsolation());
            if (definition.getIsolation() != TransactionIsolation.DEFAULT) {
                connection.setTransactionIsolation(jdbcIsolation);
            }

            JdbcTransactionContext.bind(new ConnectionState(connection, autoCommit, readOnly, isolation));
        } catch (SQLException ex) {
            closeQuietly(connection);
            throw new TransactionalDomainException("Failed to begin JDBC transaction", ex);
        }
    }

    @Override
    protected void doFlush(SimpleTransactionStatus status) {
        // The unit of work is flushed by the caller's SQL; nothing to buffer here.
    }

    @SuppressWarnings("resource")
    @Override
    protected void onCommitted(SimpleTransactionStatus status) {
        ConnectionState state = JdbcTransactionContext.getState();
        if (state == null) return;

        try {
            state.connection().commit();
        } catch (SQLException ex) {
            throw new TransactionalDomainException("Failed to commit JDBC transaction", ex);
        } finally {
            restoreAndClose(state);
        }
    }

    @SuppressWarnings("resource")
    @Override
    protected void onRolledBack(SimpleTransactionStatus status) {
        ConnectionState state = JdbcTransactionContext.getState();
        if (state == null) return;

        try {
            state.connection().rollback();
        } catch (SQLException ex) {
            throw new TransactionalDomainException("Failed to roll back JDBC transaction", ex);
        } finally {
            restoreAndClose(state);
        }
    }

    @Override
    protected void logSyncError(String phase, Exception ex) {
        LOGGER.log(WARNING, "Transaction synchronization failed in phase " + phase, ex);
    }

    /**
     * Restores connection flags and returns the connection to the pool.
     *
     * @param state the transaction connection state
     */
    private static void restoreAndClose(ConnectionState state) {
        Connection connection = state.connection();
        try {
            connection.setAutoCommit(state.autoCommit());
        } catch (SQLException ex) {
            logRestoreFailure("autoCommit", ex);
        }
        try {
            connection.setReadOnly(state.readOnly());
        } catch (SQLException ex) {
            logRestoreFailure("readOnly", ex);
        }
        if (state.isolation() != Connection.TRANSACTION_NONE) {
            try {
                connection.setTransactionIsolation(state.isolation());
            } catch (SQLException ex) {
                logRestoreFailure("isolation", ex);
            }
        }
        closeQuietly(connection);
        JdbcTransactionContext.clear();
    }

    /**
     * Logs a failure while restoring connection flags.
     *
     * @param attribute attribute being restored
     * @param ex        failure
     */
    private static void logRestoreFailure(String attribute, SQLException ex) {
        LOGGER.log(ERROR, "Failed to restore JDBC connection attribute " + attribute, ex);
    }

    /**
     * Closes a connection without masking an earlier failure.
     *
     * @param connection connection to close, possibly {@code null}
     */
    private static void closeQuietly(Connection connection) {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException ex) {
            LOGGER.log(ERROR, "Failed to close JDBC connection", ex);
        }
    }

    /**
     * Maps the application isolation level to a JDBC constant.
     *
     * @param isolation application isolation level
     * @return JDBC isolation constant
     */
    @MagicConstant(intValues = {
            Connection.TRANSACTION_NONE,
            Connection.TRANSACTION_READ_UNCOMMITTED,
            Connection.TRANSACTION_READ_COMMITTED,
            Connection.TRANSACTION_REPEATABLE_READ,
            Connection.TRANSACTION_SERIALIZABLE
    })
    private static int toJdbcIsolation(TransactionIsolation isolation) {
        return switch (isolation) {
            case READ_UNCOMMITTED -> Connection.TRANSACTION_READ_UNCOMMITTED;
            case READ_COMMITTED -> Connection.TRANSACTION_READ_COMMITTED;
            case REPEATABLE_READ -> Connection.TRANSACTION_REPEATABLE_READ;
            case SERIALIZABLE -> Connection.TRANSACTION_SERIALIZABLE;
            case DEFAULT -> Connection.TRANSACTION_NONE;
        };
    }
}
