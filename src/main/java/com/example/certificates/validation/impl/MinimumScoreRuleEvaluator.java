package com.example.certificates.validation.impl;

import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.enums.RuleType;
import com.example.certificates.exception.BusinessRuleException;
import com.example.certificates.validation.EligibilityFacts;
import com.example.certificates.validation.EligibilityResult;
import com.example.certificates.validation.EligibilityRuleEvaluator;
import org.springframework.stereotype.Component;

@Component
public class MinimumScoreRuleEvaluator implements EligibilityRuleEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.MINIMUM_SCORE;
    }

    @Override
    public void validateConfiguration(Integer threshold) {
        if (threshold == null) {
            throw new BusinessRuleException("MINIMUM_SCORE rule requires a threshold");
        }
    }

    @Override
    public EligibilityResult evaluate(ProgrammeRule rule, EligibilityFacts facts) {
        Integer required = rule.getThreshold();
        if (facts.score() == null) {
            return EligibilityResult.fail("score is required for this programme");
        }
        if (facts.score() < required) {
            return EligibilityResult.fail(
                    "score " + facts.score() + " is below the required minimum of " + required);
        }
        return EligibilityResult.pass();
    }
}
