package com.example.certificates.controller;

import com.example.certificates.dto.request.AttachDesignRequest;
import com.example.certificates.dto.request.CreateProgrammeRequest;
import com.example.certificates.dto.request.SetProgrammeRuleRequest;
import com.example.certificates.dto.request.UpdateProgrammeRequest;
import com.example.certificates.dto.response.ProgrammeResponse;
import com.example.certificates.dto.response.ProgrammeRuleResponse;
import com.example.certificates.service.ProgrammeRuleService;
import com.example.certificates.service.ProgrammeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/programmes")
public class ProgrammeController {

    private final ProgrammeService programmeService;
    private final ProgrammeRuleService programmeRuleService;

    public ProgrammeController(ProgrammeService programmeService, ProgrammeRuleService programmeRuleService) {
        this.programmeService = programmeService;
        this.programmeRuleService = programmeRuleService;
    }

    @PostMapping
    public ResponseEntity<ProgrammeResponse> create(@Valid @RequestBody CreateProgrammeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(programmeService.create(request));
    }

    @GetMapping
    public List<ProgrammeResponse> list() {
        return programmeService.list();
    }

    @GetMapping("/{id}")
    public ProgrammeResponse get(@PathVariable UUID id) {
        return programmeService.get(id);
    }

    @PatchMapping("/{id}")
    public ProgrammeResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProgrammeRequest request) {
        return programmeService.update(id, request);
    }

    @PostMapping("/{id}/disable")
    public ProgrammeResponse disable(@PathVariable UUID id) {
        return programmeService.disable(id);
    }

    @PostMapping("/{id}/design")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attachDesign(@PathVariable UUID id, @Valid @RequestBody AttachDesignRequest request) {
        programmeService.attachDesign(id, request);
    }

    @PutMapping("/{id}/rule")
    public ProgrammeRuleResponse setRule(@PathVariable UUID id, @Valid @RequestBody SetProgrammeRuleRequest request) {
        return programmeRuleService.setRule(id, request);
    }

    @GetMapping("/{id}/rule")
    public ProgrammeRuleResponse getRule(@PathVariable UUID id) {
        return programmeRuleService.getRule(id);
    }
}
