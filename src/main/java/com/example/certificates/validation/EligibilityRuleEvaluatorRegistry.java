package com.example.certificates.validation;

import com.example.certificates.enums.RuleType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the right evaluator for a rule type. Spring injects every
 * EligibilityRuleEvaluator bean, so registering a new one is automatic.
 */
@Component
public class EligibilityRuleEvaluatorRegistry {

    private final Map<RuleType, EligibilityRuleEvaluator> byType = new EnumMap<>(RuleType.class);

    public EligibilityRuleEvaluatorRegistry(List<EligibilityRuleEvaluator> evaluators) {
        for (EligibilityRuleEvaluator e : evaluators) {
            byType.put(e.supports(), e);
        }
    }

    public EligibilityRuleEvaluator forType(RuleType type) {
        EligibilityRuleEvaluator evaluator = byType.get(type);
        if (evaluator == null) {
            throw new IllegalStateException("No evaluator registered for rule type " + type);
        }
        return evaluator;
    }
}
