package com.hosiyar.lms.user.service;

import com.hosiyar.lms.common.exception.BadRequestException;
import com.hosiyar.lms.user.dto.AuthResponse;
import com.hosiyar.lms.user.dto.LoginRequest;
import com.hosiyar.lms.user.dto.RefreshRequest;
import com.hosiyar.lms.user.entity.User;
import com.hosiyar.lms.user.repository.UserRepository;
import com.hosiyar.lms.user.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                // Deliberately the same message for "no such email" and "wrong
                // password" - don't reveal which part was wrong to an attacker.
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        return issueTokens(user);
    }

    /**
     * Trades a valid refresh token for a fresh pair of tokens, without
     * requiring the password again.
     */
    public AuthResponse refresh(RefreshRequest request) {
        String email;
        String tokenType;

        try {
            email = jwtService.extractEmail(request.refreshToken());
            tokenType = jwtService.extractTokenType(request.refreshToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        // An access token passed in here is rejected - it's valid, correctly
        // signed, and still the wrong tool for this job.
        if (!JwtService.TYPE_REFRESH.equals(tokenType)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired refresh token"));

        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtService.getAccessTokenExpirySeconds()
        );
    }
}
