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
import ir.artanpg.boot.domain.exception.SerializationException;
import org.jspecify.annotations.NonNull;

/**
 * A driven port that converts a {@link DomainEvent} into a
 * transport/persistence-friendly representation.
 *
 * <h2>Contract:</h2>
 * <ul>
 *   <li><strong>Format identification:</strong> {@link #supportedFormat()} lets
 *       the publishing side select the right serializer when several formats
 *       coexist (e.g. {@code "application/json"}, {@code "avro"}).</li>
 *   <li><strong>Type information:</strong> a good implementation embeds enough
 *       metadata (event type name, schema version) in the output so that
 *       {@link EventDeserializer} can reconstruct the exact event later. This
 *       is what makes the pair safe for an event store / replay scenario.</li>
 *   <li><strong>Determinism:</strong> serializing the same event twice should
 *       produce equivalent output (stable field order), which is important for
 *       check summing and idempotency checks.</li>
 *   <li><strong>Failure mode:</strong> implementations should throw an
 *       unchecked exception rather than declaring checked exceptions, keeping
 *       application services clean.</li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @see EventDeserializer
 * @since 0.1.0
 */
public interface EventSerializer {

    /**
     * Serializes the given domain event into its external representation.
     *
     * @param event the domain event to serialize
     * @return the serialized representation of the event
     */
    byte[] serialize(@NonNull DomainEvent<?, ?> event) throws SerializationException;

    /**
     * Returns the identifier of the wire format produced by this serializer.
     *
     * <p>The value should follow media-type conventions where applicable
     * (e.g. {@code "application/json"}, {@code "application/x-avro-binary"})
     * or a stable custom token (e.g. {@code "json-v1"}). Publishers and
     * transports can use it to pick the matching {@link EventDeserializer}
     * and to set content headers on message brokers.
     *
     * @return a non-blank, stable format identifier
     */
    String supportedFormat();
}
