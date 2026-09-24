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
import ir.artanpg.boot.domain.model.Identifier;
import ir.artanpg.boot.domain.model.ValueObject;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A point-in-time snapshot of an event-sourced aggregate state.
 *
 * <p>The {@code state} payload is opaque to the framework; applications define
 * how it is produced and restored (e.g. via serialization of a memento).
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public final class AggregateSnapshot implements ValueObject, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Identifier<?> aggregateId;
    private final long version;
    private final byte[] state;
    private final Instant takenAt;

    /**
     * Creates a new aggregate snapshot.
     *
     * @param aggregateId the aggregate identifier
     * @param version     the stream version at which this snapshot was taken
     * @param state       the serialized aggregate state
     * @param takenAt     when the snapshot was taken
     */
    public AggregateSnapshot(@NonNull Identifier<?> aggregateId,
                             long version,
                             @NonNull byte[] state,
                             @NonNull Instant takenAt) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(takenAt, "takenAt must not be null");
        if (version < 0) {
            throw new DomainException("snapshot version cannot be negative");
        }
        this.aggregateId = aggregateId;
        this.version = version;
        this.state = state.clone();
        this.takenAt = takenAt;
    }

    public Identifier<?> getAggregateId() {
        return this.aggregateId;
    }

    public long getVersion() {
        return this.version;
    }

    public byte[] getState() {
        return this.state.clone();
    }

    public Instant getTakenAt() {
        return this.takenAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AggregateSnapshot that)) {
            return false;
        }
        return this.version == that.version
                && this.aggregateId.equals(that.aggregateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.aggregateId, this.version);
    }

    @Override
    public String toString() {
        return "AggregateSnapshot[aggregateId=" + this.aggregateId.value()
                + ", version=" + this.version + "]";
    }
}
