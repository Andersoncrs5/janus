package org.janus.shared.domain.page;

import java.util.Collections;
import java.util.List;

public class Page<T> {

    private List<T> content;
    private long totalElements;
    private int page;
    private int size;
    private int totalPages;

    private boolean first;
    private boolean last;

    public Page(
            List<T> content,
            long totalElements,
            int page,
            int size
    ) {
        this.content = content != null ? content : Collections.emptyList();
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;

        this.totalPages = size > 0
                ? (int) Math.ceil((double) totalElements / size)
                : 0;

        this.first = page == 0;
        this.last = page >= totalPages - 1;
    }

    public static <T> Page<T> of(List<T> content, long totalElements, int page, int size) {
        return new Page<>(content, totalElements, page, size);
    }

    public static <T> Page<T> empty(int page, int size) {
        return new Page<>(Collections.emptyList(), 0, page, size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public <R> Page<R> map(java.util.function.Function<T, R> mapper) {
        List<R> converted = this.content.stream().map(mapper).toList();
        return new Page<>(converted, this.totalElements, this.page, this.size);
    }

    public List<T> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}