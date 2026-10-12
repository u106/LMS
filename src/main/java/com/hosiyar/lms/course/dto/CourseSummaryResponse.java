package com.hosiyar.lms.course.dto;

import com.hosiyar.lms.course.entity.CourseStatus;

import java.time.Instant;
import java.util.UUID;

/** The lightweight shape used in catalogue listings. */
public record CourseSummaryResponse(
        UUID id,
        String title,
        String description,
        UUID instructorId,
        String instructorName,
        CourseStatus status,
        Instant createdAt
) {}
