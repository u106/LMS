package com.hosiyar.lms.course.service;

import com.hosiyar.lms.common.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The simplest tests in the module - no mocks at all, because the validator
 * depends on nothing. Pure input in, decision out.
 *
 * Maps to docs/prd/module-02-course-management.md US-7 (file validation).
 */
class LessonFileValidatorTest {

    private final LessonFileValidator validator = new LessonFileValidator();

    @Test
    @DisplayName("a permitted content type and reasonable size passes")
    void validFilePasses() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "lecture.pdf", "application/pdf", new byte[1024]);

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a disallowed content type is rejected")
    void disallowedTypeRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", new byte[1024]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported file type");
    }

    @Test
    @DisplayName("an empty upload is rejected")
    void emptyFileRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No file");
    }

    @Test
    @DisplayName("a document over the size limit is rejected")
    void oversizedDocumentRejected() {
        // 26 MB - just over the 25 MB document limit
        byte[] tooBig = new byte[26 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "huge.pdf", "application/pdf", tooBig);

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("too large");
    }

    @Test
    @DisplayName("the generated key never contains the uploaded filename")
    void keyIgnoresUploadedFilename() {
        UUID courseId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        String key = validator.buildKey(courseId, lessonId, "video/mp4");

        // A malicious filename like "../../../etc/passwd" must never reach the
        // key. The key is built only from ids we control plus a random UUID.
        assertThat(key)
                .startsWith("courses/" + courseId + "/lessons/" + lessonId + "/")
                .endsWith(".mp4")
                .doesNotContain("..");
    }

    @Test
    @DisplayName("two uploads of the same filename get different keys")
    void keysAreUnique() {
        UUID courseId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();

        String first = validator.buildKey(courseId, lessonId, "video/mp4");
        String second = validator.buildKey(courseId, lessonId, "video/mp4");

        assertThat(first).isNotEqualTo(second);
    }
}
