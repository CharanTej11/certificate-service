package com.example.certificates.validation.impl;

import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.enums.RuleType;
import com.example.certificates.exception.BusinessRuleException;
import com.example.certificates.validation.EligibilityFacts;
import com.example.certificates.validation.EligibilityResult;
import com.example.certificates.validation.EligibilityRuleEvaluator;
import org.springframework.stereotype.Component;

@Component
public class AllTasksDoneRuleEvaluator implements EligibilityRuleEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.ALL_TASKS_DONE;
    }

    @Override
    public void validateConfiguration(Integer threshold) {
        if (threshold == null) {
            throw new BusinessRuleException("ALL_TASKS_DONE rule requires a threshold");
        }
    }

    @Override
    public EligibilityResult evaluate(ProgrammeRule rule, EligibilityFacts facts) {
        Integer required = rule.getThreshold();
        if (facts.tasksCompleted() == null) {
            return EligibilityResult.fail("tasksCompleted is required for this programme");
        }
        if (facts.tasksCompleted() < required) {
            return EligibilityResult.fail(
                    "tasksCompleted " + facts.tasksCompleted() + " is below the required " + required);
        }
        return EligibilityResult.pass();
    }
}
