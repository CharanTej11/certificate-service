package com.example.certificates.validation.impl;

import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.enums.RuleType;
import com.example.certificates.validation.EligibilityFacts;
import com.example.certificates.validation.EligibilityResult;
import com.example.certificates.validation.EligibilityRuleEvaluator;
import org.springframework.stereotype.Component;

@Component
public class AnyoneRuleEvaluator implements EligibilityRuleEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.ANYONE;
    }

    @Override
    public void validateConfiguration(Integer threshold) {
        // ANYONE ignores the threshold entirely; nothing to validate.
    }

    @Override
    public EligibilityResult evaluate(ProgrammeRule rule, EligibilityFacts facts) {
        return EligibilityResult.pass();
    }
}
