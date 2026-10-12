package com.hosiyar.lms.course.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The course module's public face, mirroring the user module's UserDirectory.
 * Other modules depend on this interface only - never the Course entity, the
 * repository, or CourseService. See ADR-001 and ADR-006.
 *
 * This interface starts small and grows deliberately, one method at a time, as
 * other modules turn out to need something. The enrolment module is the first
 * caller: it needs to know whether a course exists and is published before
 * letting a student in.
 */
public interface CourseDirectory {

    Optional<CourseSummary> findById(UUID courseId);

    /**
     * Batch form, so a caller rendering a page of enrolments can resolve every
     * course in one query rather than one per row - the same N+1 guard the
     * user module's UserDirectory provides.
     */
    Map<UUID, CourseSummary> findAllByIds(Set<UUID> courseIds);
}
