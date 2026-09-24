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

package ir.artanpg.boot.infrastructure.event.support;

import ir.artanpg.boot.domain.event.AbstractDomainEvent;
import ir.artanpg.boot.domain.event.EventType;

/**
 * Concrete domain event used in unit tests.
 */
public class TestDomainEvent extends AbstractDomainEvent<TestIdentifier, String> {

    public static final EventType TYPE = EventType.valueOf("test.domain.event");

    public TestDomainEvent(TestIdentifier aggregateId, String payload) {
        super(aggregateId, payload);
    }

    @Override
    public EventType getEventType() {
        return TYPE;
    }
}
