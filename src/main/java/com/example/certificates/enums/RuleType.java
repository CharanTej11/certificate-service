package com.example.certificates.enums;

/**
 * Kinds of eligibility rule a programme can be configured with.
 * New kinds are added here plus a matching evaluator; no service logic changes.
 */
public enum RuleType {
    ANYONE,
    MINIMUM_SCORE,
    ALL_TASKS_DONE
}
