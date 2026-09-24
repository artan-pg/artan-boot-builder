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

import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.event.StoredEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link AbstractEventSourcedAggregateRoot}.
 *
 * @author Mohammad Yazdian
 */
class AbstractEventSourcedAggregateRootTests {

    @Test
    void apply_ShouldIncreaseVersionAndRegisterEvent_WhenNewEventIsApplied() {
        // given
        SampleEventSourcedAggregate aggregate = new SampleEventSourcedAggregate(new StringId("a-1"));

        // when
        aggregate.changeName("Alice");

        // then
        then(aggregate.getVersion()).isEqualTo(1L);
        then(aggregate.getName()).isEqualTo("Alice");
        then(aggregate.getDomainEvents()).hasSize(1);
    }

    @Test
    void loadFromHistory_ShouldRehydrateStateWithoutUncommittedEvents_WhenHistoryIsProvided() {
        // given
        StringId id = new StringId("a-1");
        NameChangedEvent event = new NameChangedEvent(id, "Bob");
        StoredEvent stored = new StoredEvent(event, 1L, Instant.now());
        SampleEventSourcedAggregate aggregate = new SampleEventSourcedAggregate(id);

        // when
        aggregate.loadFromHistory(List.of(stored));

        // then
        then(aggregate.getVersion()).isEqualTo(1L);
        then(aggregate.getName()).isEqualTo("Bob");
        then(aggregate.getDomainEvents()).isEmpty();
    }

    // --- fixtures ---

    record StringId(String value) implements Identifier<String> {
    }

    static final class NameChangedEvent extends AbstractDomainEvent<StringId, String> {

        static final EventType TYPE = EventType.valueOf("sample.name-changed");

        NameChangedEvent(StringId aggregateId, String newName) {
            super(aggregateId, newName);
        }

        @Override
        public EventType getEventType() {
            return TYPE;
        }
    }

    static final class SampleEventSourcedAggregate extends AbstractEventSourcedAggregateRoot<StringId> {

        private String name;

        SampleEventSourcedAggregate(StringId id) {
            super(id);
        }

        void changeName(String newName) {
            apply(new NameChangedEvent(getId(), newName));
        }

        String getName() {
            return this.name;
        }

        @Override
        protected void when(DomainEvent event) {
            if (event instanceof NameChangedEvent nameChanged) {
                this.name = nameChanged.getPayload();
            }
        }
    }
}
