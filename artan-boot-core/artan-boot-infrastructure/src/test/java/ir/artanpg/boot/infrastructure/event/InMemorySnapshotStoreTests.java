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

import ir.artanpg.boot.domain.event.AggregateSnapshot;
import ir.artanpg.boot.infrastructure.event.support.TestIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link InMemorySnapshotStore}.
 *
 * @author Mohammad Yazdian
 */
class InMemorySnapshotStoreTests {

    private InMemorySnapshotStore store;

    @BeforeEach
    void setUp() {
        store = new InMemorySnapshotStore();
    }

    @Test
    void save_ShouldStoreSnapshot_WhenValidSnapshotIsProvided() {
        // given
        TestIdentifier id = new TestIdentifier("agg-1");
        AggregateSnapshot snapshot = new AggregateSnapshot(
                id, 5L, "state".getBytes(StandardCharsets.UTF_8), Instant.now());

        // when
        store.save(snapshot);

        // then
        then(store.load(id)).isPresent();
        then(store.load(id).orElseThrow().getVersion()).isEqualTo(5L);
        then(store.load(id).orElseThrow().getState())
                .isEqualTo("state".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void load_ShouldReturnEmpty_WhenNoSnapshotExists() {
        // given / when / then
        then(store.load(new TestIdentifier("missing"))).isEmpty();
    }

    @Test
    void save_ShouldReplacePreviousSnapshot_WhenCalledAgain() {
        // given
        TestIdentifier id = new TestIdentifier("agg-1");
        store.save(new AggregateSnapshot(id, 3L, "v3".getBytes(StandardCharsets.UTF_8), Instant.now()));

        // when
        store.save(new AggregateSnapshot(id, 6L, "v6".getBytes(StandardCharsets.UTF_8), Instant.now()));

        // then
        then(store.load(id).orElseThrow().getVersion()).isEqualTo(6L);
    }
}
