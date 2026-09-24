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

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Unit tests for {@link IdGenerator}.
 *
 * @author Mohammad Yazdian
 */
class IdGeneratorTests {

    @Test
    void uuid_ShouldReturnNonNullId_WhenCalled() {
        // given
        IdGenerator generator = IdGenerator.uuid();

        // when
        String id = generator.nextId();

        // then
        then(id).isNotNull();
    }

    @Test
    void uuid_ShouldReturnNonBlankId_WhenCalled() {
        // given
        IdGenerator generator = IdGenerator.uuid();

        // when
        String id = generator.nextId();

        // then
        then(id).isNotBlank();
    }

    @Test
    void uuid_ShouldReturnValidUuidFormat_WhenCalled() {
        // given
        IdGenerator generator = IdGenerator.uuid();

        // when
        String id = generator.nextId();

        // then
        then(id).isNotBlank();
        then(id).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    @Test
    void uuid_ShouldReturnUniqueIds_WhenCalledMultipleTimes() {
        // given
        IdGenerator generator = IdGenerator.uuid();
        Set<String> ids = new HashSet<>();

        // when
        for (int i = 0; i < 1000; i++) {
            ids.add(generator.nextId());
        }

        // then
        then(ids).hasSize(1000);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void uuid_ShouldBeThreadSafe_WhenCalledFromMultipleThreads() throws InterruptedException {
        // given
        IdGenerator generator = IdGenerator.uuid();
        int threadCount = 10;
        int idsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<String> ids = java.util.Collections.synchronizedSet(new HashSet<>());

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        ids.add(generator.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        then(ids).hasSize(threadCount * idsPerThread);
    }

    @Test
    void sequential_ShouldReturnZero_WhenCalledFirstTime() {
        // given
        IdGenerator generator = IdGenerator.sequential();

        // when
        String id = generator.nextId();

        // then
        then(id).isEqualTo("0");
    }

    @Test
    void sequential_ShouldReturnIncrementingIds_WhenCalledMultipleTimes() {
        // given
        IdGenerator generator = IdGenerator.sequential();

        // when
        String id1 = generator.nextId();
        String id2 = generator.nextId();
        String id3 = generator.nextId();

        // then
        then(id1).isEqualTo("0");
        then(id2).isEqualTo("1");
        then(id3).isEqualTo("2");
    }

    @Test
    void sequential_ShouldReturnNonNullId_WhenCalled() {
        // given
        IdGenerator generator = IdGenerator.sequential();

        // when
        String id = generator.nextId();

        // then
        then(id).isNotNull();
    }

    @Test
    void sequential_ShouldReturnNonBlankId_WhenCalled() {
        // given
        IdGenerator generator = IdGenerator.sequential();

        // when
        String id = generator.nextId();

        // then
        then(id).isNotBlank();
    }

    @Test
    void sequential_ShouldReturnUniqueIdsPerInstance_WhenMultipleInstancesCreated() {
        // given
        IdGenerator generator1 = IdGenerator.sequential();
        IdGenerator generator2 = IdGenerator.sequential();

        // when
        String id1 = generator1.nextId();
        String id2 = generator1.nextId();
        String id3 = generator2.nextId();
        String id4 = generator2.nextId();

        // then
        then(id1).isEqualTo("0");
        then(id2).isEqualTo("1");
        then(id3).isEqualTo("0");
        then(id4).isEqualTo("1");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void sequential_ShouldBeThreadSafe_WhenCalledFromMultipleThreads() throws InterruptedException {
        // given
        IdGenerator generator = IdGenerator.sequential();
        int threadCount = 10;
        int idsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<String> ids = java.util.Collections.synchronizedSet(new HashSet<>());

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < idsPerThread; j++) {
                        ids.add(generator.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        then(ids).hasSize(threadCount * idsPerThread);
    }

    @Test
    void sequential_ShouldGenerateConsecutiveNumbers_WhenCalledManyTimes() {
        // given
        IdGenerator generator = IdGenerator.sequential();

        // when
        for (int i = 0; i < 100; i++) {
            String id = generator.nextId();

            // then
            then(id).isEqualTo(String.valueOf(i));
        }
    }

    @Test
    void nextId_ShouldReturnString_WhenCalled() {
        // given
        IdGenerator generator = IdGenerator.uuid();

        // when
        String id = generator.nextId();

        // then
        then(id).isInstanceOf(String.class);
    }
}
