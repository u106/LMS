package com.hosiyar.lms.enrollment.controller;

import com.hosiyar.lms.common.dto.ApiResponse;
import com.hosiyar.lms.common.security.AuthenticatedUser;
import com.hosiyar.lms.enrollment.dto.EnrollRequest;
import com.hosiyar.lms.enrollment.dto.EnrollmentResponse;
import com.hosiyar.lms.enrollment.service.EnrollmentService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    /**
     * Any authenticated user can enrol - enrolling is what makes them a
     * student of the course. The student id comes from the token.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @Valid @RequestBody EnrollRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        EnrollmentResponse response = enrollmentService.enroll(request.courseId(), caller.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Enrolled"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Page<EnrollmentResponse>>> myEnrollments(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PageableDefault(size = 20, sort = "enrolledAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                enrollmentService.findMine(caller.getId(), pageable)));
    }

    /** Keyed by course id - a student unenrols from a course, not by row id. */
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> unenroll(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        enrollmentService.unenroll(courseId, caller.getId());
        return ResponseEntity.noContent().build();
    }
}
