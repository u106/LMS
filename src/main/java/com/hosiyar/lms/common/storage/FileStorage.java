package com.hosiyar.lms.common.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * How any module stores and retrieves binary files.
 *
 * The point of this interface is that nothing outside this package imports
 * the AWS SDK. Swapping S3 for local disk, MinIO, or Google Cloud Storage
 * means writing one new implementation and changing nothing else - see
 * ADR-007.
 */
public interface FileStorage {

    /**
     * Uploads a file and returns the key it was stored under.
     *
     * The key is supplied by the caller rather than derived from the
     * uploaded filename, so a malicious filename can never influence where
     * the object lands.
     */
    String store(String key, InputStream content, long contentLength, String contentType);

    /**
     * A temporary URL the browser can use to fetch the object directly.
     *
     * This is why the bucket can stay completely private: nothing is public,
     * and the application never has to stream the bytes through itself.
     * The URL stops working once it expires.
     */
    String presignedDownloadUrl(String key, Duration expiry);

    void delete(String key);
}
