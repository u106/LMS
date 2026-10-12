package com.hosiyar.lms.course.service;

import com.hosiyar.lms.common.exception.AccessDeniedByOwnershipException;
import com.hosiyar.lms.common.exception.ResourceNotFoundException;
import com.hosiyar.lms.common.storage.FileStorage;
import com.hosiyar.lms.course.dto.CourseResponse;
import com.hosiyar.lms.course.dto.CourseSummaryResponse;
import com.hosiyar.lms.course.dto.CreateCourseRequest;
import com.hosiyar.lms.course.dto.UpdateCourseRequest;
import com.hosiyar.lms.course.entity.Course;
import com.hosiyar.lms.course.entity.CourseStatus;
import com.hosiyar.lms.course.entity.Lesson;
import com.hosiyar.lms.course.repository.CourseRepository;
import com.hosiyar.lms.course.repository.LessonRepository;
import com.hosiyar.lms.user.api.UserDirectory;
import com.hosiyar.lms.user.api.UserSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Note the dependency: UserDirectory, the user module's public interface -
 * not UserRepository, not UserService. That's the module boundary being
 * respected in practice rather than just in the docs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    // FIXED: Added 'final' keywords so @RequiredArgsConstructor can inject them
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserDirectory userDirectory;
    private final FileStorage fileStorage;

    @Transactional
    public CourseResponse create(CreateCourseRequest request, UUID instructorId) {
        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setInstructorId(instructorId);
        course.setStatus(CourseStatus.DRAFT);
        
        Course saved = courseRepository.save(course);
        return toResponse(saved, resolveName(instructorId));
    }

    /**
     * The public catalogue - PUBLISHED only.
     * 
     * The status filter is the whole ballgame here: drop it and every
     * instructor's unfinished drafts become world-readable.
     */
    @Transactional(readOnly = true)
    public Page<CourseSummaryResponse> findPublished(Pageable pageable) {
        return toSummaryPage(courseRepository.findAllByStatus(CourseStatus.PUBLISHED, pageable));
    }

    /**
     * An instructor's own courses, drafts included. Scoped by the id from
     * the token, never by anything the client supplies.
     */
    @Transactional(readOnly = true)
    public Page<CourseSummaryResponse> findMine(UUID instructorId, Pageable pageable) {
        return toSummaryPage(courseRepository.findAllByInstructorId(instructorId, pageable));
    }

    /**
     * A published course is visible to anyone. A draft is visible only to
     * its owner - and everyone else gets "not found", not "forbidden",
     * because a 403 would confirm that the draft exists at all.
     * 
     * callerId is null for anonymous visitors.
     */
    @Transactional(readOnly = true)
    public CourseResponse findById(UUID courseId, UUID callerId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        
        boolean isPublished = course.getStatus() == CourseStatus.PUBLISHED;
        boolean isOwner = callerId != null && course.isOwnedBy(callerId);
        
        if (!isPublished && !isOwner) {
            throw new ResourceNotFoundException("Course not found");
        }
        
        return toResponse(course, resolveName(course.getInstructorId()));
    }

    /**
     * The check this whole chapter is about.
     * 
     * Loads the course, then refuses if it isn't the caller's - BEFORE
     * anything is read out of it or written to it. Being an INSTRUCTOR got
     * you this far; being THIS course's instructor is a separate question,
     * and one no URL rule can answer, because it depends on the row.
     * 
     * Skip this and you have an IDOR: any instructor edits any course by
     * changing the id in the URL.
     * 
     * Package-private on purpose - LessonService uses it, nothing outside
     * the module can.
     */
    Course requireOwnedCourse(UUID courseId, UUID callerId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        
        if (!course.isOwnedBy(callerId)) {
            throw new AccessDeniedByOwnershipException("You do not own this course");
        }
        
        return course;
    }

    @Transactional
    public CourseResponse update(UUID courseId, UpdateCourseRequest request, UUID callerId) {
        Course course = requireOwnedCourse(courseId, callerId);
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setStatus(request.status());
        
        // No explicit save() needed - the entity is managed inside this
        // transaction, so JPA flushes the changes on commit.
        return toResponse(course, resolveName(course.getInstructorId()));
    }

    @Transactional
    public void delete(UUID courseId, UUID callerId) {
        Course course = requireOwnedCourse(courseId, callerId);
        
        // ON DELETE CASCADE removes the lesson rows, but the database has
        // never heard of S3. Their files have to be cleaned up here, before
        // the rows that point at them disappear - otherwise the keys are
        // gone and the objects are unreachable forever. See ADR-007.
        lessonRepository.findAllByCourseIdOrderByPositionAsc(courseId).stream()
                .map(Lesson::getFileKey)
                .filter(Objects::nonNull)
                .forEach(key -> {
                    try {
                        fileStorage.delete(key);
                    } catch (RuntimeException e) {
                        log.warn("Orphaned object left in storage, key={}", key, e.getMessage(),e);
                    }
                });
                
        courseRepository.delete(course);
    }

    /**
     * Resolves every instructor on the page in ONE call.
     * 
     * The naive version - calling userDirectory.findById() inside the map -
     * looks harmless and issues one query per row. On a 20-row page that's
     * 20 extra round trips instead of 1. Collecting the distinct ids first
     * and asking once is the fix.
     */
    private Page<CourseSummaryResponse> toSummaryPage(Page<Course> courses) {
        List<Course> content = courses.getContent();
        Set<UUID> instructorIds = content.stream()
                .map(Course::getInstructorId)
                .collect(Collectors.toSet());
                
        Map<UUID, UserSummary> instructors = userDirectory.findAllByIds(instructorIds);
        
        return courses.map(course -> new CourseSummaryResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getInstructorId(),
                nameOf(instructors.get(course.getInstructorId())),
                course.getStatus(),
                course.getCreatedAt()
        ));
    }

    private String resolveName(UUID instructorId) {
        return nameOf(userDirectory.findById(instructorId).orElse(null));
    }

    /**
     * A missing instructor is possible precisely because there's no database
     * foreign key across the module boundary (ADR-006). Degrade gracefully
     * rather than throwing - a deleted account shouldn't 500 the catalogue.
     */
    private String nameOf(UserSummary summary) {
        return summary != null ? summary.name() : "Unknown instructor";
    }

    private CourseResponse toResponse(Course course, String instructorName) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getInstructorId(),
                instructorName,
                course.getStatus(),
                course.getCreatedAt(),
                course.getUpdatedAt()
        );
    }
}
