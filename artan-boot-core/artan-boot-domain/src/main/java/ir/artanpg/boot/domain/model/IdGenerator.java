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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Strategy abstraction for generating unique identifiers.
 *
 * <p>Hard-coding a specific generation algorithm (such as
 * {@link UUID#randomUUID()}) inside domain objects would make them untestable
 * and inflexible: tests could not predict IDs, and applications could not plug
 * in their own ID schemes (ULID, snowflake, database sequences, ...).
 *
 * <p>Although introduced to support the {@code eventId} of domain events, this
 * abstraction lives in the general {@code model} package so that any domain
 * object that needs an identifier can benefit from it (aggregates, entities,
 * value objects, correlation IDs, ...).
 *
 * <p>This interface lets callers inject any ID strategy they want:
 * <ul>
 *   <li>{@link #uuid()} — default, random UUID-based (globally unique)</li>
 *   <li>{@link #sequential()} — deterministic counter, ideal for tests</li>
 *   <li>custom lambda — {@code IdGenerator generator = () -> "id-" + n;}</li>
 * </ul>
 *
 * <p>Implementations must be thread-safe if used from multiple threads.
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
        return UuidHolder.INSTANCE;
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

    /**
     * Holder for the shared UUID generator instance (lazy initialization).
     */
    final class UuidHolder {
        private static final IdGenerator INSTANCE = () -> UUID.randomUUID().toString();

        private UuidHolder() {
            throw new UnsupportedOperationException();
        }
    }
}
