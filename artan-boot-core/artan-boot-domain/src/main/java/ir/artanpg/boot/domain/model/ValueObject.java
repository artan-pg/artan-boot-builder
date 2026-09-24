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

import java.io.Serializable;

/**
 * Marker interface for Domain Value Objects.
 *
 * <p>Value Objects are immutable, interchangeable attributes that represent
 * descriptive aspects of the domain with no conceptual identity.
 *
 * <p><b>Contracts and Guidelines:</b>
 * <ul>
 *   <li><b>Immutability:</b> State MUST not change after creation.</li>
 *   <li><b>Equality:</b> Implementations MUST override
 *      {@link Object#equals(Object)} and {@link Object#hashCode()} based on
 *      all their structural attributes.</li>
 *   <li><b>Implementation:</b> It is highly recommended to use Java
 *      {@code record}  classes for implementation to automatically satisfy
 *      immutability and equality contracts.</li>
 * </ul>
 *
 * @author Mohammad Yazdian
 * @see Identifier
 * @since 0.1.0
 */
public interface ValueObject extends Serializable {
}
