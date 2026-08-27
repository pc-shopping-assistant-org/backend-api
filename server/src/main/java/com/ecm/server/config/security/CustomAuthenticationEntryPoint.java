package com.ecm.server.config.security;

import com.ecm.server.common.ApiResponse;
import com.ecm.server.common.StatusCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("Unauthorized access error: path={}, message={}", request.getRequestURI(), authException.getMessage());

        StatusCode statusCode = StatusCode.UNAUTHORIZED;
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(statusCode.getHttpStatus().value());

        ApiResponse<Void> apiResponse = ApiResponse.error(statusCode, statusCode.getMessage());
        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
