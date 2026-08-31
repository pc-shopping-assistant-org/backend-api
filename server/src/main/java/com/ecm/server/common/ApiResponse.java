package com.ecm.server.common;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Canonical API response envelope.
 *
 * <p>The frontend only needs three stable fields: {@code data}, a static
 * {@code message} key, and {@code errors} as an array. HTTP status remains the
 * transport-level status; this envelope deliberately does not duplicate it
 * with a JSON {@code code} or {@code success} field.</p>
 *
 * @param <T> response data payload type
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"data", "message", "errors"})
public class ApiResponse<T> {

    private T data;

    /**
     * Stable frontend mapping key. Request-specific details belong in errors;
     * the top-level message is never translated prose.
     */
    @Schema(description = "Static frontend mapping key; request-specific details belong in errors", example = "SUCCESS")
    @Builder.Default
    private String message = StatusCode.SUCCESS.name();

    @Builder.Default
    private List<ApiError> errors = new ArrayList<>();

    public static <T> ApiResponse<T> success(T data) {
        return success(StatusCode.SUCCESS, data);
    }

    public static <T> ApiResponse<CursorPageResponse<T>> cursor(CursorPageResponse<T> pageData) {
        return success(StatusCode.SUCCESS, pageData);
    }

    public static <T> ApiResponse<PageResponse<T>> paginated(PageResponse<T> pageData) {
        return success(StatusCode.SUCCESS, pageData);
    }

    public static <T> ApiResponse<T> success(StatusCode statusCode, T data) {
        StatusCode messageCode = messageCode(statusCode);
        return ApiResponse.<T>builder()
                .data(data)
                .message(messageCode.name())
                .errors(new ArrayList<>())
                .build();
    }

    public static <T> ApiResponse<T> error(StatusCode statusCode) {
        return error(statusCode, null, null);
    }

    public static <T> ApiResponse<T> error(StatusCode statusCode, String detailMessage) {
        return error(statusCode, detailMessage, null);
    }

    public static <T> ApiResponse<T> error(StatusCode statusCode, String detailMessage, Object details) {
        StatusCode messageCode = messageCode(statusCode);
        return ApiResponse.<T>builder()
                .data(null)
                .message(messageCode.name())
                .errors(toErrors(detailMessage, messageCode, details))
                .build();
    }

    private static StatusCode messageCode(StatusCode statusCode) {
        return statusCode == null ? StatusCode.INTERNAL_SERVER_ERROR : statusCode;
    }

    private static List<ApiError> toErrors(String detailMessage, StatusCode statusCode, Object details) {
        List<ApiError> result = new ArrayList<>();

        if (details instanceof Map<?, ?> map) {
            map.forEach((key, value) -> result.add(ApiError.builder()
                    .field(key == null ? null : key.toString())
                    .message(value == null ? null : value.toString())
                    .build()));
        } else if (details instanceof Collection<?> collection) {
            collection.forEach(item -> result.add(ApiError.builder()
                    .message(item == null ? null : item.toString())
                    .build()));
        } else if (details != null) {
            result.add(ApiError.builder().message(details.toString()).build());
        }

        if (detailMessage != null && !detailMessage.isBlank()
                && (statusCode == null || !detailMessage.equals(statusCode.getMessage()))) {
            result.add(0, ApiError.builder().message(detailMessage).build());
        }

        return result;
    }

}
