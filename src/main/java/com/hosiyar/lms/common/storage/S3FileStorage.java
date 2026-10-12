package com.hosiyar.lms.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * The only class in the project that knows S3 exists.
 */
@Component
public class S3FileStorage implements FileStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3FileStorage(S3Client s3Client, S3Presigner s3Presigner,
                         @Value("${lms.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    @Override
    public String store(String key, InputStream content, long contentLength, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            // The length is passed explicitly so the SDK can stream the body
            // straight through instead of buffering the whole file in memory
            // to work out how big it is.
            s3Client.putObject(request, RequestBody.fromInputStream(content, contentLength));
            return key;
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to upload file to storage", e);
        }
    }

    /**
     * Signs a URL locally using the credentials already held - no network
     * call to AWS happens here, which is why this is cheap enough to do on
     * every request rather than caching the result.
     */
    @Override
    public String presignedDownloadUrl(String key, Duration expiry) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiry)
                    .getObjectRequest(getRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to generate download URL", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            throw new FileStorageException("Failed to delete file from storage", e);
        }
    }
}
