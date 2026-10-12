package com.hosiyar.lms.user.entity;

/**
 * A user's role within the platform. Enforced properly once Spring
 * Security is wired in (chapter 5) - for now, just a data field.
 */
public enum Role {
    STUDENT,
    INSTRUCTOR,
    ADMIN
}
