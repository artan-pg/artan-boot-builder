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

package ir.artanpg.boot.domain.exception;

import java.io.Serial;

/**
 * Thrown when an optimistic concurrency conflict is detected while appending
 * events to an aggregate stream.
 *
 * @author Mohammad Yazdian
 * @see ir.artanpg.boot.application.port.driven.event.EventStore
 * @since 0.1.0
 */
public class EventConcurrencyException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long expectedVersion;

    private final long actualVersion;

    /**
     * Creates a new concurrency exception.
     *
     * @param expectedVersion the version the caller expected
     * @param actualVersion   the current version in the store
     */
    public EventConcurrencyException(long expectedVersion, long actualVersion) {
        super("Event stream concurrency conflict: expected version "
                + expectedVersion + " but was " + actualVersion);
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public long getExpectedVersion() {
        return this.expectedVersion;
    }

    public long getActualVersion() {
        return this.actualVersion;
    }
}
