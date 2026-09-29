package com.example.certificates.dto.response;

import com.example.certificates.enums.RuleType;

import java.util.UUID;

public record ProgrammeRuleResponse(
        UUID id,
        UUID programmeId,
        RuleType ruleType,
        Integer threshold
) {
}
