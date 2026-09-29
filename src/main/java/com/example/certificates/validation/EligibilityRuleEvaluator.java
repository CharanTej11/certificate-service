package com.example.certificates.validation;

import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.enums.RuleType;

/**
 * Strategy for one rule kind. Adding a new rule kind = add a new enum value plus
 * one implementation of this interface; no existing code or big if/else changes.
 */
public interface EligibilityRuleEvaluator {

    /** Which rule kind this evaluator handles. */
    RuleType supports();

    /**
     * Validate the rule's own configuration (e.g. threshold present when required).
     * Called when the rule is set through the API.
     */
    void validateConfiguration(Integer threshold);

    /** Decide whether the supplied facts satisfy the rule. */
    EligibilityResult evaluate(ProgrammeRule rule, EligibilityFacts facts);
}
