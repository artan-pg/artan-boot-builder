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

import ir.artanpg.boot.application.port.driven.transaction.TransactionCallback;
import ir.artanpg.boot.application.port.driven.transaction.TransactionDefinition;
import ir.artanpg.boot.application.port.driven.transaction.TransactionManager;
import ir.artanpg.boot.domain.exception.transaction.TransactionalDomainException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory {@link TransactionManager} used as the initial, resource-free
 * implementation of the transaction port.
 *
 * <p>This manager drives the complete workflow inherited from
 * {@link AbstractResourceTransactionManager} without touching any external
 * resource. Instead of committing changes to a database, it records what
 * happened so that callers can observe transactional behavior:
 * <ul>
 *   <li>begin/commit/rollback transitions are appended to
 *       {@link #getLifecycleEvents()} as {@code begin:<name>},
 *       {@code commit:<name>} and {@code rollback:<name>}</li>
 *   <li>flush requests are appended as {@code flush:<name>}</li>
 *   <li>synchronization failures are appended as {@code error:<phase>}</li>
 * </ul>
 *
 * <p>Resource-backed managers (JDBC, JPA, JTA, …) should extend
 * {@link AbstractResourceTransactionManager} directly and override
 * {@code doBegin} / {@code doFlush}, rather than subclassing this in-memory
 * type.
 *
 * <p>Instances are safe for concurrent use; recorded events are stored in
 * synchronized collections.
 *
 * @author Mohammad Yazdian
 * @see AbstractResourceTransactionManager
 * @see SimpleTransactionStatus
 * @since 0.1.0
 */
public class InMemoryTransactionManager extends AbstractResourceTransactionManager {

    private final List<String> lifecycleEvents = Collections.synchronizedList(new ArrayList<>());

    private final List<String> invokedOperations = Collections.synchronizedList(new ArrayList<>());

    /**
     * Constructs a new {@code InMemoryTransactionManager} with empty journals.
     */
    public InMemoryTransactionManager() {
        super();
    }

    @Override
    protected void doBegin(TransactionDefinition definition, SimpleTransactionStatus status) {
        // No resource to acquire.
    }

    @Override
    protected void doFlush(SimpleTransactionStatus status) {
        recordEvent("flush", status.getTransactionName());
    }

    @Override
    protected void onBegun(SimpleTransactionStatus status) {
        recordEvent("begin", status.getTransactionName());
    }

    @Override
    protected void onCommitted(SimpleTransactionStatus status) {
        recordEvent("commit", status.getTransactionName());
    }

    @Override
    protected void onRolledBack(SimpleTransactionStatus status) {
        recordEvent("rollback", status.getTransactionName());
    }

    @Override
    protected void logSyncError(String phase, Exception ex) {
        recordEvent("error", phase);
    }

    /**
     * Executes the given callback inside a named transaction using default
     * propagation and isolation, with an optional read-only flag.
     *
     * @param <T>           the type of result produced by the callback
     * @param operationName human-readable description used as the transaction name
     * @param operation     the callback to execute
     * @param readOnly      whether the transaction should be flagged read-only
     * @return the value returned by the callback
     * @throws TransactionalDomainException if the transaction cannot be started, committed, or rolled back
     */
    public <T> T executeNamed(String operationName, TransactionCallback<T> operation, boolean readOnly) {
        if (operation == null) throw new TransactionalDomainException("The operation cannot be null");
        if (operationName == null || operationName.isBlank()) {
            throw new TransactionalDomainException("The operation name cannot be null or blank");
        }

        TransactionDefinition definition = TransactionDefinition.builder()
                .name(operationName)
                .readOnly(readOnly)
                .build();

        synchronized (invokedOperations) {
            invokedOperations.add(operationName);
        }
        return executeInTransaction(operation, definition);
    }

    /**
     * Returns an immutable snapshot of the lifecycle events recorded so far.
     *
     * @return the recorded events, oldest first
     */
    public List<String> getLifecycleEvents() {
        synchronized (lifecycleEvents) {
            return List.copyOf(lifecycleEvents);
        }
    }

    /**
     * Returns an immutable snapshot of the operations executed through
     * {@link #executeNamed(String, TransactionCallback, boolean)}.
     *
     * @return the recorded operation names, oldest first
     */
    public List<String> getInvokedOperations() {
        synchronized (invokedOperations) {
            return List.copyOf(invokedOperations);
        }
    }

    /**
     * Clears all recorded lifecycle events and invoked operations.
     */
    public void reset() {
        synchronized (lifecycleEvents) {
            lifecycleEvents.clear();
        }
        synchronized (invokedOperations) {
            invokedOperations.clear();
        }
    }

    private void recordEvent(String action, String name) {
        synchronized (lifecycleEvents) {
            lifecycleEvents.add(action + ":" + name);
        }
    }
}
