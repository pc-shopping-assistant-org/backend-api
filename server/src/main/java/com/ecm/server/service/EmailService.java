package com.ecm.server.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp, String purpose);
}
