package com.example.certificates.exception;

/**
 * A live certificate already exists for this person + programme + route.
 * Maps to HTTP 409.
 */
public class DuplicateCertificateException extends RuntimeException {

    public DuplicateCertificateException(String message) {
        super(message);
    }
}
