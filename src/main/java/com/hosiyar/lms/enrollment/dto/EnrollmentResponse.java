package com.hosiyar.lms.enrollment.dto;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentResponse(
        UUID id,
        UUID courseId,
        String courseTitle,
        UUID studentId,
        Instant enrolledAt
) {}
