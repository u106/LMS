package com.hosiyar.lms.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(

        @NotBlank(message = "refreshToken is required")
        String refreshToken
) {}
