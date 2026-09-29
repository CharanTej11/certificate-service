package com.example.certificates.validation;

/**
 * The facts a caller supplies about a person, checked against a programme's rule.
 */
public record EligibilityFacts(
        Integer score,
        Integer tasksCompleted
) {
}
