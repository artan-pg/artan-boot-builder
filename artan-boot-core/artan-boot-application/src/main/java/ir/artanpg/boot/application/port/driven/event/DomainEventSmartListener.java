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

package ir.artanpg.boot.application.port.driven.event;

import ir.artanpg.boot.domain.event.DomainEvent;
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
 * @author Mohammad Yazdian
 * @see DomainEvent
 * @see DomainEventListener
 * @since 0.1.0
 */
public interface DomainEventSmartListener extends DomainEventListener {

    /**
     * Determine whether this listener actually supports the given event type.
     *
     * @param domainEvent the event type
     * @return {@code true} if this listener supports the given event type, {@code false} otherwise
     */
    boolean supportsEventType(@NonNull Class<? extends DomainEvent> domainEvent);

    /**
     * Determine whether this listener actually supports the given source type.
     *
     * <p>The default implementation always returns {@code true}.
     *
     * @param sourceType the source type, or {@code null} if no source
     * @return {@code true} if this listener supports the given source type, {@code false} otherwise
     */
    default boolean supportsSourceType(@Nullable Class<?> sourceType) {
        return true;
    }

    /**
     * Returns an optional content-based filter for this listener.
     *
     * <p>If non-null, the event payload will be tested against this predicate
     * before {@link #process(DomainEvent)} is invoked.
     *
     * @return a predicate filter, or {@code null} for no content filtering
     */
    @Nullable
    default Predicate<DomainEvent> getFilter() {
        return null;
    }

    /**
     * Return an identifier for the listener to be able to refer to it
     * individually.
     *
     * <p>It might be necessary for specific completion callback
     * implementations to provide a specific id, whereas for other scenarios an
     * empty String (as the common default value) is acceptable as well.
     *
     * @return identifier for the listener
     */
    default String getListenerId() {
        return "";
    }
}
