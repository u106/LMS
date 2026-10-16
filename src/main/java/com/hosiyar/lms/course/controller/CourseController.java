package com.hosiyar.lms.course.controller;

import com.hosiyar.lms.common.dto.ApiResponse;
import com.hosiyar.lms.common.security.AuthenticatedUser;
import com.hosiyar.lms.course.dto.CourseResponse;
import com.hosiyar.lms.course.dto.CourseSummaryResponse;
import com.hosiyar.lms.course.dto.CreateCourseRequest;
import com.hosiyar.lms.course.dto.UpdateCourseRequest;
import com.hosiyar.lms.course.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final  CourseService courseService;

    /**
     * Instructor-only - the role rule lives in SecurityConfig. The owner is
     * taken from the token, so a caller cannot create a course under
     * someone else's name.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @Valid @RequestBody CreateCourseRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        CourseResponse response = courseService.create(request, caller.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Course created"));
    }

    /** Public catalogue - no authentication required, PUBLISHED only. */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CourseSummaryResponse>>> catalogue(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.findPublished(pageable)));
    }

    /**
     * Declared before /{id} so "me" is never mistaken for a course id.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<CourseSummaryResponse>>> myCourses(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(courseService.findMine(caller.getId(), pageable)));
    }

    /**
     * Public for a PUBLISHED course. The principal is null for anonymous
     * visitors, which the service treats as "not the owner".
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> findById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        UUID callerId = caller != null ? caller.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(courseService.findById(id, callerId)));
    }

    /**
     * Ownership is checked in the service, not here. A check in the
     * controller only protects this one path - anything else reaching the
     * service by another route would bypass it entirely.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCourseRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        CourseResponse response = courseService.update(id, request, caller.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Course updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        courseService.delete(id, caller.getId());
        return ResponseEntity.noContent().build();
    }
}
