package com.hosiyar.lms.course.dto;

import com.hosiyar.lms.course.entity.CourseStatus;

import java.time.Instant;
import java.util.UUID;

/** The shape returned when a single course is created or viewed. */
public record CourseResponse(
        UUID id,
        String title,
        String description,
        UUID instructorId,
        String instructorName,
        CourseStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
