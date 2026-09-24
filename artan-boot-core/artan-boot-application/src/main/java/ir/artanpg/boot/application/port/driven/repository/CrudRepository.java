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

/**
 * Repository interface combining both command and query operations.
 *
 * <p>This interface extends both {@link CommandRepository} and
 * {@link QueryRepository} to provide a complete CRUD (Create, Read,
 * Update, Delete) capability for aggregate roots.
 *
 * <p>Use this interface when you need both read and write operations
 * on the same aggregate type. For CQRS-style architectures, consider
 * using {@link CommandRepository} and {@link QueryRepository} separately.
 *
 * @param <T> The type of aggregate root being managed
 * @param <I> The type of identifier used by the aggregate
 * @author Mohammad Yazdian
 * @see Repository
 * @see CommandRepository
 * @see QueryRepository
 * @since 0.1.0
 */
public interface CrudRepository<T extends AggregateRoot<I>, I extends Identifier<?>>
        extends CommandRepository<T, I>, QueryRepository<T, I> {
}
