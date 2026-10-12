package com.hosiyar.lms.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LessonRequest(

        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title must be at most 255 characters")
        String title,

        String content,

        @NotNull(message = "position is required")
        @Min(value = 1, message = "position must be 1 or greater")
        Integer position
) {}
