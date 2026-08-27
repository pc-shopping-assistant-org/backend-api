package com.ecm.server.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Standard cursor request query parameters.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageRequest {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 100;
    public static final int MIN_LIMIT = 1;

    /**
     * Opaque cursor string from previous response's nextCursor.
     */
    private String cursor;

    /**
     * Page size limit.
     */
    @Builder.Default
    @Min(value = MIN_LIMIT, message = "Limit must be at least 1")
    @Max(value = MAX_LIMIT, message = "Limit must not exceed 100")
    private Integer limit = DEFAULT_LIMIT;

    /**
     * Ensure limit stays within safe boundaries.
     */
    public int getSanitizedLimit() {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * Query limit (limit + 1) to determine if next page exists without count query.
     */
    public int getQueryLimit() {
        return getSanitizedLimit() + 1;
    }
}
