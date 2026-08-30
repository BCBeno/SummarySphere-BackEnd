package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestClient restClient = RestClient.create();

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from-email:onboarding@resend.dev}")
    private String resendFromEmail;

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<Void> sendResetPasswordEmail(String email, String token) {
        try {
            log.info("Starting async password reset email to: {}", email);
            if (resendApiKey == null || resendApiKey.isBlank()) {
                throw new IllegalStateException(
                        "Resend API key is missing. Set RESEND_API_KEY in environment variables.");
            }

            String resetLink = frontendBaseUrl + "/reset-password?token=" + token;

            String htmlContent = buildResetPasswordEmailHTML(resetLink);

            Map<String, Object> requestBody = Map.of(
                    "from", resendFromEmail,
                    "to", List.of(email),
                    "subject", "SummarySphere - Password Reset Request",
                    "html", htmlContent);

            restClient.post()
                    .uri(RESEND_API_URL)
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Password reset email sent successfully to: {}", email);
            return CompletableFuture.completedFuture(null);
        } catch (RestClientResponseException e) {
            log.error("Resend API failed for {} with status {} and body: {}", email, e.getStatusCode(),
                    e.getResponseBodyAsString());
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<Void> sendEmailVerificationEmail(String email, String token) {
        try {
            if (resendApiKey == null || resendApiKey.isBlank()) {
                throw new IllegalStateException(
                        "Resend API key is missing. Set RESEND_API_KEY in environment variables.");
            }

            String verificationLink = frontendBaseUrl + "/verify-email?token=" + token;
            Map<String, Object> requestBody = Map.of(
                    "from", resendFromEmail,
                    "to", List.of(email),
                    "subject", "SummarySphere - Verify your email",
                    "html", buildEmailVerificationHTML(verificationLink));

            restClient.post()
                    .uri(RESEND_API_URL)
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Email verification message sent successfully to: {}", email);
            return CompletableFuture.completedFuture(null);
        } catch (RestClientResponseException e) {
            log.error("Resend API failed for {} with status {} and body: {}", email, e.getStatusCode(),
                    e.getResponseBodyAsString());
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            log.error("Failed to send email verification message to: {}", email, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String buildEmailVerificationHTML(String verificationLink) {
        String safeLink = escapeHtml(verificationLink);
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Verify your email - SummarySphere</title>
                </head>
                <body style="margin:0;background:#0f172a;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;color:#e2e8f0">
                  <div style="max-width:600px;margin:0 auto;padding:36px 20px">
                    <div style="background:#1e293b;border:1px solid #334155;border-radius:18px;overflow:hidden">
                      <div style="padding:30px;background:linear-gradient(135deg,#2563eb,#4f46e5);text-align:center">
                        <h1 style="margin:0;color:#fff;font-size:28px">SummarySphere</h1>
                      </div>
                      <div style="padding:36px">
                        <h2 style="margin:0 0 14px;color:#fff;font-size:24px">Verify your email</h2>
                        <p style="margin:0 0 24px;line-height:1.6;color:#cbd5e1">Confirm your email address to upload documents and use all SummarySphere features.</p>
                        <a href="%s" style="display:inline-block;background:#2563eb;color:#fff;text-decoration:none;font-weight:700;padding:13px 24px;border-radius:9px">Verify email</a>
                        <p style="margin:26px 0 8px;color:#94a3b8;font-size:13px">Or copy this link into your browser:</p>
                        <p style="margin:0;padding:12px;background:#0f172a;border-radius:8px;word-break:break-all;color:#93c5fd;font-size:12px">%s</p>
                        <p style="margin:22px 0 0;color:#fbbf24;font-size:13px">This link expires in 1 hour and can only be used once.</p>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(safeLink, safeLink);
    }

    private String buildResetPasswordEmailHTML(String resetLink) {
        String safeLink = escapeHtml(resetLink);

        StringBuilder sb = new StringBuilder(2048);
        sb.append("<!DOCTYPE html>")
                .append("<html lang=\"en\">")
                .append("<head>")
                .append("<meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                .append("<title>Password Reset - SummarySphere</title>")
                .append("<style>")
                .append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', sans-serif; }")
                .append(".container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9fafb; }")
                .append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }")
                .append(".header h1 { margin: 0; font-size: 28px; }")
                .append(".content { background: white; padding: 40px; border-radius: 0 0 8px 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }")
                .append(".content p { color: #374151; line-height: 1.6; }")
                .append(".reset-button { display: inline-block; background-color: #667eea; color: white; padding: 12px 32px; text-decoration: none; border-radius: 6px; font-weight: bold; margin: 20px 0; }")
                .append(".reset-button:hover { background-color: #5a67d8; }")
                .append(".footer { background-color: #f3f4f6; padding: 20px; text-align: center; color: #6b7280; font-size: 12px; border-radius: 0 0 8px 8px; }")
                .append(".warning { background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 4px; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class=\"container\">")
                .append("<div class=\"header\">")
                .append("<h1>🔐 SummarySphere</h1>")
                .append("<p>Password Reset Request</p>")
                .append("</div>")
                .append("<div class=\"content\">")
                .append("<p>Hi,</p>")
                .append("<p>We received a request to reset your SummarySphere password. Click the button below to proceed:</p>")
                .append("<a href=\"")
                .append(safeLink)
                .append("\" class=\"reset-button\">Reset Your Password</a>")
                .append("<p>Or copy and paste this link in your browser:</p>")
                .append("<p style=\"word-break: break-all; background-color: #f3f4f6; padding: 10px; border-radius: 4px; font-size: 12px;\">")
                .append(safeLink)
                .append("</p>")
                .append("<div class=\"warning\">⏰ <strong>This link will expire in 1 hour</strong></div>")
                .append("<p>If you didn't request a password reset, you can safely ignore this email or contact support if you have concerns.</p>")
                .append("<p>Best regards,<br><strong>The SummarySphere Team</strong></p>")
                .append("</div>")
                .append("<div class=\"footer\">")
                .append("<p>© 2026 SummarySphere. All rights reserved.</p>")
                .append("<p>This is an automated message, please do not reply directly to this email.</p>")
                .append("</div>")
                .append("</div>")
                .append("</body>")
                .append("</html>");

        return sb.toString();
    }

    private String escapeHtml(String input) {
        if (input == null)
            return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
