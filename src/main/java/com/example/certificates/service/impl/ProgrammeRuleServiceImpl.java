package com.example.certificates.service.impl;

import com.example.certificates.dto.request.SetProgrammeRuleRequest;
import com.example.certificates.dto.response.ProgrammeRuleResponse;
import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.exception.NotFoundException;
import com.example.certificates.mapper.DtoMapper;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.ProgrammeRuleRepository;
import com.example.certificates.service.ProgrammeRuleService;
import com.example.certificates.validation.EligibilityRuleEvaluatorRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProgrammeRuleServiceImpl implements ProgrammeRuleService {

    private final ProgrammeRuleRepository ruleRepository;
    private final ProgrammeRepository programmeRepository;
    private final EligibilityRuleEvaluatorRegistry evaluatorRegistry;
    private final DtoMapper mapper;

    public ProgrammeRuleServiceImpl(ProgrammeRuleRepository ruleRepository,
                                    ProgrammeRepository programmeRepository,
                                    EligibilityRuleEvaluatorRegistry evaluatorRegistry,
                                    DtoMapper mapper) {
        this.ruleRepository = ruleRepository;
        this.programmeRepository = programmeRepository;
        this.evaluatorRegistry = evaluatorRegistry;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ProgrammeRuleResponse setRule(UUID programmeId, SetProgrammeRuleRequest request) {
        if (!programmeRepository.existsById(programmeId)) {
            throw NotFoundException.of("Programme", programmeId);
        }
        // Validate the configuration against the strategy for this rule type.
        evaluatorRegistry.forType(request.ruleType()).validateConfiguration(request.threshold());

        ProgrammeRule rule = ruleRepository.findByProgrammeId(programmeId)
                .map(existing -> {
                    existing.update(request.ruleType(), request.threshold());
                    return existing;
                })
                .orElseGet(() -> ruleRepository.save(
                        new ProgrammeRule(UUID.randomUUID(), programmeId,
                                request.ruleType(), request.threshold())));

        return mapper.toRuleResponse(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgrammeRuleResponse getRule(UUID programmeId) {
        return ruleRepository.findByProgrammeId(programmeId)
                .map(mapper::toRuleResponse)
                .orElseThrow(() -> new NotFoundException("No rule configured for programme " + programmeId));
    }
}
