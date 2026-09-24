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

import org.jspecify.annotations.NonNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an entity within an aggregate boundary whose lifecycle is managed
 * by the {@link AggregateRoot}.
 *
 * <p>Aggregate members have identity within the aggregate but cannot be
 * accessed or persisted independently. They share the aggregate root's
 * transaction boundary and do not publish domain events directly.
 *
 * @param <I> The type of identifier
 * @author Mohammad Yazdian
 * @see AggregateRoot
 * @since 0.1.0
 */
public interface AggregateMember<I extends Identifier<?>> extends Serializable {

    /**
     * Returns the unique identifier of this aggregate root.
     *
     * <p>The identifier is the primary key that distinguishes this aggregate
     * from all others in the system. It is immutable and never changes during
     * the aggregate's lifecycle.
     *
     * @return the aggregate's unique identifier
     */
    I getId();

    /**
     * Determines whether this member has the same identity as another member.
     *
     * @param other the other member to compare
     * @return {@code true} if both members have the same identifier, {@code false} otherwise
     */
    @SuppressWarnings("ConstantValue")
    default boolean sameIdentityAs(@NonNull AggregateMember<I> other) {
        return other != null && Objects.equals(other.getId(), getId());
    }

    /**
     * Checks if this domain has the same identity as the specified identifier.
     *
     * <p>This method is typically used to compare the identity of the current
     * domain with another identifier without needing to instantiate a full
     * domain object.
     *
     * @param id the identifier to compare against
     * @return {@code true} if the current domain's identifier is equal to the provided
     *         {@code id}, {@code false} otherwise
     */
    @SuppressWarnings("ConstantValue")
    default boolean sameIdentityAs(@NonNull I id) {
        return id != null && Objects.equals(id, getId());
    }
}
