package com.hosiyar.lms.course.dto;

import com.hosiyar.lms.course.entity.CourseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Publishing is just an update that sets status to PUBLISHED - no separate
 * endpoint needed. Note there's still no instructorId here: ownership can't
 * be transferred by editing a course.
 */
public record UpdateCourseRequest(

        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        @Size(max = 5000, message = "description must be at most 5000 characters")
        String description,

        @NotNull(message = "status is required")
        CourseStatus status
) {}
