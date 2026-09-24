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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;

/**
 * An aggregate root that supports auditing by tracking creation and
 * modification information.
 *
 * @param <E> The type representing the user/principal who performed the action
 * @param <I> The type of identifier used by the aggregate root
 * @author Mohammad Yazdian
 * @see AggregateRoot
 * @since 0.1.0
 */
public interface AuditableAggregateRoot<E extends Serializable, I extends Identifier<?>>
        extends AggregateRoot<I> {

    /**
     * Returns the user who created this aggregate.
     *
     * @return the creator identifier
     */
    @NonNull
    E getCreatedBy();

    /**
     * Returns the timestamp when this aggregate was created.
     *
     * @return the creation timestamp
     */
    @NonNull
    Instant getCreatedDate();

    /**
     * Returns the user who last modified this aggregate.
     *
     * @return the last modifier identifier, or {@code null} if not available
     */
    @Nullable
    E getLastModifiedBy();

    /**
     * Returns the timestamp when this aggregate was last modified.
     *
     * @return the last modification timestamp, or {@code null} if not set
     */
    @Nullable
    Instant getLastModifiedDate();
}
