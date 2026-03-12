package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public void sendResetPasswordEmail(String email, String token) {
        String resetLink = "http://localhost:5173/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("SummarySphere - Password Reset Request");
        message.setText("Click the link to reset your password: " + resetLink +
                "\n\nThis link will expire in 1 hour.");

        mailSender.send(message);
    }
}
