package com.ecm.server.exception;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertFalse(response.getBody().isSuccess());
        assertEquals(StatusCode.PRODUCT_NOT_FOUND.getCode(), response.getBody().getCode());
        assertEquals(StatusCode.PRODUCT_NOT_FOUND.getMessage(), response.getBody().getMessage());
    }

    @Test
    void handleBusinessException_withCustomMessage_shouldReturnCustomMessage() {
        String customMsg = "Product with ID 123 does not exist";
        BusinessException ex = new BusinessException(StatusCode.PRODUCT_NOT_FOUND, customMsg);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(StatusCode.PRODUCT_NOT_FOUND.getCode(), response.getBody().getCode());
        assertEquals(customMsg, response.getBody().getMessage());
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception ex = new RuntimeException("Unexpected DB connection drop");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGenericException(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals(StatusCode.INTERNAL_SERVER_ERROR.getCode(), response.getBody().getCode());
    }

    @Test
    void apiResponse_successBuilder_shouldSetSuccessTrue() {
        ApiResponse<String> response = ApiResponse.success("Data payload");

        assertTrue(response.isSuccess());
        assertEquals(StatusCode.SUCCESS.getCode(), response.getCode());
        assertEquals("Data payload", response.getData());
    }
}
