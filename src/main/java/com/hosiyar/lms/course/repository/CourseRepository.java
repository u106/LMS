package com.hosiyar.lms.course.repository;

import com.hosiyar.lms.course.entity.Course;
import com.hosiyar.lms.course.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    /**
     * The public catalogue. Filtering by status here - rather than fetching
     * everything and filtering in Java - is what keeps drafts from leaking.
     */
    Page<Course> findAllByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findAllByInstructorId(UUID instructorId, Pageable pageable);
}
