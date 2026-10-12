package com.hosiyar.lms.common.dto;

import java.time.Instant;

/**
 * Standard response envelope for every successful API response.
 * Keeps the shape consistent across all modules so viewers only learn it once.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now());
    }
}
