package com.example.certificates.validation;

/**
 * Outcome of evaluating a rule. When not eligible, reason explains why (used in bulk reporting).
 */
public record EligibilityResult(
        boolean eligible,
        String reason
) {
    public static EligibilityResult pass() {
        return new EligibilityResult(true, null);
    }

    public static EligibilityResult fail(String reason) {
        return new EligibilityResult(false, reason);
    }
}
