package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.exceptions.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitServiceTest {
    private final RateLimitService rateLimitService = new RateLimitService();

    @Test
    void login_allowsTenAttemptsAndRejectsTheEleventh() {
        for (int attempt = 0; attempt < 10; attempt++) {
            assertDoesNotThrow(() -> rateLimitService.checkLogin("192.0.2.1"));
        }

        assertThrows(RateLimitExceededException.class, () -> rateLimitService.checkLogin("192.0.2.1"));
    }

    @Test
    void registration_allowsThreeAccountsAndRejectsTheFourth() {
        for (int attempt = 0; attempt < 3; attempt++) {
            assertDoesNotThrow(() -> rateLimitService.checkRegistration("192.0.2.2"));
        }

        assertThrows(RateLimitExceededException.class, () -> rateLimitService.checkRegistration("192.0.2.2"));
    }

    @Test
    void forgotPassword_limitsEmailAndIpIndependently() {
        for (int attempt = 0; attempt < 3; attempt++) {
            String ip = "192.0.2." + attempt;
            assertDoesNotThrow(() -> rateLimitService.checkForgotPassword("user@example.com", ip));
        }
        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.checkForgotPassword("user@example.com", "192.0.2.10"));

        RateLimitService ipLimit = new RateLimitService();
        for (int attempt = 0; attempt < 10; attempt++) {
            String email = "user" + attempt + "@example.com";
            assertDoesNotThrow(() -> ipLimit.checkForgotPassword(email, "192.0.2.3"));
        }
        assertThrows(RateLimitExceededException.class,
                () -> ipLimit.checkForgotPassword("another@example.com", "192.0.2.3"));
    }

    @Test
    void summarization_allowsTenPerUserAndRejectsTheEleventh() {
        for (int attempt = 0; attempt < 10; attempt++) {
            assertDoesNotThrow(() -> rateLimitService.checkSummarization("user-id"));
        }

        assertThrows(RateLimitExceededException.class, () -> rateLimitService.checkSummarization("user-id"));
        assertDoesNotThrow(() -> rateLimitService.checkSummarization("other-user-id"));
    }
}
