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
import ir.artanpg.boot.application.port.driven.transaction.support.TransactionContext;
import ir.artanpg.boot.application.port.driven.transaction.support.TransactionSynchronization;
import ir.artanpg.boot.application.port.driven.transaction.support.TransactionSynchronizationManager;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link JdbcTransactionManager}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcTransactionManager")
class JdbcTransactionManagerTests {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    private JdbcTransactionManager manager;

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.deactivateSynchronization();
        TransactionContext.clear();
        JdbcTransactionContext.clear();
    }

    @Nested
    @DisplayName("JdbcTransactionManager(DataSource)")
    class Constructor {

        @Test
        @DisplayName("Creates manager when dataSource is non-null")
        void constructor_ShouldCreateInstance_WhenDataSourceIsNonNull() {
            // given

            // when
            manager = new JdbcTransactionManager(dataSource);

            // then
            then(manager)
                    .as("manager instance must be created")
                    .isNotNull();
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws TransactionalDomainException when dataSource is null")
        void constructor_ShouldThrowTransactionalDomainException_WhenDataSourceIsNull() {
            // given

            // when & then
            thenThrownBy(() -> new JdbcTransactionManager(null))
                    .as("null dataSource must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("The dataSource must not be null");
        }
    }

    @Nested
    @DisplayName("doBegin via executeInTransaction")
    class DoBegin {

        @SneakyThrows
        @Test
        @DisplayName("Binds connection with autoCommit disabled and default isolation")
        void doBegin_ShouldBindConnection_WhenDefinitionUsesDefaults() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when
            manager.executeInTransaction(_ -> {
                then(JdbcTransactionContext.getConnection())
                        .as("connection must be bound to thread")
                        .isSameAs(connection);
                return null;
            }, definition);

            // then
            InOrder inOrder = inOrder(connection);
            inOrder.verify(connection).setAutoCommit(false);
            inOrder.verify(connection).commit();
            inOrder.verify(connection).setAutoCommit(true);
            inOrder.verify(connection).setReadOnly(false);
            inOrder.verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            inOrder.verify(connection).close();
            verify(connection, never()).setReadOnly(true);

            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared after commit")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Applies read-only flag when definition is read-only")
        void doBegin_ShouldSetReadOnly_WhenDefinitionIsReadOnly() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .readOnly(true)
                    .build();

            // when
            manager.executeInTransaction(_ -> null, definition);

            // then
            verify(connection).setReadOnly(true);
            verify(connection).setReadOnly(false);
            verify(connection).commit();
        }

        @SneakyThrows
        @Test
        @DisplayName("Applies READ_UNCOMMITTED isolation")
        void doBegin_ShouldSetReadUncommitted_WhenIsolationIsReadUncommitted() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.READ_UNCOMMITTED)
                    .build();

            // when
            manager.executeInTransaction(_ -> null, definition);

            // then
            verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }

        @SneakyThrows
        @Test
        @DisplayName("Applies READ_COMMITTED isolation")
        void doBegin_ShouldSetReadCommitted_WhenIsolationIsReadCommitted() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.READ_COMMITTED)
                    .build();

            // when
            manager.executeInTransaction(_ -> null, definition);

            // then
            verify(connection, times(2))
                    .setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }

        @SneakyThrows
        @Test
        @DisplayName("Applies REPEATABLE_READ isolation")
        void doBegin_ShouldSetRepeatableRead_WhenIsolationIsRepeatableRead() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.REPEATABLE_READ)
                    .build();

            // when
            manager.executeInTransaction(_ -> null, definition);

            // then
            verify(connection).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }

        @SneakyThrows
        @Test
        @DisplayName("Applies SERIALIZABLE isolation")
        void doBegin_ShouldSetSerializable_WhenIsolationIsSerializable() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.SERIALIZABLE)
                    .build();

            // when
            manager.executeInTransaction(_ -> null, definition);

            // then
            verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }

        @SneakyThrows
        @Test
        @DisplayName("Does not change isolation when DEFAULT is requested")
        void doBegin_ShouldSkipIsolationChange_WhenIsolationIsDefault() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.DEFAULT)
                    .build();

            // when
            manager.executeInTransaction(_ -> null, definition);

            // then
            verify(connection, times(1))
                    .setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            verify(connection, never())
                    .setTransactionIsolation(Connection.TRANSACTION_NONE);
        }

        @SneakyThrows
        @Test
        @DisplayName("Throws TransactionalDomainException and closes connection when getConnection fails")
        void doBegin_ShouldThrowTransactionalDomainException_WhenGetConnectionFails() {
            // given
            SQLException sqlEx = new SQLException("pool exhausted");
            given(dataSource.getConnection()).willThrow(sqlEx);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> null, definition))
                    .as("SQLException on getConnection must be wrapped")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("Unable to begin transaction")
                    .hasCauseInstanceOf(TransactionalDomainException.class)
                    .cause()
                    .hasMessage("Failed to begin JDBC transaction")
                    .hasCause(sqlEx);
            then(JdbcTransactionContext.getConnection())
                    .as("context must remain unbound on begin failure")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Throws TransactionalDomainException and closes connection when setAutoCommit fails")
        void doBegin_ShouldThrowTransactionalDomainException_WhenSetAutoCommitFails() {
            // given
            SQLException sqlEx = new SQLException("autoCommit not supported");
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            willThrow(sqlEx).given(connection).setAutoCommit(false);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.withDefaults();

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> null, definition))
                    .as("SQLException on setAutoCommit must be wrapped")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessageContaining("Unable to begin transaction")
                    .hasCauseInstanceOf(TransactionalDomainException.class)
                    .cause()
                    .hasMessage("Failed to begin JDBC transaction")
                    .hasCause(sqlEx);
            verify(connection).close();
            then(JdbcTransactionContext.getConnection())
                    .as("context must remain unbound on begin failure")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("doFlush via status.flush")
    class DoFlush {

        @SneakyThrows
        @Test
        @DisplayName("Completes without error because JDBC unit of work has no buffer")
        void doFlush_ShouldCompleteWithoutError_WhenStatusFlushIsCalled() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);

            // when
            manager.executeInTransaction(status -> {
                status.flush();
                return null;
            });

            // then
            verify(connection).commit();
            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared after commit")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("onCommitted via successful execute")
    class OnCommitted {

        @SneakyThrows
        @Test
        @DisplayName("Commits connection and restores original flags")
        void onCommitted_ShouldCommitAndRestore_WhenTransactionSucceeds() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_REPEATABLE_READ);
            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.SERIALIZABLE)
                    .readOnly(true)
                    .build();

            // when
            manager.executeInTransaction(_ -> "ok", definition);

            // then
            InOrder inOrder = inOrder(connection);
            inOrder.verify(connection).setAutoCommit(false);
            inOrder.verify(connection).setReadOnly(true);
            inOrder.verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            inOrder.verify(connection).commit();
            inOrder.verify(connection).setAutoCommit(true);
            inOrder.verify(connection).setReadOnly(false);
            inOrder.verify(connection).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            inOrder.verify(connection).close();
            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared after successful commit")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Returns early when no connection state is bound")
        void onCommitted_ShouldReturnEarly_WhenStateIsNull() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);

            // when
            manager.executeInTransaction(_ -> {
                JdbcTransactionContext.clear();
                return null;
            });

            // then
            verify(connection).setAutoCommit(false);

            verify(connection, never()).commit();
            verify(connection, never()).setAutoCommit(true);
            verify(connection, never()).setReadOnly(anyBoolean());
            verify(connection, never()).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            verify(connection, never()).close();

            then(JdbcTransactionContext.getState())
                    .as("context must remain unbound")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Throws TransactionalDomainException when commit fails")
        void onCommitted_ShouldThrowTransactionalDomainException_WhenCommitFails() {
            // given
            SQLException sqlEx = new SQLException("commit failed");
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            willThrow(sqlEx).given(connection).commit();
            manager = new JdbcTransactionManager(dataSource);

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(_ -> null))
                    .as("SQLException on commit must be wrapped")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Failed to commit JDBC transaction")
                    .hasCause(sqlEx);
            verify(connection).close();
            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared even after commit failure")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Logs restore failures but still closes connection")
        void onCommitted_ShouldLogRestoreFailuresAndClose_WhenRestoreThrows() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);

            AtomicInteger autoCommitCalls = new AtomicInteger();
            AtomicInteger readOnlyCalls = new AtomicInteger();
            AtomicInteger isolationCalls = new AtomicInteger();

            doAnswer(_ -> {
                if (autoCommitCalls.incrementAndGet() > 1) {
                    throw new SQLException("restore autoCommit");
                }
                return null;
            }).when(connection).setAutoCommit(anyBoolean());

            doAnswer(_ -> {
                if (readOnlyCalls.incrementAndGet() > 1) {
                    throw new SQLException("restore readOnly");
                }
                return null;
            }).when(connection).setReadOnly(anyBoolean());

            doAnswer(_ -> {
                if (isolationCalls.incrementAndGet() > 1) {
                    throw new SQLException("restore isolation");
                }
                return null;
            }).when(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            manager = new JdbcTransactionManager(dataSource);

            // when
            manager.executeInTransaction(_ -> null);

            // then
            verify(connection).commit();
            verify(connection).close();
            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared after restore failures")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Skips isolation restore when original isolation was TRANSACTION_NONE")
        void onCommitted_ShouldSkipIsolationRestore_WhenOriginalIsolationIsNone() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_NONE);
            manager = new JdbcTransactionManager(dataSource);

            // when
            manager.executeInTransaction(_ -> null);

            // then
            verify(connection).commit();
            verify(connection).setAutoCommit(true);
            verify(connection).setReadOnly(false);
            verify(connection, never()).setTransactionIsolation(Connection.TRANSACTION_NONE);
            verify(connection).close();
        }

        @SneakyThrows
        @Test
        @DisplayName("Logs restore failure for readOnly and still closes connection")
        void OnCommitted_ShouldLogReadOnlyFailure_WhenSetReadOnlyThrows() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            willThrow(new SQLException("restore readOnly failed")).given(connection).setReadOnly(false);

            manager = new JdbcTransactionManager(dataSource);

            // when
            manager.executeInTransaction(_ -> null);

            // then
            verify(connection).commit();
            verify(connection).setReadOnly(false);
            verify(connection).close();
            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared even after readOnly restore failure")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Logs restore failure for isolation and still closes connection")
        void OnCommitted_ShouldLogIsolationFailure_WhenSetTransactionIsolationThrows() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);

            AtomicInteger isolationCalls = new AtomicInteger();
            doAnswer(_ -> {
                if (isolationCalls.incrementAndGet() > 1) throw new SQLException("restore isolation failed");
                return null;
            }).when(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            manager = new JdbcTransactionManager(dataSource);
            TransactionDefinition definition = TransactionDefinition.builder()
                    .isolation(TransactionIsolation.READ_COMMITTED)
                    .build();

            // when
            manager.executeInTransaction(_ -> null, definition);

            // then
            verify(connection).commit();
            verify(connection).close();
            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared even after isolation restore failure")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("onRolledBack via rollback-only or exception")
    class OnRolledBack {

        @SneakyThrows
        @Test
        @DisplayName("Rolls back connection and restores original flags")
        void onRolledBack_ShouldRollbackAndRestore_WhenStatusIsRollbackOnly() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(false);
            given(connection.isReadOnly()).willReturn(true);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_SERIALIZABLE);
            manager = new JdbcTransactionManager(dataSource);

            // when
            manager.executeInTransaction(status -> {
                status.setRollbackOnly();
                return null;
            });

            // then
            verify(connection, times(2)).setAutoCommit(false);
            verify(connection).rollback();
            verify(connection).setReadOnly(true);
            verify(connection).setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            verify(connection).close();
            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared after rollback")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Returns early when no connection state is bound")
        void onRolledBack_ShouldReturnEarly_WhenStateIsNull() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);

            // when
            manager.executeInTransaction(status -> {
                JdbcTransactionContext.clear();
                status.setRollbackOnly();
                return null;
            });

            // then
            verify(connection).setAutoCommit(false);
            verify(connection, never()).rollback();
            verify(connection, never()).setAutoCommit(true);
            verify(connection, never()).setReadOnly(anyBoolean());
            verify(connection, never()).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            verify(connection, never()).close();

            then(JdbcTransactionContext.getState())
                    .as("context must remain unbound")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Throws TransactionalDomainException when rollback fails")
        void onRolledBack_ShouldThrowTransactionalDomainException_WhenRollbackFails() {
            // given
            SQLException sqlEx = new SQLException("rollback failed");
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            willThrow(sqlEx).given(connection).rollback();
            manager = new JdbcTransactionManager(dataSource);

            // when & then
            thenThrownBy(() -> manager.executeInTransaction(status -> {
                status.setRollbackOnly();
                return null;
            }))
                    .as("SQLException on rollback must be wrapped")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("Failed to roll back JDBC transaction")
                    .hasCause(sqlEx);
            verify(connection).close();
            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared even after rollback failure")
                    .isNull();
        }

        @SneakyThrows
        @Test
        @DisplayName("Does nothing when no connection state is bound")
        void onRolledBack_ShouldDoNothing_WhenNoStateIsBound() {
            // given
            manager = new JdbcTransactionManager(dataSource);
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);

            // when
            manager.executeInTransaction(status -> {
                status.setRollbackOnly();
                return null;
            });

            // then
            verify(connection).rollback();
            verify(connection, atLeastOnce()).close();
        }
    }

    @Nested
    @DisplayName("logSyncError via failing synchronization")
    class LogSyncError {

        @SneakyThrows
        @Test
        @DisplayName("Logs warning and still completes transaction when synchronization fails")
        void logSyncError_ShouldLogAndContinue_WhenSynchronizationThrows() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            manager = new JdbcTransactionManager(dataSource);
            TransactionSynchronization failing = new TransactionSynchronization() {
                @Override
                public void beforeCompletion() {
                    throw new RuntimeException("sync failed");
                }
            };

            // when
            manager.executeInTransaction(_ -> {
                TransactionSynchronizationManager.registerSynchronization(failing);
                return null;
            });

            // then
            verify(connection).commit();
            then(JdbcTransactionContext.getConnection())
                    .as("context must be cleared after sync error path")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("closeQuietly edge cases")
    class CloseQuietly {

        @SneakyThrows
        @Test
        @DisplayName("Ignores close failure after successful commit")
        void closeQuietly_ShouldSwallowSQLException_WhenCloseFailsAfterCommit() {
            // given
            given(dataSource.getConnection()).willReturn(connection);
            given(connection.getAutoCommit()).willReturn(true);
            given(connection.isReadOnly()).willReturn(false);
            given(connection.getTransactionIsolation()).willReturn(Connection.TRANSACTION_READ_COMMITTED);
            willThrow(new SQLException("close failed")).given(connection).close();
            manager = new JdbcTransactionManager(dataSource);

            // when
            manager.executeInTransaction(_ -> null);

            // then
            verify(connection).commit();
            verify(connection).close();
            then(JdbcTransactionContext.getConnection())
                    .as("context must still be cleared when close fails")
                    .isNull();
        }
    }
}
