package com.hosiyar.lms.course.service;

import com.hosiyar.lms.common.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every check here runs BEFORE a single byte reaches S3. Uploading first and
 * validating afterwards means paying to store files you were always going to
 * reject.
 */
@Component
public class LessonFileValidator {

    private static final long MAX_VIDEO_BYTES = 500L * 1024 * 1024;   // 500 MB
    private static final long MAX_DOCUMENT_BYTES = 25L * 1024 * 1024; //  25 MB

    /**
     * An allow-list, not a block-list. A block-list is a promise to have
     * thought of every dangerous type in advance, which nobody can keep.
     */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "video/mp4", "mp4",
            "video/webm", "webm",
            "application/pdf", "pdf",
            "image/png", "png",
            "image/jpeg", "jpg"
    );

    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm");

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.containsKey(contentType)) {
            throw new BadRequestException(
                    "Unsupported file type. Allowed: mp4, webm, pdf, png, jpg");
        }

        long limit = VIDEO_TYPES.contains(contentType) ? MAX_VIDEO_BYTES : MAX_DOCUMENT_BYTES;
        if (file.getSize() > limit) {
            throw new BadRequestException(
                    "File is too large. Limit is " + (limit / 1024 / 1024) + " MB for this type");
        }
    }

    /**
     * Builds the object key the file will be stored under.
     *
     * Note what is NOT used: the uploaded filename. A name like
     * "../../../etc/passwd" or one containing a null byte is untrusted input,
     * and letting it decide a storage path is how path traversal happens.
     * A generated UUID plus an extension derived from the *validated*
     * content type keeps that decision entirely server-side, and means two
     * people uploading "video.mp4" cannot collide.
     */
    public String buildKey(UUID courseId, UUID lessonId, String contentType) {
        String extension = ALLOWED_TYPES.get(contentType);
        return "courses/%s/lessons/%s/%s.%s"
                .formatted(courseId, lessonId, UUID.randomUUID(), extension);
    }
}
