package com.ecm.server.service.impl;

import com.ecm.server.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        // 1. Check if SMTP mail sender is configured with valid sender address
        if (!StringUtils.hasText(fromEmail)) {
            log.info("Mail sender not configured (MAIL_USERNAME is empty). OTP for [{}] ({}) is: {}", toEmail, purpose, otp);
            return;
        }

        // 2. Construct email subject and body
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[ECM Platform] Mã xác thực OTP - " + purpose);
            message.setText("Xin chào,\n\n"
                    + "Mã xác thực OTP của bạn cho yêu cầu " + purpose + " là: " + otp + "\n"
                    + "Mã này có hiệu lực trong 5 phút.\n\n"
                    + "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.\n\n"
                    + "Trân trọng,\nĐội ngũ ECM Platform");

            // 3. Dispatch email through SMTP server
            mailSender.send(message);
            log.info("OTP email successfully sent to [{}] for purpose [{}]", toEmail, purpose);
        } catch (Exception e) {
            log.error("Failed to send OTP email to [{}]: {}", toEmail, e.getMessage());
        }
    }
}
