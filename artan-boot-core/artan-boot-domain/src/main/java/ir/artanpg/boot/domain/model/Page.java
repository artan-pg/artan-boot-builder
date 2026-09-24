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

import ir.artanpg.boot.domain.exception.DomainException;
import org.jspecify.annotations.NonNull;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * A value object representing a page of results from a paginated query.
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.
 *
 * @param <T> the type of content in the page
 * @author Mohammad Yazdian
 * @see Pageable
 * @see Sort
 * @since 0.1.0
 */
public interface Page<T> extends Serializable {

    /**
     * Transforms the content of this page using the provided mapping function.
     *
     * <p>This method is useful for converting between domain models and DTOs.
     * The pagination metadata (pageable, totalElements, etc.) is preserved in
     * the new page.
     *
     * @param converter the function to transform each element
     * @param <U>       the target type after transformation
     * @return a new {@link Page} with transformed content
     * @throws DomainException if converter is {@code null}
     */
    <U> Page<U> map(@NonNull Function<? super T, ? extends U> converter);

    /**
     * Returns the content of this page as an unmodifiable list.
     *
     * @return an unmodifiable List containing the page content
     */
    List<T> getContent();

    /**
     * Returns the pagination information associated with this page.
     *
     * @return the Pageable instance
     */
    Pageable getPageable();

    /**
     * Returns the total number of elements across all pages.
     *
     * @return the total element count
     */
    long getTotalElements();

    /**
     * Returns the total number of pages available.
     *
     * @return the total page count, or 0 if there are no elements
     */
    int getTotalPages();

    /**
     * Checks if this page is the first page.
     *
     * @return {@code true}, if this is the first page, {@code false} otherwise
     */
    boolean isFirst();

    /**
     * Checks if this page is the last page.
     *
     * @return {@code true}, if this is the last page, {@code false} otherwise
     */
    boolean isLast();
}
