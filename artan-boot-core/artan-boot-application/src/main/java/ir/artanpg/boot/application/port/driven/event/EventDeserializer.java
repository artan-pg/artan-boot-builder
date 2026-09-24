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
 * A driven port that reconstructs a {@link DomainEvent} from its external
 * representation — the exact counterpart of {@link EventSerializer}.
 *
 * <h2>Contract:</h2>
 * <ul>
 *   <li><strong>Round-trip fidelity:</strong> for every event {@code e} it must
 *       hold that {@code deserialize(serialize(e), e.getClass())} equals
 *       {@code e} (per the event's own equals contract). This property should
 *       be enforced by tests in every concrete implementation.</li>
 *   <li><strong>No unsafe JVM deserialization:</strong> implementations must
 * <em>not</em> be built on raw {@link java.io.ObjectInputStream}; prefer a
 *       schema-based codec (JSON + allow-listed types, Avro, Protobuf). This
 *       closes the well-known gadget-chain attack surface.</li>
 *   <li><strong>Explicit target type:</strong> callers pass the expected event
 *       class instead of trusting a type name embedded in the wire data, so a
 *       malicious or stale payload cannot make the runtime load arbitrary
 *       classes.</li>
 *   <li><strong>Versioning tolerance:</strong> unknown fields should be
 *       ignored (forward compatibility) while missing mandatory fields must
 *       fail fast with a clear error.</li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @see EventSerializer
 * @since 0.1.0
 */
public interface EventDeserializer {

    /**
     * Deserializes the given byte representation into a domain event of the
     * expected type.
     *
     * @param data the serialized event bytes produced by a matching {@link EventSerializer}
     * @param type the expected concrete event class
     * @param <E>  the inferred event type, bounded by {@link DomainEvent}
     * @return the reconstructed domain event
     */
    <E extends DomainEvent<?, ?>> @NonNull E deserialize(@NonNull byte[] data, @NonNull Class<E> type)
            throws SerializationException;

    /**
     * Returns the wire format consumed by this deserializer.
     *
     * <p>The value must be identical to the {@link EventSerializer#supportedFormat()}
     * of its paired serializer so that components managing multiple formats can
     * route incoming messages to the correct implementation.
     *
     * @return a non-blank, stable format identifier
     */
    String supportedFormat();
}
