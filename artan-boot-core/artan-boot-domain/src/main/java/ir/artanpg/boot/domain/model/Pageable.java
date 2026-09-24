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
 * A value object representing pagination information for queries.
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.
 *
 * @author Mohammad Yazdian
 * @see Sort
 * @see Page
 * @since 0.1.0
 */
public interface Pageable extends Serializable {

    /**
     * Returns a Pageable for the next page.
     *
     * <p>This method increments the page number by 1 while preserving the page
     * size and sort criteria.
     *
     * @return a new {@link Pageable} instance for the next page
     */
    Pageable nextPage();

    /**
     * Returns a Pageable for the previous page, or the first page if on the
     * first page.
     *
     * <p>This is a safe navigation method that ensures the page number never
     * goes below {@code 0}.
     *
     * @return a new {@link Pageable} instance for the previous page if available, otherwise the first page
     */
    Pageable previousOrFirstPage();

    /**
     * Returns a Pageable for the first page.
     *
     * <p>This method resets the page number to 0 while preserving the page
     * size and sort criteria.
     *
     * @return a new {@link Pageable} instance for the first page
     */
    Pageable firstPage();

    /**
     * Checks if there is a previous page available.
     *
     * @return {@code true}, if the current page number is greater than {@code 0}, {@code false} otherwise
     */
    boolean hasPrevious();

    /**
     * Returns the current page number.
     *
     * @return the zero-based page number
     */
    int getPageNumber();

    /**
     * Returns the page size (number of items per page).
     *
     * @return the page size
     */
    int getPageSize();

    /**
     * Calculates the database offset based on the page number and page size.
     *
     * @return the offset value (pageNumber * pageSize)
     */
    long getOffset();

    /**
     * Returns the sorting criteria associated with this Pageable.
     *
     * @return the Sort criteria
     */
    Sort getSort();
}
