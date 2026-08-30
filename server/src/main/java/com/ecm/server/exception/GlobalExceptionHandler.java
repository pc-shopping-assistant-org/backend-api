package com.ecm.server.exception;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global centralized exception handler.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle business exceptions explicitly thrown by the application.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception occurred: code={}, message={}",
                ex.getStatusCode().getCode(), ex.getMessage());

        StatusCode statusCode = ex.getStatusCode();
        ApiResponse<Void> response = ApiResponse.error(
                statusCode,
                ex.getMessage(),
                ex.getDetails()
        );

        return ResponseEntity.status(statusCode.getHttpStatus()).body(response);
    }

    /**
     * Handle validation errors from @Valid request payloads.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Validation failed for request: {}", errors);

        StatusCode statusCode = StatusCode.VALIDATION_ERROR;
        ApiResponse<Void> response = ApiResponse.error(
                statusCode,
                statusCode.getMessage(),
                errors
        );

        return ResponseEntity.status(statusCode.getHttpStatus()).body(response);
    }

    /**
     * Handle constraint violations (e.g. query param validation).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        log.warn("Constraint violation: {}", errors);

        StatusCode statusCode = StatusCode.VALIDATION_ERROR;
        ApiResponse<Void> response = ApiResponse.error(
                statusCode,
                statusCode.getMessage(),
                errors
        );

        return ResponseEntity.status(statusCode.getHttpStatus()).body(response);
    }

    /**
     * Handle missing required request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParams(MissingServletRequestParameterException ex) {
        StatusCode statusCode = StatusCode.MISSING_REQUIRED_PARAMETER;
        String message = String.format("Parameter '%s' of type %s is required", ex.getParameterName(), ex.getParameterType());

        ApiResponse<Void> response = ApiResponse.error(statusCode, message);
        return ResponseEntity.status(statusCode.getHttpStatus()).body(response);
    }

    /**
     * Handle unreadable/malformed HTTP request payloads.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON payload: {}", ex.getMessage());

        StatusCode statusCode = StatusCode.MALFORMED_JSON;
        ApiResponse<Void> response = ApiResponse.error(statusCode);
        return ResponseEntity.status(statusCode.getHttpStatus()).body(response);
    }

    /**
     * Handle unsupported HTTP methods.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        StatusCode statusCode = StatusCode.BAD_REQUEST;
        String message = String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod());

        ApiResponse<Void> response = ApiResponse.error(statusCode, message);
        return ResponseEntity.status(statusCode.getHttpStatus()).body(response);
    }

    /**
     * Handle endpoint/resource not found (Spring 6 / Boot 3/4).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        StatusCode statusCode = StatusCode.ENDPOINT_NOT_FOUND;
        ApiResponse<Void> response = ApiResponse.error(statusCode, "API endpoint not found: " + ex.getResourcePath());
        return ResponseEntity.status(statusCode.getHttpStatus()).body(response);
    }

    /**
     * Handle Redis connection failure / timeout outage.
     */
    @ExceptionHandler({
            org.springframework.data.redis.RedisConnectionFailureException.class,
            org.springframework.data.redis.RedisSystemException.class,
            org.springframework.dao.QueryTimeoutException.class,
            io.lettuce.core.RedisException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleRedisException(Exception ex) {
        log.error("Redis connection outage detected: {}", ex.getMessage());

        StatusCode statusCode = StatusCode.SERVICE_UNAVAILABLE;
        ApiResponse<Void> response = ApiResponse.error(
                statusCode,
                "Cache or session service is temporarily unavailable. Please try again shortly."
        );
        return ResponseEntity.status(statusCode.getHttpStatus()).body(response);
    }

    /**
     * Handle all uncaught runtime exceptions.
     */
    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: ", ex);

        StatusCode statusCode = StatusCode.INTERNAL_SERVER_ERROR;
        ApiResponse<Void> response = ApiResponse.error(statusCode);
        return ResponseEntity.status(statusCode.getHttpStatus()).body(response);
    }
}
