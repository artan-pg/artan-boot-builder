package ir.artanpg.boot.domain.model;

import ir.artanpg.boot.domain.exception.DomainException;

import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default implementation for the {@link Page} interface.
 *
 * @param <T> the type of content in the page
 * @author Mohammad Yazdian
 * @since 0.1.0
 */
public class SimplePage<T> implements Page<T> {

    @Serial
    private static final long serialVersionUID = 4221897366779878549L;

    /**
     * The actual content of this page as an immutable list.
     */
    private final List<T> content;

    /**
     * The pagination information associated with this page.
     */
    private final Pageable pageable;

    /**
     * The total number of elements across all pages.
     */
    private final long totalElements;

    /**
     * The total number of pages available.
     */
    private final int totalPages;

    /**
     * Flag indicating whether this page is the first page.
     */
    private final boolean first;

    /**
     * Flag indicating whether this page is the last page.
     */
    private final boolean last;

    /**
     * Constructs a new Page with the provided content, pagination information,
     * and total elements.
     *
     * <p>This constructor automatically calculates the total pages and
     * navigation flags based on the provided parameters.
     *
     * @param content       the content of the page
     * @param pageable      the pagination information
     * @param totalElements the total number of elements across all pages
     * @throws DomainException if content is {@code null}
     * @throws DomainException if pageable is {@code null}
     */
    private SimplePage(List<T> content, Pageable pageable, long totalElements) {
        if (content == null) throw new DomainException("The content list must not be null");
        if (pageable == null) throw new DomainException("The pageable list must not be null");
        if (totalElements < 0) throw new DomainException("The total elements must not be negative");

        this.content = Collections.unmodifiableList(content);
        this.pageable = pageable;

        this.totalElements = totalElements;
        this.totalPages =
                (getTotalElements() == 0) ? 0 :
                        (int) Math.ceil(getTotalElements() / (double) getPageable().getPageSize());

        this.first = pageable.getPageNumber() == 0;
        this.last = pageable.getPageNumber() + 1 >= getTotalPages();
    }

    /**
     * Creates a new Page from a list of content and pagination information.
     *
     * <p>The total elements count is derived from the content size, making
     * this method suitable for cases where you have all data loaded in memory.
     *
     * @param content       the content of the page
     * @param pageable      the pagination information
     * @param totalElements the total number of elements across all pages
     * @param <T>           the type of content in the page
     * @return a new {@link Page} instance
     * @throws DomainException if content is {@code null}
     * @throws DomainException if pageable is {@code null}
     */
    public static <T> Page<T> of(List<T> content, Pageable pageable, long totalElements) {
        return new SimplePage<>(content, pageable, totalElements);
    }

    /**
     * Creates an empty Page with the given pagination information.
     *
     * <p>This method is useful when a query returns no results, but you still
     * want to preserve pagination metadata.
     *
     * @param pageable the pagination information
     * @param <T>      the type of content in the page
     * @return an empty new {@link Page} instance
     * @throws DomainException if pageable is {@code null}
     */
    public static <T> Page<T> empty(Pageable pageable) {
        return new SimplePage<>(Collections.emptyList(), pageable, 0);
    }

    @Override
    public <U> Page<U> map(Function<? super T, ? extends U> converter) {
        if (converter == null) throw new DomainException("Function converter must not be null");

        List<U> mappedContent = getContent().stream()
                .map(converter)
                .collect(Collectors.toList());

        return new SimplePage<>(mappedContent, getPageable(), getTotalElements());
    }

    @Override
    public List<T> getContent() {
        return this.content;
    }

    @Override
    public Pageable getPageable() {
        return this.pageable;
    }

    @Override
    public long getTotalElements() {
        return this.totalElements;
    }

    @Override
    public int getTotalPages() {
        return this.totalPages;
    }

    @Override
    public boolean isFirst() {
        return this.first;
    }

    @Override
    public boolean isLast() {
        return this.last;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Page<?> page)) return false;
        return Objects.equals(this.content, page.getContent()) &&
                Objects.equals(this.pageable, page.getPageable());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.content, this.pageable);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Page.class.getSimpleName() + "[", "]")
                .add("content=" + this.content)
                .add("pageable=" + this.pageable)
                .add("totalElements=" + this.totalElements)
                .add("totalPages=" + this.totalPages)
                .add("first=" + this.first)
                .add("last=" + this.last)
                .toString();
    }
}
