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

import ir.artanpg.boot.application.port.driven.event.DomainEventBus;
import ir.artanpg.boot.application.port.driven.event.DomainEventPublisher;
import ir.artanpg.boot.infrastructure.event.support.TestAggregateRoot;
import ir.artanpg.boot.infrastructure.event.support.TestDomainEvent;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link AggregateDomainEventPublisher}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
class AggregateDomainEventPublisherTests {

    @Mock
    private DomainEventPublisher publisher;

    @Mock
    private DomainEventBus eventBus;

    @Test
    void publishEventsFrom_ShouldUsePublisher_WhenOnlyPublisherIsConfigured() {
        // given
        AggregateDomainEventPublisher aggregatePublisher = new AggregateDomainEventPublisher(publisher);
        TestAggregateRoot aggregate = new TestAggregateRoot(new TestIdentifier("agg-1"));
        TestDomainEvent event = new TestDomainEvent(new TestIdentifier("agg-1"), "one");
        aggregate.doSomething(event);

        // when
        aggregatePublisher.publishEventsFrom(aggregate);

        // then
        verify(publisher).publish(event);
        then(aggregate.getDomainEvents()).isEmpty();
    }

    @Test
    void publishEventsFrom_ShouldUseBus_WhenBusIsConfigured() {
        // given
        AggregateDomainEventPublisher aggregatePublisher = new AggregateDomainEventPublisher(eventBus);
        TestAggregateRoot aggregate = new TestAggregateRoot(new TestIdentifier("agg-1"));
        TestDomainEvent event = new TestDomainEvent(new TestIdentifier("agg-1"), "one");
        aggregate.doSomething(event);

        // when
        aggregatePublisher.publishEventsFrom(aggregate);

        // then
        verify(eventBus).publish(event);
    }

    @Test
    void publishEventsFrom_ShouldPreferBus_WhenBothPublisherAndBusAreProvided() {
        // given
        AggregateDomainEventPublisher aggregatePublisher =
                new AggregateDomainEventPublisher(publisher, eventBus);
        TestAggregateRoot aggregate = new TestAggregateRoot(new TestIdentifier("agg-1"));
        TestDomainEvent event = new TestDomainEvent(new TestIdentifier("agg-1"), "one");
        aggregate.doSomething(event);

        // when
        aggregatePublisher.publishEventsFrom(aggregate);

        // then
        verify(eventBus).publish(event);
        verify(publisher, never()).publish(event);
    }

    @Test
    void publishEventsFrom_ShouldDoNothing_WhenAggregateHasNoEvents() {
        // given
        AggregateDomainEventPublisher aggregatePublisher = new AggregateDomainEventPublisher(publisher);
        TestAggregateRoot aggregate = new TestAggregateRoot(new TestIdentifier("agg-1"));

        // when
        aggregatePublisher.publishEventsFrom(aggregate);

        // then
        verifyNoInteractions(publisher);
    }

    @Test
    void publishEventsFrom_ShouldThrowNullPointerException_WhenAggregateIsNull() {
        // given
        AggregateDomainEventPublisher aggregatePublisher = new AggregateDomainEventPublisher(publisher);

        // when / then
        thenThrownBy(() -> aggregatePublisher.publishEventsFrom(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("aggregate must not be null");
    }
}
