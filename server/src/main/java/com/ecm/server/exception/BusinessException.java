package com.ecm.server.exception;

import com.ecm.server.common.StatusCode;
import lombok.Getter;

/**
 * Custom runtime exception thrown when a business rule or validation fails.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final StatusCode statusCode;
    private final Object details;

    public BusinessException(StatusCode statusCode) {
        super(statusCode.getMessage());
        this.statusCode = statusCode;
        this.details = null;
    }

    public BusinessException(StatusCode statusCode, String customMessage) {
        super(customMessage);
        this.statusCode = statusCode;
        this.details = null;
    }

    public BusinessException(StatusCode statusCode, Object details) {
        super(statusCode.getMessage());
        this.statusCode = statusCode;
        this.details = details;
    }

    public BusinessException(StatusCode statusCode, String customMessage, Object details) {
        super(customMessage);
        this.statusCode = statusCode;
        this.details = details;
    }
}
