package com.hosiyar.lms.course.api;

import java.util.UUID;

/**
 * What other modules are allowed to know about a course.
 *
 * Deliberately narrow, like UserSummary in the user module: enough to enrol a
 * student and show them what they enrolled in, and nothing more. No entity
 * ever leaves the course module. See ADR-006.
 */
public record CourseSummary(
        UUID id,
        String title,
        UUID instructorId,
        boolean published,
        int lessonCount
) {}
