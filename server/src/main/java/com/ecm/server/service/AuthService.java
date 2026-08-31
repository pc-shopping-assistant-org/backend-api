package com.ecm.server.service;

import com.ecm.server.dto.request.*;
import com.ecm.server.dto.response.AuthResponse;

public interface AuthService {

    void register(RegisterRequest request);

    AuthResponse verifyRegistrationOtp(VerifyOtpRequest request);

    void resendOtp(ResendOtpRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String bearerToken, String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
