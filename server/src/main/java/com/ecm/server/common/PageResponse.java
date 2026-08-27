package com.ecm.server.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Standard pagination metadata and items wrapper.
 *
 * @param <T> Item type
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    @Builder.Default
    private List<T> items = Collections.emptyList();

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean isFirst;
    private boolean isLast;

    /**
     * Map directly from Spring Data Page (1-indexed page for API consumers).
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        if (page == null) {
            return PageResponse.<T>builder().build();
        }

        return PageResponse.<T>builder()
                .items(page.getContent())
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }

    /**
     * Map from Spring Data Page while transforming entity items into DTO items.
     */
    public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
        if (page == null) {
            return PageResponse.<R>builder().build();
        }

        List<R> mappedItems = page.getContent().stream()
                .map(mapper)
                .toList();

        return PageResponse.<R>builder()
                .items(mappedItems)
                .page(page.getNumber() + 1)
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
