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

package ir.artanpg.boot.application.port.driven.repository;

import ir.artanpg.boot.domain.model.AggregateRoot;
import ir.artanpg.boot.domain.model.Identifier;

import java.util.Collection;
import java.util.List;

/**
 * Command repository interface providing write operations for aggregate roots.
 *
 * <p>This interface extends the base {@link Repository} to add command
 * operations for persisting, updating, and deleting aggregate roots.
 *
 * @param <T> The type of aggregate root being managed
 * @param <I> The type of identifier used by the aggregate
 * @author Mohammad Yazdian
 * @see Repository
 * @since 0.1.0
 */
public interface CommandRepository<T extends AggregateRoot<I>, I extends Identifier<?>> extends Repository<T, I> {

    /**
     * Saves a single aggregate root.
     *
     * <p>This method persists the aggregate, handling both creation (insert)
     * and updates (merge). The repository implementation should determine whether to perform an insert or update based
     * on the aggregate's state.
     *
     * <p><b>Behavior:</b>
     * <ul>
     *   <li>If the aggregate has no identifier, it's treated as a new entity</li>
     *   <li>If the aggregate has an identifier, it's treated as an existing entity</li>
     *   <li>The returned aggregate may be a different instance than the input</li>
     *   <li>All aggregate invariants should be validated before saving</li>
     * </ul>
     *
     * @param domain the aggregate root to save
     * @return the saved aggregate
     */
    T save(T domain);

    /**
     * Saves multiple aggregate roots in a batch operation.
     *
     * <p>This method efficiently persists a collection of aggregates in a
     * single batch operation.
     *
     * <p><b>Behavior:</b>
     * <ul>
     *   <li>Each aggregate is validated before saving</li>
     *   <li>The operation is typically atomic (all or nothing)</li>
     *   <li>Returned collection may contain different instances</li>
     *   <li>Preserves the order of the input collection if possible</li>
     * </ul>
     *
     * @param domains the collection of aggregates to save
     * @return the saved aggregates
     */
    List<T> saveAll(Collection<T> domains);

    /**
     * Deletes a single aggregate root.
     *
     * <p>This method removes the specified aggregate from the data source.
     * The aggregate should exist in the data source prior to deletion.
     *
     * <p><b>Behavior:</b>
     * <ul>
     *   <li>If the aggregate doesn't exist, behavior is
     *   implementation-specific</li>
     *   <li>Some implementations may throw an exception if not found</li>
     *   <li>Others may silently ignore missing aggregates</li>
     *   <li>Cascading deletions may occur based on aggregate design</li>
     * </ul>
     *
     * @param domain the aggregate to delete
     */
    void delete(T domain);

    /**
     * Deletes multiple aggregate roots in a batch operation.
     *
     * <p>This method efficiently removes a collection of aggregates in a
     * single batch operation.
     *
     * <p><b>Behavior:</b>
     * <ul>
     *   <li>Typically atomic (all or nothing)</li>
     *   <li>Missing aggregates may be silently ignored</li>
     *   <li>More efficient for large collections</li>
     *   <li>Cascading deletions may apply</li>
     * </ul>
     *
     * @param domains the collection of aggregates to delete
     */
    void deleteAll(Collection<T> domains);

    /**
     * Deletes an aggregate root by its identifier.
     *
     * <p>This is a convenient method for deleting an aggregate without first
     * loading it. This can be more efficient when you only have the identifier and don't need the aggregate instance.
     *
     * <p><b>Behavior:</b>
     * <ul>
     *   <li>If the identifier doesn't exist, implementation may silently ignore</li>
     *   <li>Some implementations may throw an exception</li>
     *   <li>More efficient than find-then-delete</li>
     * </ul>
     *
     * @param identifier the identifier of the aggregate to delete
     */
    void deleteById(I identifier);

    /**
     * Deletes multiple aggregate roots by their identifiers.
     *
     * <p>This method efficiently removes aggregates by their identifiers
     * in a single batch operation without needing to load the aggregates first.
     *
     * <p><b>Behavior:</b>
     * <ul>
     *   <li>Atomic operation typically</li>
     *   <li>Missing identifiers are usually ignored</li>
     *   <li>Much more efficient than individual deletions</li>
     *   <li>Ideal for archiving or bulk cleanup operations</li>
     * </ul>
     *
     * @param identifiers the collection of identifiers to delete
     */
    void deleteAllById(Collection<I> identifiers);
}
