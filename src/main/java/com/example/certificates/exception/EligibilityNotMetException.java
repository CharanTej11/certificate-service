package com.example.certificates.exception;

/**
 * The supplied facts do not satisfy the programme's eligibility rule.
 * Maps to HTTP 422.
 */
public class EligibilityNotMetException extends RuntimeException {

    public EligibilityNotMetException(String message) {
        super(message);
    }
}
