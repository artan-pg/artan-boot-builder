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
import ir.artanpg.boot.infrastructure.adapter.driven.transaction.JdbcTransactionContext.ConnectionState;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link JdbcTransactionContext}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcTransactionContext")
class JdbcTransactionContextTests {

    @Mock
    private Connection connection;

    @AfterEach
    void tearDown() {
        JdbcTransactionContext.clear();
    }

    @Nested
    @DisplayName("JdbcTransactionContext()")
    class Constructor {

        @SneakyThrows
        @Test
        @DisplayName("Throws UnsupportedOperationException when instantiated via reflection")
        void constructor_ShouldThrowUnsupportedOperationException_WhenInvoked() {
            // given
            var ctor = JdbcTransactionContext.class.getDeclaredConstructor();
            ctor.setAccessible(true);

            // when & then
            thenThrownBy(ctor::newInstance)
                    .as("utility class must not be instantiable")
                    .hasCauseInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("bind(ConnectionState)")
    class Bind {

        @Test
        @DisplayName("Binds the given state to the current thread")
        void bind_ShouldBindState_WhenStateIsNonNull() {
            // given
            ConnectionState state = new ConnectionState(connection, true, false, Connection.TRANSACTION_READ_COMMITTED);

            // when
            JdbcTransactionContext.bind(state);

            // then
            then(JdbcTransactionContext.getState())
                    .as("bound state must be retrievable")
                    .isSameAs(state);
            then(JdbcTransactionContext.getConnection())
                    .as("bound connection must be retrievable")
                    .isSameAs(connection);
        }

        @SuppressWarnings("DataFlowIssue")
        @Test
        @DisplayName("Throws TransactionalDomainException when state is null")
        void bind_ShouldThrowTransactionalDomainException_WhenStateIsNull() {
            // given

            // when & then
            thenThrownBy(() -> JdbcTransactionContext.bind(null))
                    .as("null state must be rejected")
                    .isInstanceOf(TransactionalDomainException.class)
                    .hasMessage("The connection state cannot be null");
        }

        @Test
        @DisplayName("Overwrites previously bound state")
        void bind_ShouldOverwritePreviousState_WhenCalledAgain() {
            // given
            ConnectionState first = new ConnectionState(connection, true, false, Connection.TRANSACTION_READ_COMMITTED);
            ConnectionState second = new ConnectionState(connection, false, true, Connection.TRANSACTION_SERIALIZABLE);
            JdbcTransactionContext.bind(first);

            // when
            JdbcTransactionContext.bind(second);

            // then
            then(JdbcTransactionContext.getState())
                    .as("latest state must replace the previous one")
                    .isSameAs(second);
        }
    }

    @Nested
    @DisplayName("getConnection()")
    class GetConnection {

        @Test
        @DisplayName("Returns the bound connection when a state is present")
        void getConnection_ShouldReturnBoundConnection_WhenStateIsBound() {
            // given
            ConnectionState state = new ConnectionState(connection, true, false, Connection.TRANSACTION_READ_COMMITTED);
            JdbcTransactionContext.bind(state);

            // when
            Connection result = JdbcTransactionContext.getConnection();

            // then
            then(result)
                    .as("must return the connection from the bound state")
                    .isSameAs(connection);
        }

        @Test
        @DisplayName("Returns null when no state is bound")
        void getConnection_ShouldReturnNull_WhenNoStateIsBound() {
            // given

            // when
            Connection result = JdbcTransactionContext.getConnection();

            // then
            then(result)
                    .as("must return null when thread has no binding")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("getState()")
    class GetState {

        @Test
        @DisplayName("Returns the bound state when present")
        void getState_ShouldReturnBoundState_WhenStateIsBound() {
            // given
            ConnectionState state = new ConnectionState(connection, true, false, Connection.TRANSACTION_READ_COMMITTED);
            JdbcTransactionContext.bind(state);

            // when
            ConnectionState result = JdbcTransactionContext.getState();

            // then
            then(result)
                    .as("must return the exact bound state")
                    .isSameAs(state);
        }

        @Test
        @DisplayName("Returns null when no state is bound")
        void getState_ShouldReturnNull_WhenNoStateIsBound() {
            // given

            // when
            ConnectionState result = JdbcTransactionContext.getState();

            // then
            then(result)
                    .as("must return null when thread has no binding")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("Removes the bound state from the current thread")
        void clear_ShouldRemoveBoundState_WhenStateIsBound() {
            // given
            ConnectionState state = new ConnectionState(connection, true, false, Connection.TRANSACTION_READ_COMMITTED);
            JdbcTransactionContext.bind(state);

            // when
            JdbcTransactionContext.clear();

            // then
            then(JdbcTransactionContext.getState())
                    .as("state must be removed")
                    .isNull();
            then(JdbcTransactionContext.getConnection())
                    .as("connection must no longer be retrievable")
                    .isNull();
        }

        @Test
        @DisplayName("Does nothing when no state is bound")
        void clear_ShouldDoNothing_WhenNoStateIsBound() {
            // given

            // when
            JdbcTransactionContext.clear();

            // then
            then(JdbcTransactionContext.getState())
                    .as("state must remain null")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("ConnectionState")
    class ConnectionStateRecord {

        @Test
        @DisplayName("Stores and exposes all components")
        void connectionState_ShouldExposeAllComponents_WhenCreated() {
            // given
            boolean autoCommit = true;
            boolean readOnly = false;
            int isolation = Connection.TRANSACTION_REPEATABLE_READ;

            // when
            ConnectionState state = new ConnectionState(connection, autoCommit, readOnly, isolation);

            // then
            then(state.connection())
                    .as("connection component must match constructor argument")
                    .isSameAs(connection);
            then(state.autoCommit())
                    .as("autoCommit component must match constructor argument")
                    .isEqualTo(autoCommit);
            then(state.readOnly())
                    .as("readOnly component must match constructor argument")
                    .isEqualTo(readOnly);
            then(state.isolation())
                    .as("isolation component must match constructor argument")
                    .isEqualTo(isolation);
        }

        @Test
        @DisplayName("Supports equality based on component values")
        void connectionState_ShouldBeEqual_WhenComponentsAreEqual() {
            // given
            ConnectionState first = new ConnectionState(connection, true, false, Connection.TRANSACTION_READ_COMMITTED);
            ConnectionState second =
                    new ConnectionState(connection, true, false, Connection.TRANSACTION_READ_COMMITTED);

            // when & then
            then(first)
                    .as("records with identical components must be equal")
                    .isEqualTo(second)
                    .hasSameHashCodeAs(second);
        }

        @Test
        @DisplayName("Is not equal when any component differs")
        void connectionState_ShouldNotBeEqual_WhenComponentDiffers() {
            // given
            ConnectionState base = new ConnectionState(connection, true, false, Connection.TRANSACTION_READ_COMMITTED);
            ConnectionState differentAutoCommit =
                    new ConnectionState(connection, false, false, Connection.TRANSACTION_READ_COMMITTED);
            ConnectionState differentReadOnly =
                    new ConnectionState(connection, true, true, Connection.TRANSACTION_READ_COMMITTED);
            ConnectionState differentIsolation =
                    new ConnectionState(connection, true, false, Connection.TRANSACTION_SERIALIZABLE);

            // when & then
            then(base)
                    .as("must differ when autoCommit differs")
                    .isNotEqualTo(differentAutoCommit);
            then(base)
                    .as("must differ when readOnly differs")
                    .isNotEqualTo(differentReadOnly);
            then(base)
                    .as("must differ when isolation differs")
                    .isNotEqualTo(differentIsolation);
        }
    }
}
