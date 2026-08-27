package com.ecm.server.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Standard cursor-based pagination response envelope.
 *
 * @param <T> Item type
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursorPageResponse<T> {

    @Builder.Default
    private List<T> items = Collections.emptyList();

    private String nextCursor;
    private String prevCursor;
    private boolean hasNext;
    private boolean hasPrev;
    private int size;

    /**
     * Build cursor response from a query result fetched with (limit + 1) items.
     *
     * @param rawItems        Result list queried with size (limit + 1)
     * @param limit           Requested page size
     * @param cursorExtractor Extractor for cursor value (e.g. Entity::getId or Entity::getCreatedAt)
     */
    public static <T> CursorPageResponse<T> of(
            List<T> rawItems,
            int limit,
            Function<T, String> cursorExtractor
    ) {
        if (rawItems == null || rawItems.isEmpty()) {
            return CursorPageResponse.<T>builder()
                    .items(Collections.emptyList())
                    .hasNext(false)
                    .size(0)
                    .build();
        }

        boolean hasNext = rawItems.size() > limit;
        List<T> pageItems = hasNext ? rawItems.subList(0, limit) : rawItems;

        String nextCursor = (hasNext && !pageItems.isEmpty())
                ? cursorExtractor.apply(pageItems.get(pageItems.size() - 1))
                : null;

        return CursorPageResponse.<T>builder()
                .items(pageItems)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(pageItems.size())
                .build();
    }

    /**
     * Build cursor response while transforming Entity items to DTO items.
     *
     * @param rawItems        Result list queried with size (limit + 1)
     * @param limit           Requested page size
     * @param cursorExtractor Extractor for cursor value from Entity
     * @param mapper          DTO mapping function
     */
    public static <T, R> CursorPageResponse<R> of(
            List<T> rawItems,
            int limit,
            Function<T, String> cursorExtractor,
            Function<T, R> mapper
    ) {
        if (rawItems == null || rawItems.isEmpty()) {
            return CursorPageResponse.<R>builder()
                    .items(Collections.emptyList())
                    .hasNext(false)
                    .size(0)
                    .build();
        }

        boolean hasNext = rawItems.size() > limit;
        List<T> pageItems = hasNext ? rawItems.subList(0, limit) : rawItems;

        String nextCursor = (hasNext && !pageItems.isEmpty())
                ? cursorExtractor.apply(pageItems.get(pageItems.size() - 1))
                : null;

        List<R> mappedItems = pageItems.stream()
                .map(mapper)
                .toList();

        return CursorPageResponse.<R>builder()
                .items(mappedItems)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .size(mappedItems.size())
                .build();
    }
}
