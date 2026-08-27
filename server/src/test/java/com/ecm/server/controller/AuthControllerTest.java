package com.ecm.server.controller;

import com.ecm.server.dto.request.LoginRequest;
import com.ecm.server.dto.request.RegisterRequest;
import com.ecm.server.dto.response.AuthResponse;
import com.ecm.server.dto.response.UserSummaryResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_whenValidUsernameAndPassword_shouldReturn200AndAuthResponse() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .username("testuser")
                .password("Password123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(UserSummaryResponse.builder()
                        .id(UUID.randomUUID())
                        .username("testuser")
                        .role("ROLE_CUSTOMER")
                        .fullName("Test User")
                        .email("test@example.com")
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20000))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.user.username").value("testuser"));
    }

    @Test
    void register_whenValidPayload_shouldReturn200() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .password("Password123")
                .fullName("New User")
                .email("newuser@example.com")
                .phone("0987654321")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(20000));
    }
}
