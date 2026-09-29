package com.example.certificates.dto.request;

import com.example.certificates.enums.RuleType;
import jakarta.validation.constraints.NotNull;

public record SetProgrammeRuleRequest(
        @NotNull RuleType ruleType,
        Integer threshold
) {
}
