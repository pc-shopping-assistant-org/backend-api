package com.ecm.server.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

/**
 * Standard API Response envelope.
 *
 * @param <T> Response data payload type
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private int code;
    private String message;
    private T data;
    private Object errors;

    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(StatusCode.SUCCESS.getCode())
                .message(StatusCode.SUCCESS.getMessage())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<CursorPageResponse<T>> cursor(CursorPageResponse<T> pageData) {
        return ApiResponse.<CursorPageResponse<T>>builder()
                .success(true)
                .code(StatusCode.SUCCESS.getCode())
                .message(StatusCode.SUCCESS.getMessage())
                .data(pageData)
                .build();
    }

    public static <T> ApiResponse<PageResponse<T>> paginated(PageResponse<T> pageData) {
        return ApiResponse.<PageResponse<T>>builder()
                .success(true)
                .code(StatusCode.SUCCESS.getCode())
                .message(StatusCode.SUCCESS.getMessage())
                .data(pageData)
                .build();
    }

    public static <T> ApiResponse<T> success(StatusCode statusCode, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(statusCode.getCode())
                .message(statusCode.getMessage())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(StatusCode statusCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(statusCode.getCode())
                .message(statusCode.getMessage())
                .build();
    }

    public static <T> ApiResponse<T> error(StatusCode statusCode, String customMessage) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(statusCode.getCode())
                .message(customMessage)
                .build();
    }

    public static <T> ApiResponse<T> error(StatusCode statusCode, String customMessage, Object errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(statusCode.getCode())
                .message(customMessage)
                .errors(errors)
                .build();
    }
}
