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

import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestAggregateRoot;
import ir.artanpg.boot.infrastructure.event.support.TestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * Unit tests for {@link AggregateDomainEventPublisher}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
class AggregateDomainEventPublisherTests {

    @Mock
    private DomainEventPublisher publisher;

    private AggregateDomainEventPublisher aggregatePublisher;

    @BeforeEach
    void setUp() {
        aggregatePublisher = new AggregateDomainEventPublisher(publisher);
    }

    @Test
    void publishEventsFrom_ShouldPublishAllRegisteredEvents_WhenAggregateHasEvents() {
        // given
        TestAggregateRoot aggregate = new TestAggregateRoot(new TestIdentifier("agg-1"));
        TestDomainEvent event1 = new TestDomainEvent(new TestIdentifier("agg-1"), "one");
        TestDomainEvent event2 = new TestDomainEvent(new TestIdentifier("agg-1"), "two");
        aggregate.doSomething(event1);
        aggregate.doSomething(event2);

        // when
        aggregatePublisher.publishEventsFrom(aggregate);

        // then
        then(publisher).should().publish(event1);
        then(publisher).should().publish(event2);
        // getDomainEvents() clears the collection
        then(aggregate.getDomainEvents()).isEmpty();
    }

    @Test
    void publishEventsFrom_ShouldDoNothing_WhenAggregateHasNoEvents() {
        // given
        TestAggregateRoot aggregate = new TestAggregateRoot(new TestIdentifier("agg-1"));

        // when
        aggregatePublisher.publishEventsFrom(aggregate);

        // then
        then(publisher).shouldHaveNoInteractions();
    }

    @Test
    void publishEventsFrom_ShouldThrowNullPointerException_WhenAggregateIsNull() {
        // given / when / then
        thenThrownBy(() -> aggregatePublisher.publishEventsFrom(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("aggregate must not be null");
    }

    @Test
    void constructor_ShouldThrowNullPointerException_WhenPublisherIsNull() {
        // given / when / then
        thenThrownBy(() -> new AggregateDomainEventPublisher(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("publisher must not be null");
    }
}
