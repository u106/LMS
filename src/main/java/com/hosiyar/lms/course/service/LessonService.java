package com.hosiyar.lms.course.service;

import com.hosiyar.lms.common.exception.BadRequestException;
import com.hosiyar.lms.common.exception.ResourceNotFoundException;
import com.hosiyar.lms.common.storage.FileStorage;
import com.hosiyar.lms.course.dto.FileDownloadResponse;
import com.hosiyar.lms.course.dto.LessonRequest;
import com.hosiyar.lms.course.dto.LessonResponse;
import com.hosiyar.lms.course.entity.Course;
import com.hosiyar.lms.course.entity.CourseStatus;
import com.hosiyar.lms.course.entity.Lesson;
import com.hosiyar.lms.course.repository.CourseRepository;
import com.hosiyar.lms.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Every write here answers the same question as CourseService's writes, just
 * one level up the tree: you may touch a lesson only if you own the course
 * it belongs to. A lesson has no separate owner of its own.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final FileStorage fileStorage;
    private final LessonFileValidator fileValidator;

    @Value("${lms.s3.presigned-url-expiry-minutes}")
    private long presignedExpiryMinutes;

    @Transactional
    public LessonResponse create(UUID courseId, LessonRequest request, UUID callerId) {
        Course course = courseService.requireOwnedCourse(courseId, callerId);

        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle(request.title());
        lesson.setContent(request.content());
        lesson.setPosition(request.position());

        return toResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse update(UUID courseId, UUID lessonId, LessonRequest request, UUID callerId) {
        courseService.requireOwnedCourse(courseId, callerId);

        Lesson lesson = requireLessonInCourse(lessonId, courseId);
        lesson.setTitle(request.title());
        lesson.setContent(request.content());
        lesson.setPosition(request.position());

        return toResponse(lesson);
    }

    @Transactional
    public void delete(UUID courseId, UUID lessonId, UUID callerId) {
        courseService.requireOwnedCourse(courseId, callerId);
        Lesson lesson = requireLessonInCourse(lessonId, courseId);

        // The database will drop the row, but it has never heard of S3.
        // Without this, the object stays in the bucket forever, costing money
        // and referenced by nothing - see ADR-007.
        deleteStoredFileQuietly(lesson.getFileKey());
        lessonRepository.delete(lesson);
    }

    /**
     * Attaches a file to a lesson.
     *
     * Ownership first, then validation, then upload. Uploading before either
     * check would mean paying to store files belonging to requests we were
     * always going to reject.
     */
    @Transactional
    public LessonResponse attachFile(UUID courseId, UUID lessonId, MultipartFile file, UUID callerId) {
        courseService.requireOwnedCourse(courseId, callerId);
        Lesson lesson = requireLessonInCourse(lessonId, courseId);
        fileValidator.validate(file);

        String previousKey = lesson.getFileKey();
        String key = fileValidator.buildKey(courseId, lessonId, file.getContentType());

        try (var stream = file.getInputStream()) {
            fileStorage.store(key, stream, file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        lesson.setFileKey(key);
        lesson.setFileName(file.getOriginalFilename());
        lesson.setContentType(file.getContentType());
        lesson.setFileSize(file.getSize());

        // Replacing a file leaves the old object orphaned unless we say so.
        if (previousKey != null) {
            deleteStoredFileQuietly(previousKey);
        }

        return toResponse(lesson);
    }

    /**
     * Hands back a short-lived URL pointing straight at object storage.
     *
     * The bytes never travel through this application - no memory pressure,
     * no bandwidth cost, and the bucket stays completely private.
     */
    @Transactional(readOnly = true)
    public FileDownloadResponse fileUrl(UUID courseId, UUID lessonId, UUID callerId) {
        requireVisibleCourse(courseId, callerId);
        Lesson lesson = requireLessonInCourse(lessonId, courseId);

        if (!lesson.hasFile()) {
            throw new ResourceNotFoundException("This lesson has no file attached");
        }

        Duration expiry = Duration.ofMinutes(presignedExpiryMinutes);
        return new FileDownloadResponse(
                fileStorage.presignedDownloadUrl(lesson.getFileKey(), expiry),
                lesson.getFileName(),
                lesson.getContentType(),
                lesson.getFileSize(),
                Instant.now().plus(expiry)
        );
    }

    @Transactional
    public LessonResponse removeFile(UUID courseId, UUID lessonId, UUID callerId) {
        courseService.requireOwnedCourse(courseId, callerId);
        Lesson lesson = requireLessonInCourse(lessonId, courseId);

        if (!lesson.hasFile()) {
            throw new BadRequestException("This lesson has no file to remove");
        }

        deleteStoredFileQuietly(lesson.getFileKey());
        lesson.setFileKey(null);
        lesson.setFileName(null);
        lesson.setContentType(null);
        lesson.setFileSize(null);

        return toResponse(lesson);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> findByCourse(UUID courseId, UUID callerId) {
        requireVisibleCourse(courseId, callerId);
        return lessonRepository.findAllByCourseIdOrderByPositionAsc(courseId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * A published course is visible to anyone; a draft only to its owner,
     * and everyone else gets "not found" rather than "forbidden" so its
     * existence isn't revealed.
     */
    private Course requireVisibleCourse(UUID courseId, UUID callerId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        boolean isPublished = course.getStatus() == CourseStatus.PUBLISHED;
        boolean isOwner = callerId != null && course.isOwnedBy(callerId);

        if (!isPublished && !isOwner) {
            throw new ResourceNotFoundException("Course not found");
        }
        return course;
    }

    /**
     * Looked up by lesson id AND course id together, so a lesson belonging to
     * one course cannot be operated on through another course's URL.
     */
    private Lesson requireLessonInCourse(UUID lessonId, UUID courseId) {
        return lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
    }

    /**
     * A failed cleanup shouldn't roll back the user's actual operation - the
     * lesson really was deleted. Log it loudly instead; an orphaned object is
     * a billing annoyance, not a correctness bug.
     */
    private void deleteStoredFileQuietly(String key) {
        if (key == null) return;
        try {
            fileStorage.delete(key);
        } catch (RuntimeException e) {
            log.warn("Orphaned object left in storage, key={}", key, e);
        }
    }

    private LessonResponse toResponse(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getCourse().getId(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getPosition(),
                lesson.getFileName(),
                lesson.getContentType(),
                lesson.getFileSize(),
                lesson.hasFile(),
                lesson.getCreatedAt(),
                lesson.getUpdatedAt()
        );
    }
}
