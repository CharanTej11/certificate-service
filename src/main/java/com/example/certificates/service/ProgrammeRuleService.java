package com.example.certificates.service;

import com.example.certificates.dto.request.SetProgrammeRuleRequest;
import com.example.certificates.dto.response.ProgrammeRuleResponse;

import java.util.UUID;

public interface ProgrammeRuleService {

    /** Create or replace the eligibility rule for a programme. */
    ProgrammeRuleResponse setRule(UUID programmeId, SetProgrammeRuleRequest request);

    ProgrammeRuleResponse getRule(UUID programmeId);
}
