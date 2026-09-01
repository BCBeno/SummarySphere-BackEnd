package com.beno.summaryspherebackend.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsValidRegistration() {
        var request = new AuthSchema.RegisterRequest("Test User", "test@example.com", "Password1!");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidRegistrationFields() {
        var request = new AuthSchema.RegisterRequest(" ", "invalid-email", "password");

        Set<ConstraintViolation<AuthSchema.RegisterRequest>> violations = validator.validate(request);

        assertTrue(hasViolationFor(violations, "fullName"));
        assertTrue(hasViolationFor(violations, "email"));
        assertTrue(hasViolationFor(violations, "password"));
    }

    @Test
    void loginDoesNotRequireRegistrationPasswordComplexity() {
        var request = new AuthSchema.LoginRequest("test@example.com", "legacy-password");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void resetPasswordUsesSameStrengthRulesAsRegistration() {
        var request = new AuthSchema.ResetPasswordRequest("valid-token", "weakpassword");

        assertTrue(hasViolationFor(validator.validate(request), "newPassword"));
    }

    @Test
    void rejectsBlankAndOversizedChatMessages() {
        assertTrue(hasViolationFor(validator.validate(new ChatSchema.ChatRequest(" ")), "message"));
        assertTrue(hasViolationFor(validator.validate(new AgentSchema.ChatRequest("x".repeat(4001))), "message"));
    }

    @Test
    void summaryTypeMustNotBeBlank() {
        assertTrue(validator.validate(new SummarizationSchema.SummarizeRequest("detailed")).isEmpty());
        assertEquals(1, validator.validate(new SummarizationSchema.SummarizeRequest(" ")).size());
    }

    private static boolean hasViolationFor(Set<? extends ConstraintViolation<?>> violations, String field) {
        return violations.stream().anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
    }
}
