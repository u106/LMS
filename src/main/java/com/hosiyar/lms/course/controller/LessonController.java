package com.hosiyar.lms.course.controller;

import com.hosiyar.lms.common.dto.ApiResponse;
import com.hosiyar.lms.common.security.AuthenticatedUser;
import com.hosiyar.lms.course.dto.FileDownloadResponse;
import com.hosiyar.lms.course.dto.LessonRequest;
import com.hosiyar.lms.course.dto.LessonResponse;
import com.hosiyar.lms.course.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Lessons are a nested resource - they only exist inside a course, and the
 * URL says so. Every write here is gated by ownership of the parent course.
 */
@RestController
@RequestMapping("/api/v1/courses/{courseId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LessonResponse>>> list(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        UUID callerId = caller != null ? caller.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(lessonService.findByCourse(courseId, callerId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LessonResponse>> create(
            @PathVariable UUID courseId,
            @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        LessonResponse response = lessonService.create(courseId, request, caller.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Lesson created"));
    }

    @PutMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<LessonResponse>> update(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        LessonResponse response = lessonService.update(courseId, lessonId, request, caller.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Lesson updated"));
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        lessonService.delete(courseId, lessonId, caller.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Multipart, not JSON - the only endpoint in the project that isn't.
     * The file arrives as a form part rather than a request body.
     */
    @PostMapping(path = "/{lessonId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LessonResponse>> uploadFile(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        LessonResponse response = lessonService.attachFile(courseId, lessonId, file, caller.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "File uploaded"));
    }

    /**
     * Returns a temporary URL rather than the bytes themselves. The browser
     * fetches directly from object storage, so this application never
     * streams a 500 MB video through its own memory.
     */
    @GetMapping("/{lessonId}/file")
    public ResponseEntity<ApiResponse<FileDownloadResponse>> fileUrl(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        UUID callerId = caller != null ? caller.getId() : null;
        return ResponseEntity.ok(ApiResponse.success(
                lessonService.fileUrl(courseId, lessonId, callerId)));
    }

    @DeleteMapping("/{lessonId}/file")
    public ResponseEntity<ApiResponse<LessonResponse>> removeFile(
            @PathVariable UUID courseId,
            @PathVariable UUID lessonId,
            @AuthenticationPrincipal AuthenticatedUser caller
    ) {
        LessonResponse response = lessonService.removeFile(courseId, lessonId, caller.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "File removed"));
    }
}
