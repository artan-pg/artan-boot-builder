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

import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link JavaSerializationEventSerializer}.
 *
 * @author Mohammad Yazdian
 */
class JavaSerializationEventSerializerTests {

    private final JavaSerializationEventSerializer serializer = new JavaSerializationEventSerializer();

    @Test
    void serializeAndDeserialize_ShouldRoundTrip_WhenEventIsValid() {
        // given
        TestDomainEvent original = new TestDomainEvent(new TestIdentifier("id-1"), "payload-data");

        // when
        byte[] bytes = serializer.serialize(original);
        DomainEvent restored = serializer.deserialize(original.getEventType().getName(), bytes);

        // then
        then(restored).isInstanceOf(TestDomainEvent.class);
        then(restored.getEventId()).isEqualTo(original.getEventId());
        then(restored.getPayload()).isEqualTo("payload-data");
    }
}
