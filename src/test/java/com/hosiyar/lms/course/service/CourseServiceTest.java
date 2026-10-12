package com.hosiyar.lms.course.service;

import com.hosiyar.lms.common.exception.AccessDeniedByOwnershipException;
import com.hosiyar.lms.common.exception.ResourceNotFoundException;
import com.hosiyar.lms.common.storage.FileStorage;
import com.hosiyar.lms.course.dto.UpdateCourseRequest;
import com.hosiyar.lms.course.entity.Course;
import com.hosiyar.lms.course.entity.CourseStatus;
import com.hosiyar.lms.course.entity.Lesson;
import com.hosiyar.lms.course.repository.CourseRepository;
import com.hosiyar.lms.course.repository.LessonRepository;
import com.hosiyar.lms.user.api.UserDirectory;
import com.hosiyar.lms.user.api.UserSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The security-critical tests of the module. Maps to
 * docs/prd/module-02-course-management.md - especially US-5, the ownership
 * rejection, flagged as the one that matters most before any code existed.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private UserDirectory userDirectory;
    @Mock private FileStorage fileStorage;

    @InjectMocks private CourseService courseService;

    private UUID ownerId;
    private UUID otherId;
    private Course course;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        otherId = UUID.randomUUID();

        course = new Course();
        course.setTitle("Spring Security in depth");
        course.setDescription("Filters, JWT, authorization");
        course.setInstructorId(ownerId);
        course.setStatus(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("the owner can update their own course")
    void ownerCanUpdate() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userDirectory.findById(ownerId))
                .thenReturn(Optional.of(new UserSummary(ownerId, "Priya")));

        UpdateCourseRequest request =
                new UpdateCourseRequest("New title", "New desc", CourseStatus.PUBLISHED);

        courseService.update(courseId, request, ownerId);

        assertThat(course.getTitle()).isEqualTo("New title");
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
    }

    @Test
    @DisplayName("a non-owner updating a course is rejected and nothing changes")
    void nonOwnerCannotUpdate() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        UpdateCourseRequest request =
                new UpdateCourseRequest("Hijacked", "x", CourseStatus.PUBLISHED);

        assertThatThrownBy(() -> courseService.update(courseId, request, otherId))
                .isInstanceOf(AccessDeniedByOwnershipException.class);

        // The important half: the title was NOT changed before the throw.
        // A weaker test would check only that an exception was raised.
        assertThat(course.getTitle()).isEqualTo("Spring Security in depth");
        assertThat(course.getStatus()).isEqualTo(CourseStatus.DRAFT);
    }

    @Test
    @DisplayName("a non-owner deleting a course is rejected and delete is never called")
    void nonOwnerCannotDelete() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.delete(courseId, otherId))
                .isInstanceOf(AccessDeniedByOwnershipException.class);

        verify(courseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleting a course removes its lessons' files from storage first")
    void deleteCleansUpStoredFiles() {
        UUID courseId = UUID.randomUUID();

        Lesson withFile = new Lesson();
        withFile.setCourse(course);
        withFile.setFileKey("courses/x/lessons/y/abc.mp4");

        Lesson withoutFile = new Lesson();
        withoutFile.setCourse(course);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.findAllByCourseIdOrderByPositionAsc(courseId))
                .thenReturn(List.of(withFile, withoutFile));

        courseService.delete(courseId, ownerId);

        // The S3 object is deleted (orphan prevention), and only for the
        // lesson that actually had a file.
        verify(fileStorage).delete("courses/x/lessons/y/abc.mp4");
        verify(fileStorage, times(1)).delete(any());
        verify(courseRepository).delete(course);
    }

    @Test
    @DisplayName("a draft is not visible to a non-owner - reported as not found")
    void draftHiddenFromNonOwner() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        // 404, NOT the ownership 403 - a stranger shouldn't learn the draft
        // even exists.
        assertThatThrownBy(() -> courseService.findById(courseId, otherId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listing a page of courses resolves instructors in ONE batch call")
    void listingUsesBatchLookupNotPerRow() {
        Course c1 = new Course();
        c1.setInstructorId(ownerId);
        c1.setStatus(CourseStatus.PUBLISHED);
        Course c2 = new Course();
        c2.setInstructorId(otherId);
        c2.setStatus(CourseStatus.PUBLISHED);

        when(courseRepository.findAllByStatus(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(c1, c2)));
        when(userDirectory.findAllByIds(anySet()))
                .thenReturn(Map.of(
                        ownerId, new UserSummary(ownerId, "Priya"),
                        otherId, new UserSummary(otherId, "Arjun")));

        courseService.findPublished(org.springframework.data.domain.Pageable.unpaged());

        // The N+1 guard: findAllByIds is called exactly once for the whole
        // page, and the per-row findById is never used. This can't count real
        // SQL - that's the chapter 2 log demo - but it does lock in the batch
        // shape so a future refactor can't quietly reintroduce N+1.
        verify(userDirectory, times(1)).findAllByIds(anySet());
        verify(userDirectory, never()).findById(any());
    }
}
