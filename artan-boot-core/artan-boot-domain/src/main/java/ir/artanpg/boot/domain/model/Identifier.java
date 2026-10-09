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

import ir.artanpg.boot.domain.exception.DomainException;

import java.io.Serializable;

/**
 * Represents a type-safe identifier for domain entities and aggregates.
 *
 * <p>Identifiers are immutable value objects that uniquely identify domain
 * objects across the system.
 *
 * @param <I> The type of the identifier value (typically String, Long, or UUID)
 * @author Mohammad Yazdian
 * @see AggregateRoot#getId()
 * @since 0.1.0
 */
@FunctionalInterface
public interface Identifier<I extends Serializable> extends ValueObject, Serializable {

    /**
     * Returns the underlying identifier value.
     *
     * <p>This is the primary accessor method that provides the raw identifier
     * value used for persistence, equality checks, and external references.
     *
     * @return the identifier value
     * @throws DomainException if the id was {@code null}, or if it was a string, it was also {@code blank}
     */
    I value();

    /**
     * Validates this identifier's raw value and returns it.
     *
     * @return the non-null (and non-blank, for Strings) value
     * @throws DomainException if the value is {@code null} or a {@code blank} string
     */
    default I requireValid() {
        I v = value();
        if (v == null) throw new DomainException("The identifier value cannot be null");
        if (v instanceof String s && s.isBlank()) throw new DomainException("The identifier value cannot be blank");
        return v;
    }
}
