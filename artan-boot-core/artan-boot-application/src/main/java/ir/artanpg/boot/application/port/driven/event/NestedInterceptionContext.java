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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents the nesting aspect of an interception context.
 *
 * <p>Allows forming a type-safe parent chain across different event types.
 *
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public interface NestedInterceptionContext {

    /**
     * Returns the enclosing dispatch round this round is nested in, if any.
     * Useful for tracing causality when a handler synchronously publishes
     * further events, and for enforcing global cascade-depth budgets.
     *
     * @return the parent context, or {@code null} for a root round
     */
    @Nullable
    NestedInterceptionContext getParent();

    /**
     * Number of ancestors between this context and the root dispatch round.
     * A value of {@code 0} marks a root context. Dispatchers can use this to
     * detect runaway cascades (handler - event - handler - …) and fail fast.
     *
     * @return the nesting depth
     */
    int getDepth();

    /**
     * Walks the parent chain up to the outermost (root) dispatch round.
     *
     * @return the root context of this causal chain (this instance if root)
     */
    @NonNull
    NestedInterceptionContext getRootRound();
}
