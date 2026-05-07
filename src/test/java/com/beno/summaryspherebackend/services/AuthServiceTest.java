package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.dtos.AuthSchema;
import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.Role;
import com.beno.summaryspherebackend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    EmailService emailService;

    @InjectMocks
    AuthService authService;

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        String email = "test@test.com";
        AuthSchema.RegisterRequest request = new AuthSchema.RegisterRequest("Test User", email, "password123");

        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldReturnAuthResponse_WhenRegistrationIsSuccessful() {
        String email = "test@test.com";
        AuthSchema.RegisterRequest request = new AuthSchema.RegisterRequest("Test User", email, "password123");

        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any())).thenReturn("mockToken");

        AuthSchema.AuthResponse response = authService.register(request);

        verify(userRepository).save(any());

        assertNotNull(response);
        assertEquals("mockToken", response.token());
        assertEquals(email, response.email());
        assertEquals("Test User", response.fullName());
        assertEquals("USER", response.role());
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {
        String email = "missing@test.com";
        AuthSchema.LoginRequest request = new AuthSchema.LoginRequest(email, "password123");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenLoginIsSuccessful() {
        String email = "test@test.com";
        AuthSchema.LoginRequest request = new AuthSchema.LoginRequest(email, "password123");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(User.builder()
                .email(email)
                .fullName("Test User")
                .role(Role.USER)
                .password("pw")
                .build()));
        when(jwtService.generateToken(any())).thenReturn("mockToken");

        AuthSchema.AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mockToken", response.token());
        assertEquals(email, response.email());
        assertEquals("Test User", response.fullName());
        assertEquals("USER", response.role());
    }

    @Test
    void forgotPassword_whenUserMissing_succeeds() {
        // Security: silently succeed to prevent email enumeration
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        authService.forgotPassword("missing@test.com");

        verify(emailService, never()).sendResetPasswordEmail(anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void forgotPassword_setsResetTokenAndSendsEmail() {
        User user = User.builder()
                .email("test@test.com")
                .password("pw")
                .fullName("Test")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.forgotPassword("test@test.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertNotNull(saved.getResetToken());
        assertNotNull(saved.getResetTokenExpiry());
        assertTrue(saved.getResetTokenExpiry().isAfter(LocalDateTime.now()));

        verify(emailService).sendResetPasswordEmail(eq("test@test.com"), eq(saved.getResetToken()));
    }

    @Test
    void resetPassword_whenTokenInvalid_throws() {
        when(userRepository.findByResetToken("bad")).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.resetPassword("bad", "new"));
        assertEquals("Invalid token", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_whenTokenExpired_throwsAndDoesNotChangePassword() {
        User user = User.builder()
                .email("test@test.com")
                .password("old")
                .fullName("Test")
                .role(Role.USER)
                .build();
        user.setResetToken("t");
        user.setResetTokenExpiry(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByResetToken("t")).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.resetPassword("t", "new"));
        assertEquals("Token expired", exception.getMessage());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_success_encodesPasswordAndClearsTokenFields() {
        User user = User.builder()
                .email("test@test.com")
                .password("old")
                .fullName("Test")
                .role(Role.USER)
                .build();
        user.setResetToken("t");
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByResetToken("t")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encoded");

        authService.resetPassword("t", "newPassword");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("encoded", saved.getPassword());
        assertNull(saved.getResetToken());
        assertNull(saved.getResetTokenExpiry());
    }
}