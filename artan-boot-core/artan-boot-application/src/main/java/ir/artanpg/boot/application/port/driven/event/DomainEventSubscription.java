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

import ir.artanpg.boot.domain.event.EventTopic;

/**
 * Represents an active subscription of a {@link DomainEventListener} to a
 * topic on the {@link DomainEventBus}.
 *
 * <p>Call {@link #unsubscribe()} to remove the listener from the topic.
 *
 * @author Mohammad Yazdian
 * @see DomainEventBus
 * @since 0.1.0
 */
public interface DomainEventSubscription {

    /**
     * Cancels this subscription so that the listener no longer receives events
     * on the associated topic.
     *
     * <p>Calling this method more than once has no additional effect.
     */
    void unsubscribe();

    /**
     * Returns whether this subscription is still active.
     *
     * @return {@code true} if the listener is still registered
     */
    boolean isActive();

    /**
     * Returns the topic this subscription is bound to.
     *
     * @return the event topic
     */
    EventTopic getTopic();
}
