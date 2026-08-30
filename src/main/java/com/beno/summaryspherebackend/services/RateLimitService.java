package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.exceptions.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    public void checkLogin(String ip) {
        check("login:ip:" + normalize(ip), 10, Duration.ofMinutes(15), "Too many login attempts. Try again later.");
    }

    public void checkRegistration(String ip) {
        check("register:ip:" + normalize(ip), 3, Duration.ofHours(1), "Too many registrations from this IP. Try again later.");
    }

    public void checkForgotPassword(String email, String ip) {
        check("forgot:email:" + normalize(email), 3, Duration.ofHours(1), "Too many password reset requests. Try again later.");
        check("forgot:ip:" + normalize(ip), 10, Duration.ofHours(1), "Too many password reset requests from this IP. Try again later.");
    }

    public void checkSummarization(String userId) {
        check("summarize:user:" + normalize(userId) + ":" + LocalDate.now(), 10, Duration.ofDays(1),
                "Daily summarization limit reached. Try again tomorrow.");
    }

    private void check(String key, int limit, Duration window, String message) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        Deque<Instant> userAttempts = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (userAttempts) {
            while (!userAttempts.isEmpty() && userAttempts.peekFirst().isBefore(cutoff)) {
                userAttempts.removeFirst();
            }
            if (userAttempts.size() >= limit) {
                throw new RateLimitExceededException(message);
            }
            userAttempts.addLast(now);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
    }
}
