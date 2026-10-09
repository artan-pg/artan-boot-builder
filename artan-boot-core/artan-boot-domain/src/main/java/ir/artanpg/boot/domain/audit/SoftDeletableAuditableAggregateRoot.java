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

package ir.artanpg.boot.domain.audit;

import ir.artanpg.boot.domain.exception.BusinessValidationException;
import ir.artanpg.boot.domain.exception.DomainException;
import ir.artanpg.boot.domain.model.AggregateRoot;
import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.domain.model.SoftDeletableAggregateRoot;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;

/**
 * An aggregate root that supports auditing by tracking deleter information.
 *
 * @param <E> The type representing the user/principal who performed the action
 * @param <I> The type of identifier used by the aggregate root
 * @author Mohammad Yazdian
 * @see AggregateRoot
 * @see AuditableAggregateRoot
 * @since 0.1.0
 */
public interface SoftDeletableAuditableAggregateRoot<E extends Serializable, I extends Identifier<?>>
        extends AuditableAggregateRoot<E, I>, SoftDeletableAggregateRoot<I> {

    /**
     * Marks this aggregate as soft-deleted, transitioning it to a logically
     * deleted state.
     *
     * <p>This method performs a logical deletion of the aggregate without
     * physically removing it from the data store. The aggregate remains in the
     * database but is excluded from standard queries and business operations.
     *
     * <p><b>Implementation Guidelines</b>
     * <ul>
     *   <li><b>Idempotency:</b> This method should be idempotent. If the
     *      aggregate is already deleted, calling this method again should have
     *      no effect and should not throw an exception. This allows safe retry
     *      mechanisms and concurrent operations.</li>
     *   <li><b>Domain Event:</b> It is strongly recommended to implement this
     *      method using the {@code apply} pattern with a domain event. This
     *      ensures consistency with event sourcing principles and maintains a
     *      complete audit trail of state changes.</li>
     *   <li><b>Business Validation:</b> Before marking as deleted, the
     *      aggregate should validate that deletion is allowed according to
     *      business rules. If deletion is not permitted, this method should
     *      throw a {@link BusinessValidationException}.</li>
     *   <li><b>Cascade Behavior:</b> Consider whether deletion should cascade
     *      to child entities or related aggregates. This logic should be
     *      encapsulated within the aggregate or handled by domain services.
     *      </li>
     *   <li><b>State Mutation:</b> This method should only change the internal
     *      state of the aggregate. It should not perform any I/O operations or
     *      interact with external systems.</li>
     * </ul>
     *
     * @param deletedBy the user/system who deleted this aggregate
     * @throws DomainException if deletedBy is {@code null} or {@code blank}
     * @see #isDeleted()
     * @see #isRestorable()
     */
    void markAsDeleted(String deletedBy);

    /**
     * Returns the user who deleted this aggregate.
     *
     * @return the deleter identifier
     */
    @Nullable
    E getDeletedBy();

    /**
     * Returns the timestamp when this aggregate was deleted.
     *
     * @return the deleted timestamp
     */
    @Nullable
    Instant getDeletedDate();
}
