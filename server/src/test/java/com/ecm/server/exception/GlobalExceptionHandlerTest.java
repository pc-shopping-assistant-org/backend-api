package com.ecm.server.exception;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_shouldReturnCorrectStatusCodeAndMessage() {
        BusinessException ex = new BusinessException(StatusCode.PRODUCT_NOT_FOUND);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(StatusCode.PRODUCT_NOT_FOUND.name(), response.getBody().getMessage());
        assertEquals(0, response.getBody().getErrors().size());
    }

    @Test
    void handleBusinessException_withCustomMessage_shouldKeepStaticKeyAndExposeDetailInErrors() {
        String customMsg = "Product with ID 123 does not exist";
        BusinessException ex = new BusinessException(StatusCode.PRODUCT_NOT_FOUND, customMsg);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(StatusCode.PRODUCT_NOT_FOUND.name(), response.getBody().getMessage());
        assertEquals(customMsg, response.getBody().getErrors().getFirst().getMessage());
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception ex = new RuntimeException("Unexpected DB connection drop");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGenericException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(StatusCode.INTERNAL_SERVER_ERROR.name(), response.getBody().getMessage());
    }

    @Test
    void apiResponse_successBuilder_shouldExposeCanonicalEnvelope() {
        ApiResponse<String> response = ApiResponse.success("Data payload");

        assertEquals("Data payload", response.getData());
        assertEquals(StatusCode.SUCCESS.name(), response.getMessage());
        assertEquals(0, response.getErrors().size());
    }

    @Test
    void handleBusinessException_usesStaticMessageKey_forCredentialErrors() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(
                new BusinessException(StatusCode.INVALID_CREDENTIALS, "Account lookup failed"));

        assertEquals(StatusCode.INVALID_CREDENTIALS.name(), response.getBody().getMessage());
        assertEquals("Account lookup failed", response.getBody().getErrors().getFirst().getMessage());
    }

    @Test
    void handleBindException_keepsStaticValidationMessageAndFieldErrors() {
        BindException exception = new BindException(new Object(), "filter");
        exception.addError(new FieldError("filter", "limit", "Limit must be at least 1"));

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBindException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(StatusCode.VALIDATION_ERROR.name(), response.getBody().getMessage());
        assertEquals("limit", response.getBody().getErrors().getFirst().getField());
        assertEquals("Limit must be at least 1", response.getBody().getErrors().getFirst().getMessage());
    }

    @Test
    void handleMethodNotSupported_usesMethodNotAllowedStaticKey() {
        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("DELETE"));

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals(StatusCode.METHOD_NOT_ALLOWED.name(), response.getBody().getMessage());
        assertEquals("HTTP method 'DELETE' is not supported for this endpoint",
                response.getBody().getErrors().getFirst().getMessage());
    }
}
