package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<Void> sendResetPasswordEmail(String email, String token) {
        try {
            log.info("Starting async password reset email to: {}", email);
            String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("SummarySphere - Password Reset Request");
            message.setText("Click the link to reset your password: " + resetLink +
                    "\n\nThis link will expire in 1 hour.");

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", email);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            // Log the error but don't fail - the reset token is already saved in DB
            return CompletableFuture.failedFuture(e);
        }
    }
}
