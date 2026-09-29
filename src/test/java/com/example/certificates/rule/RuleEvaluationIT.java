package com.example.certificates.rule;

import com.example.certificates.AbstractIntegrationTest;
import com.example.certificates.dto.request.IssueCertificateRequest;
import com.example.certificates.entity.Person;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.enums.RuleType;
import com.example.certificates.exception.EligibilityNotMetException;
import com.example.certificates.repository.PersonRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.ProgrammeRuleRepository;
import com.example.certificates.service.CertificateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RuleEvaluationIT extends AbstractIntegrationTest {

    @Autowired CertificateService certificateService;
    @Autowired PersonRepository personRepository;
    @Autowired ProgrammeRepository programmeRepository;
    @Autowired ProgrammeRuleRepository ruleRepository;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private Person newPerson() {
        return personRepository.save(new Person(UUID.randomUUID(), "P-" + UUID.randomUUID()));
    }

    private Programme newProgramme(RuleType type, Integer threshold) {
        Programme p = programmeRepository.save(new Programme(UUID.randomUUID(), "Prog-" + UUID.randomUUID(), null));
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), p.getId(), type, threshold));
        return p;
    }

    @Test
    void anyoneAlwaysPasses() {
        Programme p = newProgramme(RuleType.ANYONE, null);
        assertThatCode(() -> certificateService.issue(
                new IssueCertificateRequest(newPerson().getId(), p.getId(), null, null, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void minimumScorePassesAtOrAboveThresholdAndFailsBelow() {
        Programme p = newProgramme(RuleType.MINIMUM_SCORE, 70);
        assertThatCode(() -> certificateService.issue(
                new IssueCertificateRequest(newPerson().getId(), p.getId(), null, 70, null)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> certificateService.issue(
                new IssueCertificateRequest(newPerson().getId(), p.getId(), null, 64, null)))
                .isInstanceOf(EligibilityNotMetException.class);
    }

    @Test
    void allTasksDonePassesAtOrAboveAndFailsBelow() {
        Programme p = newProgramme(RuleType.ALL_TASKS_DONE, 8);
        assertThatCode(() -> certificateService.issue(
                new IssueCertificateRequest(newPerson().getId(), p.getId(), null, null, 8)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> certificateService.issue(
                new IssueCertificateRequest(newPerson().getId(), p.getId(), null, null, 5)))
                .isInstanceOf(EligibilityNotMetException.class);
    }

    @Test
    void changingThresholdThroughApiTakesEffectWithoutRestart() throws Exception {
        Programme p = newProgramme(RuleType.MINIMUM_SCORE, 70);

        // Score 65 fails while threshold is 70.
        assertThatThrownBy(() -> certificateService.issue(
                new IssueCertificateRequest(newPerson().getId(), p.getId(), null, 65, null)))
                .isInstanceOf(EligibilityNotMetException.class);

        // Lower threshold to 65 via the API — configuration change, no redeploy.
        mockMvc.perform(put("/api/v1/programmes/{id}/rule", p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ruleType\":\"MINIMUM_SCORE\",\"threshold\":65}"))
                .andExpect(status().isOk());

        // Same score 65 now succeeds immediately.
        var response = certificateService.issue(
                new IssueCertificateRequest(newPerson().getId(), p.getId(), null, 65, null));
        assertThat(response.publicCode()).isNotBlank();
    }
}
