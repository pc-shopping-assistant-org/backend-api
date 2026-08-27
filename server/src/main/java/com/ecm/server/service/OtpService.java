package com.ecm.server.service;

import com.ecm.server.dto.request.RegisterRequest;

public interface OtpService {

    String generateAndSaveOtp(String email, String purpose);

    boolean verifyOtp(String email, String purpose, String otp);

    void deleteOtp(String email, String purpose);

    void savePendingRegistration(String email, RegisterRequest request);

    RegisterRequest getPendingRegistration(String email);

    void deletePendingRegistration(String email);
}
