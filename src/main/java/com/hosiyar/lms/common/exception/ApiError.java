package com.hosiyar.lms.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Standard error envelope returned for every failed API response.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {}
