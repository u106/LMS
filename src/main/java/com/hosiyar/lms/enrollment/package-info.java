/**
 * Student enrollment and progress tracking.
 *
 * Enrollment implemented (chapter 2): enroll, my-enrollments, unenroll.
 * The StudentEnrolled event (chapter 3) and progress tracking (chapters 4-5)
 * build on this.
 *
 * This module references students and courses by id only, through the user
 * and course modules' public interfaces (UserDirectory, CourseDirectory) -
 * never their entities or repositories. See ADR-006. It holds no foreign key
 * to anything, making it the purest expression of the boundary rule.
 */
package com.hosiyar.lms.enrollment;
