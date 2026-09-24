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
