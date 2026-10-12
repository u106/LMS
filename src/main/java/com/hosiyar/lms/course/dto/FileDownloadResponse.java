package com.hosiyar.lms.course.dto;

import java.time.Instant;

/**
 * A temporary link straight to object storage - the bytes never pass through
 * this application. Clients should follow it promptly rather than storing it,
 * since it stops working at expiresAt.
 */
public record FileDownloadResponse(
        String url,
        String fileName,
        String contentType,
        Long fileSize,
        Instant expiresAt
) {}
