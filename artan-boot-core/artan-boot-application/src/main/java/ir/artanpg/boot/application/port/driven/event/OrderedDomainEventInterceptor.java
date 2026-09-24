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

/**
 * Optional contract for {@link DomainEventInterceptor}s that need a defined
 * execution order within an interception phase.
 *
 * <p>Interceptors are sorted ascending by {@link #getOrder()}: lower values
 * run first in the "before"/"onError" phases and last in the "after" phases
 * (mirroring the classic onion model), so wrapping interceptors observe both
 * entry and exit of everything they wrap.
 *
 * <p>Interceptors that do not implement this contract are treated as
 * {@link #DEFAULT_ORDER} and keep registration order relative to each other.
 *
 * @author Mohammad Yazdian
 * @see DomainEventInterceptor
 * @since 0.1.0
 */
public interface OrderedDomainEventInterceptor {

    /**
     * Order assigned to interceptors that do not specify one explicitly.
     */
    int DEFAULT_ORDER = 1000;

    /**
     * Returns the position of this interceptor within its phase chain.
     *
     * @return the sort key; lower runs earlier in before/onError phases
     */
    int getOrder();
}
