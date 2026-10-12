package com.hosiyar.lms.enrollment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Only the course. The student is taken from the authenticated token, never
 * from the body - the same rule as every ownership-sensitive endpoint in the
 * project.
 */
public record EnrollRequest(
        @NotNull(message = "courseId is required")
        UUID courseId
) {}
