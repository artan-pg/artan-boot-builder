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
import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.EventType;
import ir.artanpg.boot.domain.model.AbstractEventSourcedAggregateRoot;
import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link AbstractEventSourcedRepository}.
 *
 * @author Mohammad Yazdian
 */
@ExtendWith(MockitoExtension.class)
class AbstractEventSourcedRepositoryTests {

    @Mock
    private DomainEventPublisher publisher;

    private InMemoryEventStore eventStore;

    private SampleEventSourcedRepository repository;

    @BeforeEach
    void setUp() {
        eventStore = new InMemoryEventStore();
        repository = new SampleEventSourcedRepository(eventStore, publisher);
    }

    @Test
    void save_ShouldAppendAndPublish_WhenAggregateHasUncommittedEvents() {
        // given
        TestIdentifier id = new TestIdentifier("acc-1");
        SampleAccount account = new SampleAccount(id);
        account.open("Alice");

        // when
        repository.save(account);

        // then
        then(eventStore.load(id)).hasSize(1);
        then(account.getDomainEvents()).isEmpty();
        verify(publisher, times(1)).publish(any(DomainEvent.class));
    }

    @Test
    void save_ShouldDoNothing_WhenNoUncommittedEvents() {
        // given
        SampleAccount account = new SampleAccount(new TestIdentifier("acc-1"));

        // when
        repository.save(account);

        // then
        then(eventStore.load(account.getId())).isEmpty();
        verifyNoInteractions(publisher);
    }

    @Test
    void findById_ShouldRehydrateAggregate_WhenStreamExists() {
        // given
        TestIdentifier id = new TestIdentifier("acc-1");
        SampleAccount account = new SampleAccount(id);
        account.open("Alice");
        account.rename("Alicia");
        repository.save(account);

        // when
        Optional<SampleAccount> loaded = repository.findById(id);

        // then
        then(loaded).isPresent();
        then(loaded.get().getHolderName()).isEqualTo("Alicia");
        then(loaded.get().getVersion()).isEqualTo(2L);
        then(loaded.get().getDomainEvents()).isEmpty();
    }

    @Test
    void findById_ShouldReturnEmpty_WhenStreamDoesNotExist() {
        // given / when / then
        then(repository.findById(new TestIdentifier("missing"))).isEmpty();
    }

    @Test
    void save_ShouldSupportMultipleSaves_WhenVersionIsTracked() {
        // given
        TestIdentifier id = new TestIdentifier("acc-1");
        SampleAccount account = new SampleAccount(id);
        account.open("Alice");
        repository.save(account);

        SampleAccount reloaded = repository.findById(id).orElseThrow();
        reloaded.rename("Bob");

        // when
        repository.save(reloaded);

        // then
        SampleAccount finalState = repository.findById(id).orElseThrow();
        then(finalState.getHolderName()).isEqualTo("Bob");
        then(finalState.getVersion()).isEqualTo(2L);
        then(eventStore.load(id)).hasSize(2);
    }

    // --- fixtures ---

    static final class AccountOpenedEvent extends AbstractDomainEvent<TestIdentifier, String> {

        static final EventType TYPE = EventType.valueOf("account.opened");

        AccountOpenedEvent(TestIdentifier id, String holderName) {
            super(id, holderName);
        }

        @Override
        public EventType getEventType() {
            return TYPE;
        }
    }

    static final class AccountRenamedEvent extends AbstractDomainEvent<TestIdentifier, String> {

        static final EventType TYPE = EventType.valueOf("account.renamed");

        AccountRenamedEvent(TestIdentifier id, String newName) {
            super(id, newName);
        }

        @Override
        public EventType getEventType() {
            return TYPE;
        }
    }

    static final class SampleAccount extends AbstractEventSourcedAggregateRoot<TestIdentifier> {

        private String holderName;

        SampleAccount(TestIdentifier id) {
            super(id);
        }

        void open(String holderName) {
            apply(new AccountOpenedEvent(getId(), holderName));
        }

        void rename(String newName) {
            apply(new AccountRenamedEvent(getId(), newName));
        }

        String getHolderName() {
            return this.holderName;
        }

        @Override
        protected void when(DomainEvent event) {
            if (event instanceof AccountOpenedEvent opened) {
                this.holderName = opened.getPayload();
            }
            else if (event instanceof AccountRenamedEvent renamed) {
                this.holderName = renamed.getPayload();
            }
        }
    }

    static final class SampleEventSourcedRepository
            extends AbstractEventSourcedRepository<SampleAccount, TestIdentifier> {

        SampleEventSourcedRepository(InMemoryEventStore store, DomainEventPublisher publisher) {
            super(store, publisher);
        }

        @Override
        protected SampleAccount createInstance(TestIdentifier id) {
            return new SampleAccount(id);
        }
    }
}
