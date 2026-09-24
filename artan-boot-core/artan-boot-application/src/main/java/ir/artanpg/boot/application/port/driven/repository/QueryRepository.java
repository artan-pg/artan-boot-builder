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
import ir.artanpg.boot.domain.model.Page;
import ir.artanpg.boot.domain.model.Pageable;
import ir.artanpg.boot.domain.model.Sort;

import java.util.List;
import java.util.Optional;

/**
 * Query repository interface providing read-only operations for aggregate
 * roots.
 *
 * <p>This interface extends the base {@link Repository} to add query
 * capabilities for retrieving aggregate roots. It focuses exclusively on read
 * operations, separating query concerns from command operations.
 *
 * @param <T> The type of aggregate root being managed
 * @param <I> The type of identifier used by the aggregate
 * @author Mohammad Yazdian
 * @see Repository
 * @since 0.1.0
 */
public interface QueryRepository<T extends AggregateRoot<I>, I extends Identifier<?>> extends Repository<T, I> {

    /**
     * Retrieves an aggregate by its identifier.
     *
     * @param identifier the unique identifier of the country
     * @return Optional of the aggregate, or {@code empty} if not found
     */
    Optional<T> findById(I identifier);

    /**
     * Retrieves aggregates by a collection of identifiers.
     *
     * @param identifiers the IDs to look up
     * @return collection of found aggregates
     */
    List<T> findAllById(List<I> identifiers);

    /**
     * Retrieves all aggregates.
     *
     * @return collection of all aggregates
     */
    List<T> findAll();

    /**
     * Retrieves all aggregates with sorting.
     *
     * @param sort the sort criteria
     * @return sorted collection of aggregates
     */
    List<T> findAll(Sort sort);

    /**
     * Retrieves a paginated subset of aggregates.
     *
     * @param pageable pagination information
     * @return page of aggregates with metadata
     */
    Page<T> findAll(Pageable pageable);

    /**
     * Returns total number of aggregates.
     *
     * @return total count
     */
    long count();

    /**
     * Checks if an aggregate exists.
     *
     * @param identifier the ID to check
     * @return {@code true}, if exists, {@code false} otherwise
     */
    boolean existsById(I identifier);
}
