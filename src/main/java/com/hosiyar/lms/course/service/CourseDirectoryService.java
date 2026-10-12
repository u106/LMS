package com.hosiyar.lms.course.service;

import com.hosiyar.lms.course.api.CourseDirectory;
import com.hosiyar.lms.course.api.CourseSummary;
import com.hosiyar.lms.course.entity.Course;
import com.hosiyar.lms.course.entity.CourseStatus;
import com.hosiyar.lms.course.repository.CourseRepository;
import com.hosiyar.lms.course.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lives inside the course module; only the CourseDirectory interface it
 * implements is visible to anyone else.
 */
@Service
@RequiredArgsConstructor
public class CourseDirectoryService implements CourseDirectory {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseSummary> findById(UUID courseId) {
        return courseRepository.findById(courseId).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, CourseSummary> findAllByIds(Set<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return courseRepository.findAllById(courseIds).stream()
                .map(this::toSummary)
                .collect(Collectors.toMap(CourseSummary::id, Function.identity()));
    }

    private CourseSummary toSummary(Course course) {
        return new CourseSummary(
                course.getId(),
                course.getTitle(),
                course.getInstructorId(),
                course.getStatus() == CourseStatus.PUBLISHED,
                lessonRepository.countByCourseId(course.getId())
        );
    }
}
