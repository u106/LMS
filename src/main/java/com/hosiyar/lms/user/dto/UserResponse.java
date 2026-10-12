package com.hosiyar.lms.user.dto;

import com.hosiyar.lms.user.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        Instant createdAt
) {}
