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

/**
 * Serializes and deserializes domain events for persistent event stores.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public interface EventSerializer {

    /**
     * Serializes the event to a binary payload.
     *
     * @param event the domain event
     * @return the serialized payload
     */
    @NonNull
    byte[] serialize(@NonNull DomainEvent event);

    /**
     * Deserializes a payload back into a domain event.
     *
     * @param eventTypeName the event type name (from {@code EventType.getName()})
     * @param payload       the serialized payload
     * @return the domain event
     */
    @NonNull
    DomainEvent deserialize(@NonNull String eventTypeName, @NonNull byte[] payload);
}
