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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Functional interface for generating unique identifiers.
 *
 * <p>Provides a contract for ID generation strategies used
 * throughout the domain model.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
@FunctionalInterface
public interface IdGenerator {

    /**
     * Generates a new unique identifier.
     *
     * @return a non-null, non-blank unique ID string
     */
    @NonNull
    String nextId();

    /**
     * Returns the default {@code IdGenerator} backed by
     * {@link UUID#randomUUID()}.
     *
     * <p>Each call produces a statistically unique random UUID string.
     * This generator is thread-safe.
     *
     * @return a shared UUID-based generator instance
     */
    static IdGenerator uuid() {
        return () -> UUID.randomUUID().toString();
    }

    /**
     * Creates a new sequential (deterministic) generator starting from zero.
     *
     * <p>Successive calls return {@code "0"}, {@code "1"}, {@code "2"}, ...
     * Useful in tests where predictable IDs are required. The returned
     * instance is thread-safe but its values are only unique per-instance.
     *
     * @return a fresh sequential generator
     */
    static IdGenerator sequential() {
        AtomicLong counter = new AtomicLong(0L);
        return () -> Long.toString(counter.getAndIncrement());
    }
}
