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

package ir.artanpg.boot.application.port.driving.event;

import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.exception.DomainEventException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * An extension of {@link DomainEventListener} that provides additional metadata
 * and filtering capabilities for domain event listeners.
 *
 * <p>This interface allows listeners to specify which event types and source
 * types they support, as well as define their execution order and an optional
 * identifier.
 *
 * @param <T> the specific {@code DomainEvent} subclass to listen to
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @see DomainEventListener
 * @since 0.1.0
 */
public interface DomainEventSmartListener<T extends DomainEvent<?, ?>> extends DomainEventListener<T> {

    /**
     * Determines whether this listener supports the given semantic event type.
     *
     * <p>This method enables <strong>semantic matching</strong> rather than
     * structural class matching. This is critical for:
     * <ul>
     *   <li><strong>Event Sourcing:</strong> Events stored in the event store
     *       may be deserialized into different class versions, but their
     *       {@link EventType} remains stable.</li>
     *   <li><strong>Distributed Systems:</strong> Integration events from
     *       other bounded contexts may have different class names but the same
     *       semantic type.</li>
     *   <li><strong>Refactoring Safety:</strong> Renaming or moving event
     *       classes will not break listener registration.</li>
     * </ul>
     *
     * <p>Implementation Guidelines:
     * <ul>
     *   <li>Compare against stable, human-readable event type names.</li>
     *   <li>Prefer using {@link EventType#valueOf(String)} constants for
     *       type-safe comparisons.</li>
     *   <li>This method should be fast and side effect free.</li>
     * </ul>
     *
     * @param eventType the semantic type of the event; never {@code null}
     * @return {@code true} if this listener supports the given event type, {@code false} otherwise
     * @throws DomainEventException if eventType is {@code null}
     */
    boolean supportsEventType(@NonNull EventType eventType);

    /**
     * Returns an optional content-based filter for this listener.
     *
     * <p>If non-null, the event payload will be tested against this predicate
     * before {@link #process(DomainEvent)} is invoked.
     *
     * @return a predicate filter, or {@code null} for no content filtering
     */
    @Nullable
    default Predicate<DomainEvent<?, ?>> getFilter() {
        return null;
    }

    /**
     * Returns a unique identifier for this listener.
     *
     * <p>This ID is used for:
     * <ul>
     *   <li><strong>Duplicate Prevention:</strong> The multicaster will reject
     *       registration of a second listener with the same non-blank ID.</li>
     *   <li><strong>Logging and Tracing:</strong> Error messages and interceptor
     *       logs will reference this ID instead of the class name.</li>
     *   <li><strong>Dynamic Management:</strong> Listeners can be removed or
     *       disabled by ID at runtime.</li>
     * </ul>
     *
     * <p>If this method returns {@code null} or a {@code blank} string, the
     * multicaster will use the listener's class name as a fallback identifier.
     *
     * @return a unique listener identifier, or {@code blank} for default behavior
     */
    default String getListenerId() {
        return "";
    }
}
