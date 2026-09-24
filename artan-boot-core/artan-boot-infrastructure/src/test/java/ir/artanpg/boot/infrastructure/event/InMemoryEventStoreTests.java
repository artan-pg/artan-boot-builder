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

import ir.artanpg.boot.domain.event.StoredEvent;
import ir.artanpg.boot.domain.exception.EventConcurrencyException;
import ir.artanpg.boot.infrastructure.event.support.TestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * Unit tests for {@link InMemoryEventStore}.
 *
 * @author Mohammad Yazdian
 */
class InMemoryEventStoreTests {

    private InMemoryEventStore store;

    private TestIdentifier aggregateId;

    @BeforeEach
    void setUp() {
        store = new InMemoryEventStore();
        aggregateId = new TestIdentifier("agg-1");
    }

    @Test
    void append_ShouldStoreEventsWithIncreasingVersion_WhenStreamIsNew() {
        // given
        TestDomainEvent e1 = new TestDomainEvent(aggregateId, "one");
        TestDomainEvent e2 = new TestDomainEvent(aggregateId, "two");

        // when
        store.append(aggregateId, List.of(e1, e2), 0L);

        // then
        List<StoredEvent> history = store.load(aggregateId);
        then(history).hasSize(2);
        then(history.get(0).getStreamVersion()).isEqualTo(1L);
        then(history.get(1).getStreamVersion()).isEqualTo(2L);
        then(history.get(0).getEvent()).isEqualTo(e1);
        then(store.currentVersion(aggregateId)).isEqualTo(2L);
    }

    @Test
    void append_ShouldAcceptMinusOne_WhenStreamIsNew() {
        // given
        TestDomainEvent e1 = new TestDomainEvent(aggregateId, "one");

        // when
        store.append(aggregateId, List.of(e1), -1L);

        // then
        then(store.load(aggregateId)).hasSize(1);
    }

    @Test
    void append_ShouldThrowEventConcurrencyException_WhenExpectedVersionDoesNotMatch() {
        // given
        store.append(aggregateId, List.of(new TestDomainEvent(aggregateId, "one")), 0L);

        // when / then
        thenThrownBy(() -> store.append(aggregateId, List.of(new TestDomainEvent(aggregateId, "two")), 0L))
                .isInstanceOf(EventConcurrencyException.class)
                .satisfies(ex -> {
                    EventConcurrencyException concurrency = (EventConcurrencyException) ex;
                    then(concurrency.getExpectedVersion()).isEqualTo(0L);
                    then(concurrency.getActualVersion()).isEqualTo(1L);
                });
    }

    @Test
    void load_ShouldReturnEmptyList_WhenStreamDoesNotExist() {
        // given / when / then
        then(store.load(new TestIdentifier("unknown"))).isEmpty();
    }

    @Test
    void load_ShouldReturnEventsAfterFromVersion_WhenFromVersionIsProvided() {
        // given
        store.append(aggregateId, List.of(
                new TestDomainEvent(aggregateId, "one"),
                new TestDomainEvent(aggregateId, "two"),
                new TestDomainEvent(aggregateId, "three")
        ), 0L);

        // when
        List<StoredEvent> partial = store.load(aggregateId, 1L);

        // then
        then(partial).hasSize(2);
        then(partial.get(0).getStreamVersion()).isEqualTo(2L);
        then(partial.get(1).getStreamVersion()).isEqualTo(3L);
    }

    @Test
    void append_ShouldDoNothing_WhenEventListIsEmpty() {
        // given / when
        store.append(aggregateId, List.of(), 0L);

        // then
        then(store.load(aggregateId)).isEmpty();
    }
}
