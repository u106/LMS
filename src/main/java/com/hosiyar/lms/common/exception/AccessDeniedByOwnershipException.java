package com.hosiyar.lms.common.exception;

/**
 * Thrown when a caller is authenticated and has the right role, but does not
 * own the specific thing they are trying to touch.
 *
 * Distinct from BadRequestException (the caller did something wrong) and
 * ResourceNotFoundException (it isn't there). This one means: it exists, we
 * know exactly who you are, and it still isn't yours.
 */
public class AccessDeniedByOwnershipException extends RuntimeException {
    public AccessDeniedByOwnershipException(String message) {
        super(message);
    }
}
