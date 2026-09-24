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

package ir.artanpg.boot.infrastructure.event;

import ir.artanpg.boot.application.port.driven.event.EventSerializer;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.exception.DomainException;
import org.jspecify.annotations.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

/**
 * {@link EventSerializer} based on Java native serialization.
 *
 * <p>Suitable for tests and simple deployments. Production systems may prefer
 * JSON, Avro or Protobuf-based serializers.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public class JavaSerializationEventSerializer implements EventSerializer {

    @Override
    @NonNull
    public byte[] serialize(@NonNull DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(event);
            oos.flush();
            return bos.toByteArray();
        }
        catch (IOException ex) {
            throw new DomainException("Failed to serialize domain event", ex);
        }
    }

    @Override
    @NonNull
    public DomainEvent deserialize(@NonNull String eventTypeName, @NonNull byte[] payload) {
        Objects.requireNonNull(eventTypeName, "eventTypeName must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(payload))) {
            Object obj = ois.readObject();
            if (!(obj instanceof DomainEvent event)) {
                throw new DomainException("Deserialized object is not a DomainEvent: " + obj.getClass());
            }
            return event;
        }
        catch (IOException | ClassNotFoundException ex) {
            throw new DomainException("Failed to deserialize domain event of type " + eventTypeName, ex);
        }
    }
}
