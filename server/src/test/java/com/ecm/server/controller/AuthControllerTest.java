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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
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
    void login_whenValidIdentifierAndPassword_shouldReturn200AndAuthResponse() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .identifier("test@example.com")
                .password("Password123")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .tokenType("Bearer")
                .expiresIn(86400)
                .user(UserSummaryResponse.builder()
                        .id(UUID.randomUUID())
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
                .andExpect(jsonPath("$.message").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    void register_whenValidPayload_shouldReturn200() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .password("Password123")
                .fullName("New User")
                .email("newuser@example.com")
                .phone("0987654321")
                .address("123 Example Street, Ho Chi Minh City")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"));
    }

    @Test
    void forgotPassword_acceptsPhoneIdentifier() throws Exception {
        doNothing().when(authService).forgotPassword(any());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0987654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"));

        verify(authService).forgotPassword(any());
    }

    @Test
    void resetPassword_acceptsPhoneIdentifier() throws Exception {
        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"0987654321\",\"otp\":\"123456\",\"newPassword\":\"NewPassword1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"));

        verify(authService).resetPassword(any());
    }

    @Test
    void logout_passesBearerTokenToRevocationService() throws Exception {
        doNothing().when(authService).logout("Bearer access-token", null);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"));

        verify(authService).logout("Bearer access-token", null);
    }

    @Test
    void logout_withRefreshToken_revokesTheTokenPair() throws Exception {
        doNothing().when(authService).logout("Bearer access-token", "refresh-token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SUCCESS"));

        verify(authService).logout("Bearer access-token", "refresh-token");
    }
}
