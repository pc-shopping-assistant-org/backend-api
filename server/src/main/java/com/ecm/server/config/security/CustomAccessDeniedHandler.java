package com.ecm.server.config.security;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("Access denied error: path={}, message={}", request.getRequestURI(), accessDeniedException.getMessage());

        StatusCode statusCode = StatusCode.FORBIDDEN;
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(statusCode.getHttpStatus().value());

        ApiResponse<Void> apiResponse = ApiResponse.error(statusCode, statusCode.getMessage());
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
