package ir.artanpg.boot.domain.model;

import ir.artanpg.boot.domain.exception.DomainException;

import java.io.Serial;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Default implementation for the {@link Pageable} interface.
 *
 * @author Mohammad Yazdian
 * @since 2.0.0
 */
public class SimplePageable implements Pageable {

    @Serial
    private static final long serialVersionUID = 6204295823387966016L;

    /**
     * The default number of records to retrieve per page in paginated queries.
     */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * The zero-based page number requested.
     **/
    private final int pageNumber;

    /**
     * The number of items to include per page.
     */
    private final int pageSize;

    /**
     * The sorting criteria applied to the query results.
     */
    private final Sort sort;

    /**
     * Constructs a new Pageable with the page number, page size and Sort.
     *
     * @param pageNumber the zero-based page number
     * @param pageSize   the number of items per page
     * @param sort       the sorting criteria
     * @throws DomainException if pageNumber is negative, pageSize is less than 1, or sort is {@code null}
     */
    private SimplePageable(int pageNumber, int pageSize, Sort sort) {
        if (pageNumber < 0) throw new DomainException("The page number must not be less than zero");
        if (pageSize < 1) throw new DomainException("The page size must not be less than one");
        if (sort == null) throw new DomainException("The sort must not be null");

        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.sort = sort;
    }

    /**
     * Creates and returns a new default {@link Pageable} instance configured
     * for the first page using the default page size and no sorting.
     *
     * @return a new default {@link Pageable} instance
     */
    public static Pageable of() {
        return of(DEFAULT_PAGE_SIZE);
    }

    /**
     * Creates a Pageable for the first page with the specified page size.
     *
     * @param pageSize the number of items per page
     * @return a new {@link Pageable} instance for the first page
     * @throws DomainException if pageSize is less than {@code 1}
     */
    public static Pageable of(int pageSize) {
        return of(0, pageSize);
    }

    /**
     * Creates and returns a new {@link Pageable} instance configured for the
     * first page using the default page size and the specified sorting
     * criteria.
     *
     * @param sort the sorting criteria to apply
     * @return a new {@link Pageable} instance
     */
    public static Pageable of(Sort sort) {
        return of(DEFAULT_PAGE_SIZE, sort);
    }

    /**
     * Creates a Pageable for the first page with the specified page size and
     * sort criteria.
     *
     * @param pageSize the number of items per page
     * @param sort     the sorting criteria
     * @return a new {@link Pageable} instance for the first page with sorting
     * @throws DomainException if pageSize is less than {@code 1} or sort is {@code null}
     */
    public static Pageable of(int pageSize, Sort sort) {
        return of(0, pageSize, sort);
    }

    /**
     * Creates a Pageable with the specified page number and page size.
     *
     * @param pageNumber the zero-based page number
     * @param pageSize   the number of items per page
     * @return a new {@link Pageable} instance with no sorting
     * @throws DomainException if pageNumber is negative or pageSize is less than {@code 1}
     */
    public static Pageable of(int pageNumber, int pageSize) {
        return of(pageNumber, pageSize, Sort.unsorted());
    }

    /**
     * Creates a Pageable with the specified page number, page size, and sort
     * criteria.
     *
     * @param pageNumber the zero-based page number
     * @param pageSize   the number of items per page
     * @param sort       the sorting criteria
     * @return a new {@link Pageable} instance with the specified parameters
     * @throws DomainException if pageNumber is negative, pageSize is less than {@code 1}, or sort is {@code null}
     */
    public static Pageable of(int pageNumber, int pageSize, Sort sort) {
        return new SimplePageable(pageNumber, pageSize, sort);
    }

    @Override
    public Pageable nextPage() {
        return new SimplePageable(getPageNumber() + 1, getPageSize(), getSort());
    }

    @Override
    public Pageable previousOrFirstPage() {
        return hasPrevious() ? previousPage() : firstPage();
    }

    /**
     * Returns a Pageable for the previous page.
     *
     * @return a new {@link Pageable} instance for the previous page
     */
    private Pageable previousPage() {
        return new SimplePageable(getPageNumber() - 1, getPageSize(), getSort());
    }

    @Override
    public Pageable firstPage() {
        return new SimplePageable(0, getPageSize(), getSort());
    }

    @Override
    public boolean hasPrevious() {
        return getPageNumber() > 0;
    }

    @Override
    public int getPageNumber() {
        return this.pageNumber;
    }

    @Override
    public int getPageSize() {
        return this.pageSize;
    }

    @Override
    public long getOffset() {
        return (long) getPageNumber() * getPageSize();
    }

    @Override
    public Sort getSort() {
        return this.sort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Pageable that)) return false;

        return this.pageNumber == that.getPageNumber() &&
                this.pageSize == that.getPageSize() &&
                Objects.equals(this.sort, that.getSort());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pageNumber, this.pageSize, this.sort);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Pageable.class.getSimpleName() + "[", "]")
                .add("page=" + this.pageNumber)
                .add("size=" + this.pageSize)
                .add("sort=" + this.sort)
                .toString();
    }
}
