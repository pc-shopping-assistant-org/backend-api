package com.ecm.server.service.impl;

import com.ecm.server.dto.request.RegisterRequest;
import com.ecm.server.service.OtpService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    public static final int OTP_MIN_VALUE = 100000;
    public static final int OTP_BOUND = 900000;
    public static final long OTP_TTL_MINUTES = 5;
    public static final long REGISTRATION_DATA_TTL_MINUTES = 10;
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String REG_DATA_KEY_PREFIX = "reg_data:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final com.ecm.server.service.EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateAndSaveOtp(String email, String purpose) {
        // 1. Generate secure 6-digit random OTP code
        String otp = String.valueOf(OTP_MIN_VALUE + secureRandom.nextInt(OTP_BOUND));

        // 2. Store OTP in Redis with 5-minute time-to-live (TTL)
        String key = buildOtpKey(email, purpose);
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(OTP_TTL_MINUTES));

        // 3. Dispatch OTP via email service and log for development
        log.info("Generated OTP for [{}] with purpose [{}]: {}", email, purpose, otp);
        emailService.sendOtpEmail(email, otp, purpose);
        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String purpose, String otp) {
        // 1. Retrieve cached OTP from Redis
        String key = buildOtpKey(email, purpose);
        String cachedOtp = redisTemplate.opsForValue().get(key);

        // 2. Match provided OTP against cached value
        return cachedOtp != null && cachedOtp.equals(otp);
    }

    @Override
    public void deleteOtp(String email, String purpose) {
        // 1. Evict OTP key from Redis upon successful verification
        String key = buildOtpKey(email, purpose);
        redisTemplate.delete(key);
    }

    @Override
    public void savePendingRegistration(String email, RegisterRequest request) {
        // 1. Serialize registration DTO to JSON
        try {
            String json = objectMapper.writeValueAsString(request);
            // 2. Cache registration payload in Redis with 10-minute TTL pending OTP verification
            String key = REG_DATA_KEY_PREFIX + email.toLowerCase();
            redisTemplate.opsForValue().set(key, json, Duration.ofMinutes(REGISTRATION_DATA_TTL_MINUTES));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize pending registration data for {}: {}", email, e.getMessage());
        }
    }

    @Override
    public RegisterRequest getPendingRegistration(String email) {
        // 1. Fetch cached registration JSON from Redis
        String key = REG_DATA_KEY_PREFIX + email.toLowerCase();
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }

        // 2. Deserialize JSON back into RegisterRequest DTO
        try {
            return objectMapper.readValue(json, RegisterRequest.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize pending registration data for {}: {}", email, e.getMessage());
            return null;
        }
    }

    @Override
    public void deletePendingRegistration(String email) {
        // 1. Evict temporary registration payload from Redis
        String key = REG_DATA_KEY_PREFIX + email.toLowerCase();
        redisTemplate.delete(key);
    }

    private String buildOtpKey(String email, String purpose) {
        return OTP_KEY_PREFIX + purpose.toUpperCase() + ":" + email.toLowerCase();
    }
}
