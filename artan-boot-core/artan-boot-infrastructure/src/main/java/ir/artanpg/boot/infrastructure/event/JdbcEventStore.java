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

import ir.artanpg.boot.application.port.driven.event.EventSerializer;
import ir.artanpg.boot.application.port.driven.event.EventStore;
import ir.artanpg.boot.domain.event.DomainEvent;
import ir.artanpg.boot.domain.event.StoredEvent;
import ir.artanpg.boot.domain.exception.EventConcurrencyException;
import ir.artanpg.boot.domain.model.Identifier;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * JDBC-backed {@link EventStore}.
 *
 * <p>Requires a table (see {@link #SCHEMA_DDL}). Optimistic concurrency is
 * enforced by checking {@code MAX(stream_version)} before insert.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public class JdbcEventStore implements EventStore {

    /**
     * Suggested DDL for the event store table (ANSI-ish SQL).
     */
    public static final String SCHEMA_DDL = """
            CREATE TABLE IF NOT EXISTS domain_event_entry (
                event_id        VARCHAR(64)   NOT NULL,
                aggregate_id    VARCHAR(255)  NOT NULL,
                stream_version  BIGINT        NOT NULL,
                event_type      VARCHAR(255)  NOT NULL,
                occurred_at     TIMESTAMP     NOT NULL,
                stored_at       TIMESTAMP     NOT NULL,
                payload         BLOB          NOT NULL,
                PRIMARY KEY (event_id),
                UNIQUE (aggregate_id, stream_version)
            )
            """;

    private static final String INSERT_SQL = """
            INSERT INTO domain_event_entry
                (event_id, aggregate_id, stream_version, event_type, occurred_at, stored_at, payload)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_MAX_VERSION_SQL =
            "SELECT COALESCE(MAX(stream_version), 0) FROM domain_event_entry WHERE aggregate_id = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT event_id, aggregate_id, stream_version, event_type, occurred_at, stored_at, payload "
                    + "FROM domain_event_entry WHERE aggregate_id = ? ORDER BY stream_version ASC";

    private static final String SELECT_FROM_VERSION_SQL =
            "SELECT event_id, aggregate_id, stream_version, event_type, occurred_at, stored_at, payload "
                    + "FROM domain_event_entry WHERE aggregate_id = ? AND stream_version > ? "
                    + "ORDER BY stream_version ASC";

    private final JdbcTemplate jdbcTemplate;

    private final EventSerializer serializer;

    private final Clock clock;

    public JdbcEventStore(@NonNull JdbcTemplate jdbcTemplate, @NonNull EventSerializer serializer) {
        this(jdbcTemplate, serializer, Clock.systemUTC());
    }

    public JdbcEventStore(@NonNull JdbcTemplate jdbcTemplate,
                          @NonNull EventSerializer serializer,
                          @NonNull Clock clock) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    @Transactional
    public void append(@NonNull Identifier<?> aggregateId,
                       @NonNull List<DomainEvent> events,
                       long expectedVersion) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(events, "events must not be null");
        if (events.isEmpty()) {
            return;
        }

        String key = String.valueOf(aggregateId.value());
        Long currentVersion = this.jdbcTemplate.queryForObject(SELECT_MAX_VERSION_SQL, Long.class, key);
        long actual = (currentVersion != null) ? currentVersion : 0L;
        long effectiveExpected = (expectedVersion == -1L) ? 0L : expectedVersion;

        if (effectiveExpected != actual) {
            throw new EventConcurrencyException(effectiveExpected, actual);
        }

        Instant storedAt = Instant.now(this.clock);
        long nextVersion = actual;

        for (DomainEvent event : events) {
            nextVersion++;
            byte[] payload = this.serializer.serialize(event);
            this.jdbcTemplate.update(INSERT_SQL,
                    event.getEventId(),
                    key,
                    nextVersion,
                    event.getEventType().getName(),
                    Timestamp.from(event.getOccurredAt()),
                    Timestamp.from(storedAt),
                    payload);
        }
    }

    @Override
    @NonNull
    public List<StoredEvent> load(@NonNull Identifier<?> aggregateId) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        String key = String.valueOf(aggregateId.value());
        return this.jdbcTemplate.query(SELECT_ALL_SQL, storedEventMapper(), key);
    }

    @Override
    @NonNull
    public List<StoredEvent> load(@NonNull Identifier<?> aggregateId, long fromVersion) {
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        String key = String.valueOf(aggregateId.value());
        return this.jdbcTemplate.query(SELECT_FROM_VERSION_SQL, storedEventMapper(), key, fromVersion);
    }

    private RowMapper<StoredEvent> storedEventMapper() {
        return (rs, rowNum) -> mapRow(rs);
    }

    private StoredEvent mapRow(ResultSet rs) throws SQLException {
        String eventType = rs.getString("event_type");
        byte[] payload = rs.getBytes("payload");
        DomainEvent event = this.serializer.deserialize(eventType, payload);
        long streamVersion = rs.getLong("stream_version");
        Instant storedAt = rs.getTimestamp("stored_at").toInstant();
        return new StoredEvent(event, streamVersion, storedAt);
    }
}
