package com.hosiyar.lms.enrollment.service;

import com.hosiyar.lms.common.exception.BadRequestException;
import com.hosiyar.lms.common.exception.ResourceNotFoundException;
import com.hosiyar.lms.course.api.CourseDirectory;
import com.hosiyar.lms.course.api.CourseSummary;
import com.hosiyar.lms.enrollment.dto.EnrollmentResponse;
import com.hosiyar.lms.enrollment.entity.Enrollment;
import com.hosiyar.lms.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Note the dependency: CourseDirectory, the course module's public interface -
 * not CourseRepository, not CourseService, not the Course entity. The whole
 * module boundary rests on that being the only way in. See ADR-006.
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final  EnrollmentRepository enrollmentRepository;
    private final  CourseDirectory courseDirectory;

    @Transactional
    public EnrollmentResponse enroll(UUID courseId, UUID studentId) {
        // Ask the course module whether this is a real, published course.
        // We cannot see the Course entity, only what CourseDirectory exposes.
        CourseSummary course = courseDirectory.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (!course.published()) {
            // A draft is invisible to a student, so treat it as not found
            // rather than confirming it exists - same instinct as Module 2.
            throw new ResourceNotFoundException("Course not found");
        }

        // First line of defence: a cheap application-level check that catches
        // the common case without hitting a constraint violation.
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new BadRequestException("You are already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setEnrolledAt(Instant.now());

        try {
            Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);
            return toResponse(saved, course.title());
        } catch (DataIntegrityViolationException e) {
            // Second line of defence, and the one that actually matters under
            // load: two concurrent requests can both pass the exists() check
            // above, but the unique constraint lets only one row in. We catch
            // the loser's violation and turn it into the same clean error.
            // Exactly the race-condition lesson from the first registration
            // video, one module later.
            throw new BadRequestException("You are already enrolled in this course");
        }
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> findMine(UUID studentId, Pageable pageable) {
        Page<Enrollment> page = enrollmentRepository.findAllByStudentId(studentId, pageable);

        // Resolve every course on the page in ONE batch call, not one per row -
        // the same N+1 guard used for instructors in Module 2.
        Set<UUID> courseIds = page.getContent().stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toSet());
        Map<UUID, CourseSummary> courses = courseDirectory.findAllByIds(courseIds);

        return page.map(enrollment -> toResponse(
                enrollment,
                titleOf(courses.get(enrollment.getCourseId()))
        ));
    }

    @Transactional
    public void unenroll(UUID courseId, UUID studentId) {
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not enrolled in this course"));

        // Deliberately removes ONLY the enrolment row. Progress is keyed by
        // (student, lesson) and lives independently, so it survives - if the
        // student re-enrols later, their position and completed lessons are
        // still there. See ADR-008 / US-3. (Progress itself arrives in
        // chapter 4; this is the framing that makes that work.)
        enrollmentRepository.delete(enrollment);
    }

    private String titleOf(CourseSummary summary) {
        return summary != null ? summary.title() : "Unknown course";
    }

    private EnrollmentResponse toResponse(Enrollment enrollment, String courseTitle) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourseId(),
                courseTitle,
                enrollment.getStudentId(),
                enrollment.getEnrolledAt()
        );
    }
}
