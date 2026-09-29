package com.example.certificates.exception;

/**
 * Requested resource does not exist. Maps to HTTP 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String what, Object id) {
        return new NotFoundException(what + " not found: " + id);
    }
}
