package com.hosiyar.lms.common.storage;

/**
 * Wraps whatever the underlying storage provider throws, so callers never
 * need to catch an AWS-specific exception type.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
