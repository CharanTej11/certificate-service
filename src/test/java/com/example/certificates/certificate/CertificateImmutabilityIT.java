package com.example.certificates.certificate;

import com.example.certificates.AbstractIntegrationTest;
import com.example.certificates.dto.request.AttachDesignRequest;
import com.example.certificates.dto.request.IssueCertificateRequest;
import com.example.certificates.dto.request.UpdateProgrammeRequest;
import com.example.certificates.dto.response.CertificateResponse;
import com.example.certificates.entity.Design;
import com.example.certificates.entity.Person;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeDesignVersion;
import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.entity.Track;
import com.example.certificates.entity.TrackProgramme;
import com.example.certificates.enums.RuleType;
import com.example.certificates.repository.DesignRepository;
import com.example.certificates.repository.PersonRepository;
import com.example.certificates.repository.ProgrammeDesignVersionRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.ProgrammeRuleRepository;
import com.example.certificates.repository.TrackProgrammeRepository;
import com.example.certificates.repository.TrackRepository;
import com.example.certificates.service.CertificateService;
import com.example.certificates.service.ProgrammeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CertificateImmutabilityIT extends AbstractIntegrationTest {

    @Autowired CertificateService certificateService;
    @Autowired ProgrammeService programmeService;
    @Autowired PersonRepository personRepository;
    @Autowired ProgrammeRepository programmeRepository;
    @Autowired ProgrammeRuleRepository ruleRepository;
    @Autowired DesignRepository designRepository;
    @Autowired ProgrammeDesignVersionRepository versionRepository;
    @Autowired TrackRepository trackRepository;
    @Autowired TrackProgrammeRepository trackProgrammeRepository;

    @Test
    void certificateSurvivesRenameDesignChangeDisableAndTrackRemoval() {
        // Given a person, a programme with a rule, a design attached, and a track.
        Person priya = personRepository.save(new Person(UUID.randomUUID(), "Priya Sharma"));
        Programme sql = programmeRepository.save(new Programme(UUID.randomUUID(),
                "Advanced SQL", "SQL tuning"));
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), sql.getId(), RuleType.MINIMUM_SCORE, 70));

        Design goldV1 = designRepository.save(new Design(UUID.randomUUID(), "Gold Border v1", "<v1/>"));
        versionRepository.save(new ProgrammeDesignVersion(UUID.randomUUID(), sql.getId(), goldV1.getId(),
                1, Instant.now().minus(5, ChronoUnit.DAYS)));

        Track track = trackRepository.save(new Track(UUID.randomUUID(), "Data Engineering Track", null));
        trackProgrammeRepository.save(new TrackProgramme(UUID.randomUUID(), track.getId(), sql.getId(), 0));

        // When the certificate is issued.
        CertificateResponse issued = certificateService.issue(new IssueCertificateRequest(
                priya.getId(), sql.getId(), track.getId(), 85, null));
        String code = issued.publicCode();

        assertThat(issued.programmeName()).isEqualTo("Advanced SQL");
        assertThat(issued.designName()).isEqualTo("Gold Border v1");
        assertThat(issued.trackName()).isEqualTo("Data Engineering Track");

        // When everything about the programme changes afterwards.
        programmeService.update(sql.getId(), new UpdateProgrammeRequest("Advanced SQL and Query Tuning", null));
        Design goldV2 = designRepository.save(new Design(UUID.randomUUID(), "Gold Border v2", "<v2/>"));
        programmeService.attachDesign(sql.getId(), new AttachDesignRequest(goldV2.getId()));
        programmeService.disable(sql.getId());
        trackProgrammeRepository.delete(
                trackProgrammeRepository.findByTrackIdAndProgrammeId(track.getId(), sql.getId()).orElseThrow());

        // Then the certificate still says exactly what it said at issue time.
        CertificateResponse looked = certificateService.getByPublicCode(code);
        assertThat(looked.personName()).isEqualTo("Priya Sharma");
        assertThat(looked.programmeName()).isEqualTo("Advanced SQL");
        assertThat(looked.designName()).isEqualTo("Gold Border v1");
        assertThat(looked.designContent()).isEqualTo("<v1/>");
        assertThat(looked.trackName()).isEqualTo("Data Engineering Track");
        assertThat(looked.issuedAt()).isEqualTo(issued.issuedAt());
    }
}
