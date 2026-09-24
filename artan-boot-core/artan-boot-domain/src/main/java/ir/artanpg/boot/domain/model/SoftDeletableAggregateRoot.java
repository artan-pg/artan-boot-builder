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

package ir.artanpg.boot.domain.model;

import ir.artanpg.boot.domain.exception.BusinessValidationException;

/**
 * An aggregate root that supports soft deletion (logical deletion) instead of
 * physical deletion from the data store.
 *
 * <p>Soft deletion marks an aggregate as "deleted" without actually removing
 * it from the database. This approach preserves data integrity, maintains
 * audit trails, and allows for potential restoration of deleted records.
 *
 * <h2>Design Rationale</h2>
 *
 * <p>In many business domains, physical deletion of data is prohibited due to:
 * <ul>
 *   <li><b>Regulatory Compliance:</b> Legal requirements to retain data for
 *      audit purposes</li>
 *   <li><b>Data Integrity:</b> Maintaining referential integrity with related
 *      entities</li>
 *   <li><b>Audit Trails:</b> Preserving historical records for compliance and
 *      analysis</li>
 *   <li><b>Restoration:</b> Ability to recover accidentally or mistakenly
 *      deleted data</li>
 *   <li><b>Soft References:</b> Other aggregates may hold references to this
 *      aggregate</li>
 * </ul>
 *
 * <h2>Implementation Guidelines</h2>
 * <ul>
 *   <li><b>State Transition:</b> The transition to "deleted" state should be
 *      performed through a business method (e.g., {@code delete()}) that
 *      applies a domain event (e.g., {@code AggregateDeletedEvent}) to
 *      maintain consistency with the {@code apply} pattern.</li>
 *   <li><b>Immutability:</b> Once deleted, an aggregate should not be modified
 *      except through a restoration process.</li>
 *   <li><b>Query Filtering:</b> The infrastructure layer (repositories) must
 *      automatically filter out soft-deleted aggregates from query results
 *      unless explicitly requested.</li>
 *   <li><b>Cascade Behavior:</b> Consider whether deletion should cascade to
 *      child entities or related aggregates.</li>
 * </ul>
 *
 * <h2>Integration with Repositories</h2>
 *
 * <p>Repositories should provide methods to query both active and deleted
 * aggregates:
 *
 * <h2>When to Use</h2>
 * <ul>
 *   <li>Financial records that must be retained for audit</li>
 *   <li>User accounts that may need to be restored</li>
 *   <li>Documents with legal retention requirements</li>
 *   <li>Any entity where deletion has business significance</li>
 * </ul>
 *
 * <h2>When NOT to Use</h2>
 * <ul>
 *   <li>Temporary or transient data with no business value</li>
 *   <li>Cache entries or session data</li>
 *   <li>Data subject to "right to be forgotten" regulations (e.g., GDPR)</li>
 * </ul>
 *
 * @param <I> The type of identifier used by the aggregate root
 * @author Mohammad Yazdian
 * @see AggregateRoot
 * @since 0.1.0
 */
public interface SoftDeletableAggregateRoot<I extends Identifier<?>> extends AggregateRoot<I> {

    /**
     * Determines whether this aggregate has been soft-deleted.
     *
     * <p>A soft-deleted aggregate is logically removed from the system but
     * remains in the data store for audit, compliance, or restoration
     * purposes.
     *
     * <p><b>Query Behavior:</b> Repositories should automatically filter out
     * soft-deleted aggregates from standard queries. Use specialized query
     * methods to retrieve deleted aggregates when needed.
     *
     * @return {@code true} if this aggregate has been soft-deleted, {@code false} otherwise
     */
    boolean isDeleted();

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
     * @throws BusinessValidationException if deletion is not allowed according to business rules
     * @see #isDeleted()
     * @see #isRestorable()
     */
    void markAsDeleted();

    /**
     * Determines whether this soft-deleted aggregate can be restored to an active state.
     *
     * <p>This method indicates whether a deleted aggregate is eligible for restoration.
     * Restoration may be restricted by business rules, data integrity constraints, or
     * regulatory requirements.
     *
     * <p><b>Semantics</b>
     * <ul>
     *   <li>Returns {@code true} if the aggregate is deleted AND can be safely restored
     *       without violating business rules or data integrity constraints.</li>
     *   <li>Returns {@code false} if:
     *       <ul>
     *         <li>The aggregate is not deleted (active aggregates don't need restoration)</li>
     *         <li>The aggregate is deleted but restoration is not allowed (e.g., due to
     *             regulatory requirements, data dependencies, or business rules)</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p><b>Common Restoration Restrictions</b>
     * <ul>
     *   <li><b>Time-based Restrictions:</b> Restoration may only be allowed within a certain
     *       time window (e.g., 30 days after deletion).</li>
     *   <li><b>Dependency Checks:</b> Restoration may be blocked if related entities have been
     *       permanently deleted or if references would be broken.</li>
     *   <li><b>Regulatory Compliance:</b> Some data may be subject to "right to be forgotten"
     *       regulations (e.g., GDPR) that prohibit restoration.</li>
     *   <li><b>Business Rules:</b> Certain aggregates may have business rules that permanently
     *       prevent restoration (e.g., financial records after audit closure).</li>
     *   <li><b>Data Integrity:</b> Restoration may be blocked if it would create inconsistent
     *       state with other aggregates or violate uniqueness constraints.</li>
     * </ul>
     *
     * @return {@code true} if this aggregate is deleted and can be restored, {@code false} otherwise
     * @see #isDeleted()
     * @see #markAsDeleted()
     */
    boolean isRestorable();

    /**
     * Restores this soft-deleted aggregate to an active state.
     *
     * <p>This method reverses the logical deletion performed by
     * {@link #markAsDeleted()}. The aggregate transitions back to an active
     * state and becomes visible in standard queries.
     *
     * <p><b>Implementation Guidelines:</b>
     * <ul>
     *   <li><b>Guard:</b> Must verify {@link #isRestorable()} returns
     *       {@code true} before performing restoration.</li>
     *   <li><b>Idempotency:</b> If the aggregate is already active,
     *       this method should have no effect.</li>
     *   <li><b>Domain Event:</b> A domain event (e.g.,
     *       {@code AggregateRestoredEvent}) should be registered.</li>
     * </ul>
     *
     * @throws BusinessValidationException if the aggregate is not restorable according to {@link #isRestorable()}
     * @see #isRestorable()
     * @see #markAsDeleted()
     */
    void restore();
}
