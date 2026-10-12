package com.hosiyar.lms.user.service;

import com.hosiyar.lms.common.exception.BadRequestException;
import com.hosiyar.lms.user.dto.AuthResponse;
import com.hosiyar.lms.user.dto.LoginRequest;
import com.hosiyar.lms.user.dto.RefreshRequest;
import com.hosiyar.lms.user.entity.Role;
import com.hosiyar.lms.user.entity.User;
import com.hosiyar.lms.user.repository.UserRepository;
import com.hosiyar.lms.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Maps back to the acceptance criteria in
 * docs/prd/module-01-users-auth.md (US-2 and US-3).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.STUDENT);
    }

    @Test
    @DisplayName("correct credentials return an access and refresh token")
    void loginSucceedsWithCorrectCredentials() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

        AuthResponse response = authService.login(new LoginRequest("test@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresIn()).isEqualTo(900L);
    }

    @Test
    @DisplayName("a wrong password is rejected and issues no token")
    void loginFailsWithWrongPassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "wrong-password")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid email or password");

        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("an unknown email gives the same message as a wrong password")
    void loginFailsWithUnknownEmailUsingIdenticalMessage() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOf(BadRequestException.class)
                // Identical to the wrong-password message on purpose: an attacker
                // shouldn't be able to discover which emails are registered.
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("a valid refresh token issues a fresh token pair")
    void refreshSucceedsWithRefreshToken() {
        when(jwtService.extractEmail("refresh-token")).thenReturn("test@example.com");
        when(jwtService.extractTokenType("refresh-token")).thenReturn(JwtService.TYPE_REFRESH);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(900L);

        AuthResponse response = authService.refresh(new RefreshRequest("refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
    }

    @Test
    @DisplayName("an access token passed to refresh is rejected")
    void refreshRejectsAnAccessToken() {
        when(jwtService.extractEmail("access-token")).thenReturn("test@example.com");
        when(jwtService.extractTokenType("access-token")).thenReturn(JwtService.TYPE_ACCESS);

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("access-token")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(userRepository, never()).findByEmail(anyString());
    }
}
