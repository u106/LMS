package com.hosiyar.lms.course.service;

import com.hosiyar.lms.common.exception.AccessDeniedByOwnershipException;
import com.hosiyar.lms.common.exception.ResourceNotFoundException;
import com.hosiyar.lms.common.storage.FileStorage;
import com.hosiyar.lms.course.entity.Course;
import com.hosiyar.lms.course.entity.CourseStatus;
import com.hosiyar.lms.course.entity.Lesson;
import com.hosiyar.lms.course.repository.CourseRepository;
import com.hosiyar.lms.course.repository.LessonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lesson-level authorization walks up to the parent course. Maps to
 * docs/prd/module-02-course-management.md US-6 and US-7.
 */
@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

    @Mock private LessonRepository lessonRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseService courseService;
    @Mock private FileStorage fileStorage;

    private LessonService lessonService;

    private UUID ownerId;
    private UUID otherId;
    private Course course;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        otherId = UUID.randomUUID();

        course = new Course();
        course.setInstructorId(ownerId);
        course.setStatus(CourseStatus.PUBLISHED);

        lessonService = new LessonService(
                lessonRepository, courseRepository, courseService, fileStorage,
                new LessonFileValidator());
        // The presigned-expiry value is normally injected from config.
        ReflectionTestUtils.setField(lessonService, "presignedExpiryMinutes", 15L);
    }

    @Test
    @DisplayName("uploading to a course you don't own never reaches storage")
    void nonOwnerUploadNeverHitsStorage() {
        UUID courseId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        // The ownership guard (delegated to CourseService) throws.
        when(courseService.requireOwnedCourse(courseId, otherId))
                .thenThrow(new AccessDeniedByOwnershipException("You do not own this course"));

        MultipartFile file = new MockMultipartFile(
                "file", "x.pdf", "application/pdf", new byte[1024]);

        assertThatThrownBy(() -> lessonService.attachFile(courseId, lessonId, file, otherId))
                .isInstanceOf(AccessDeniedByOwnershipException.class);

        // The whole point: we reject BEFORE uploading, so nothing is stored
        // and no S3 cost is incurred for a request we always meant to refuse.
        verify(fileStorage, never()).store(anyString(), any(), anyLong(), anyString());
    }

    @Test
    @DisplayName("a lesson id from another course is not found under this course's path")
    void crossCourseLessonNotFound() {
        UUID courseId = UUID.randomUUID();
        UUID foreignLessonId = UUID.randomUUID();

        // Caller genuinely owns the course in the path...
        when(courseService.requireOwnedCourse(courseId, ownerId)).thenReturn(course);
        // ...but the lesson doesn't belong to it, so the AND-both-ids query
        // returns nothing.
        when(lessonRepository.findByIdAndCourseId(foreignLessonId, courseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> lessonService.delete(courseId, foreignLessonId, ownerId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileStorage, never()).delete(anyString());
    }

    @Test
    @DisplayName("deleting a lesson also removes its stored file")
    void deletingLessonRemovesFile() {
        UUID courseId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setFileKey("courses/x/lessons/y/abc.mp4");

        when(courseService.requireOwnedCourse(courseId, ownerId)).thenReturn(course);
        when(lessonRepository.findByIdAndCourseId(lessonId, courseId))
                .thenReturn(Optional.of(lesson));

        lessonService.delete(courseId, lessonId, ownerId);

        verify(fileStorage).delete("courses/x/lessons/y/abc.mp4");
        verify(lessonRepository).delete(lesson);
    }
}
