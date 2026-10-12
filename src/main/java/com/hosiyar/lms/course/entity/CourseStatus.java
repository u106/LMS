package com.hosiyar.lms.course.entity;

/**
 * A course is DRAFT until its instructor deliberately publishes it.
 * Only PUBLISHED courses appear in the public catalogue.
 */
public enum CourseStatus {
    DRAFT,
    PUBLISHED
}
