package com.example.certificates.certificate;

import com.example.certificates.AbstractIntegrationTest;
import com.example.certificates.dto.request.IssueCertificateRequest;
import com.example.certificates.dto.response.CertificateResponse;
import com.example.certificates.dto.response.PageResponse;
import com.example.certificates.entity.Person;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.enums.RuleType;
import com.example.certificates.repository.PersonRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.ProgrammeRuleRepository;
import com.example.certificates.service.CertificateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateNoDesignIT extends AbstractIntegrationTest {

    @Autowired CertificateService certificateService;
    @Autowired PersonRepository personRepository;
    @Autowired ProgrammeRepository programmeRepository;
    @Autowired ProgrammeRuleRepository ruleRepository;

    @Test
    void certificateWithNoDesignCanBeIssuedLookedUpAndListed() {
        // A programme with no design attached at all.
        Person person = personRepository.save(new Person(UUID.randomUUID(), "No Design Person"));
        Programme programme = programmeRepository.save(new Programme(UUID.randomUUID(), "Onboarding", "Basics"));
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), programme.getId(), RuleType.ANYONE, null));

        CertificateResponse issued = certificateService.issue(
                new IssueCertificateRequest(person.getId(), programme.getId(), null, null, null));

        assertThat(issued.designName()).isNull();
        assertThat(issued.designContent()).isNull();

        // Looked up by id.
        CertificateResponse looked = certificateService.getByPublicCode(issued.publicCode());
        assertThat(looked.programmeName()).isEqualTo("Onboarding");
        assertThat(looked.designName()).isNull();

        // Appears in the person's list.
        PageResponse<CertificateResponse> page =
                certificateService.listForPerson(person.getId(), PageRequest.of(0, 10));
        assertThat(page.content()).anyMatch(c -> c.publicCode().equals(issued.publicCode()));
    }
}
