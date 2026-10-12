package com.hosiyar.lms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Note there is no instructorId field. Ownership is taken from the
 * authenticated caller's token - never from the request body, or anyone
 * could create a course in someone else's name.
 */
public record CreateCourseRequest(

        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        @Size(max = 5000, message = "description must be at most 5000 characters")
        String description
) {}
