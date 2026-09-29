package com.example.certificates.exception;

/**
 * A business invariant was violated (e.g. bad rule configuration,
 * programme not in the supplied track). Maps to HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
