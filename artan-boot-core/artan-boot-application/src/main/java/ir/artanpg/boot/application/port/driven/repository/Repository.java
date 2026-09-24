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
 * Base marker interface for all repository types in the domain layer.
 *
 * <p>This interface serves as the root of the repository hierarchy and
 * provides a common type for all repository implementations. It follows
 * the Repository pattern from Domain-Driven Design.
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li><b>Persistence Ignorance:</b> The domain layer remains unaware of
 *       persistence details</li>
 *   <li><b>Aggregate Boundary:</b> Only aggregate roots are managed directly
 *       </li>
 *   <li><b>Implementation Location:</b> Concrete implementations belong in the
 *       infrastructure layer</li>
 * </ul>
 *
 * @param <T> The type of aggregate root being managed
 * @param <I> The type of identifier used by the aggregate
 * @author Mohammad Yazdian
 * @see AggregateRoot
 * @see Identifier
 * @see CommandRepository
 * @see QueryRepository
 * @see CrudRepository
 * @since 0.1.0
 */
public interface Repository<T extends AggregateRoot<I>, I extends Identifier<?>> {
}
