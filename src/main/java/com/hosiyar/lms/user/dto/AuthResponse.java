package com.hosiyar.lms.user.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
