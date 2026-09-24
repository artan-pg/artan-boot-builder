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

package ir.artanpg.boot.domain.event;

import ir.artanpg.boot.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link EventTopic}.
 *
 * @author Mohammad Yazdian
 */
class EventTopicTests {

    @Test
    void of_ShouldReturnSameInstance_WhenNameIsEqual() {
        // given / when
        EventTopic t1 = EventTopic.of("account.opened");
        EventTopic t2 = EventTopic.of("account.opened");

        // then
        then(t1).isSameAs(t2);
        then(t1.getName()).isEqualTo("account.opened");
    }

    @Test
    void of_ShouldCreateFromEventType_WhenEventTypeIsProvided() {
        // given
        EventType type = EventType.valueOf("payment.completed");

        // when
        EventTopic topic = EventTopic.of(type);

        // then
        then(topic.getName()).isEqualTo("payment.completed");
    }

    @Test
    void of_ShouldThrowDomainException_WhenNameIsBlank() {
        // given / when / then
        thenThrownBy(() -> EventTopic.of("  "))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("topic name");
    }

    @Test
    void of_ShouldThrowDomainException_WhenNameIsNull() {
        // given / when / then
        thenThrownBy(() -> EventTopic.of((String) null))
                .isInstanceOf(DomainException.class);
    }
}
