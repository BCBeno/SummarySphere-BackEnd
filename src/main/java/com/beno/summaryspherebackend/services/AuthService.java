package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.dtos.AuthSchema;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.Role;
import com.beno.summaryspherebackend.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;

    public AuthSchema.AuthResponse register(AuthSchema.RegisterRequest request) {
        return register(request, "unknown");
    }

    public AuthSchema.AuthResponse register(AuthSchema.RegisterRequest request, String ip) {
        if (rateLimitService != null) {
            rateLimitService.checkRegistration(ip);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use: " + request.email());
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // always hash the password!
                .role(Role.USER)
                .emailVerified(false)
                .build();

        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return toAuthResponse(token, user);
    }

    public AuthSchema.AuthResponse login(AuthSchema.LoginRequest request) {
        return login(request, "unknown");
    }

    public AuthSchema.AuthResponse login(AuthSchema.LoginRequest request, String ip) {
        if (rateLimitService != null) {
            rateLimitService.checkLogin(ip);
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtService.generateToken(user);
        return toAuthResponse(token, user);
    }

    public void requestEmailVerification(User user, String ip) {
        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }
        if (rateLimitService != null) {
            rateLimitService.checkEmailVerification(user.getEmail(), ip);
        }

        String verificationToken = UUID.randomUUID().toString();

        user.setEmailVerificationTokenHash(sha256(verificationToken));
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        emailService.sendEmailVerificationEmail(user.getEmail(), verificationToken);
    }

    @Transactional
    public void confirmEmailVerification(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Verification token is required");
        }

        User user = userRepository.findByEmailVerificationTokenHash(sha256(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification link"));

        LocalDateTime expiry = user.getEmailVerificationTokenExpiry();
        if (expiry == null || expiry.isBefore(LocalDateTime.now())) {
            user.setEmailVerificationTokenHash(null);
            user.setEmailVerificationTokenExpiry(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Verification link has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);
    }


    public void forgotPassword(String email) {
        forgotPassword(email, "unknown");
    }

    public void forgotPassword(String email, String ip) {
        if (rateLimitService != null) {
            rateLimitService.checkForgotPassword(email, ip);
        }
        try {
            var user = userRepository.findByEmail(email);
            if (user.isEmpty()) {
                return;
            }

            User foundUser = user.get();
            String resetToken = UUID.randomUUID().toString();
            foundUser.setResetToken(resetToken);
            foundUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(foundUser);

            // Send email asynchronously - no need to wait for completion
            emailService.sendResetPasswordEmail(email, resetToken);
        } catch (Exception ignored) {
            // Always return success to the client for forgot-password flows.
            // This prevents email enumeration attacks.
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private AuthSchema.AuthResponse toAuthResponse(String token, User user) {
        return new AuthSchema.AuthResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.isEmailVerified());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

}
