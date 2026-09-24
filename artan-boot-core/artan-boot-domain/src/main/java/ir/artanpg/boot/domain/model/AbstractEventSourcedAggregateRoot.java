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

import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.StoredEvent;
import ir.artanpg.boot.domain.exception.DomainException;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * Base class for event-sourced aggregates.
 *
 * <p>New domain facts are recorded via {@link #apply(DomainEvent)}, which both
 * mutates state through {@link #when(DomainEvent)} and registers the event for
 * later persistence. Historical events are applied through
 * {@link #loadFromHistory(List)} without being re-registered as uncommitted.
 *
 * @param <I> the identifier type
 * @author Mohammad Yazdian
 * @see EventSourcedAggregateRoot
 * @since 0.1.0
 */
public abstract class AbstractEventSourcedAggregateRoot<I extends Identifier<?>>
        extends AbstractAggregateRoot<I>
        implements EventSourcedAggregateRoot<I> {

    @Serial
    private static final long serialVersionUID = 1L;

    private long version;

    protected AbstractEventSourcedAggregateRoot(I id) {
        super(id);
        this.version = 0L;
    }

    protected AbstractEventSourcedAggregateRoot(AbstractBuilder<I, ?, ?> builder) {
        super(builder);
        this.version = 0L;
    }

    @Override
    public long getVersion() {
        return this.version;
    }

    /**
     * Applies a new domain event: updates state and registers it as uncommitted.
     *
     * @param event the new domain event
     */
    protected void apply(DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        when(event);
        registerEvent(event);
        this.version++;
    }

    /**
     * Applies state changes for the given event without registering it.
     *
     * <p>Subclasses implement this to fold events into aggregate state.
     *
     * @param event the event to fold into state
     */
    protected abstract void when(DomainEvent event);

    @Override
    public void loadFromHistory(List<StoredEvent> history) {
        if (history == null) {
            throw new DomainException("history cannot be null");
        }
        for (StoredEvent stored : history) {
            when(stored.getEvent());
            this.version = stored.getStreamVersion();
        }
    }
}
