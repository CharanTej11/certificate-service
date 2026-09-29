package com.example.certificates.certificate;

import com.example.certificates.AbstractIntegrationTest;
import com.example.certificates.dto.request.IssueCertificateRequest;
import com.example.certificates.entity.Person;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.entity.Track;
import com.example.certificates.entity.TrackProgramme;
import com.example.certificates.enums.RuleType;
import com.example.certificates.exception.DuplicateCertificateException;
import com.example.certificates.repository.PersonRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.ProgrammeRuleRepository;
import com.example.certificates.repository.TrackProgrammeRepository;
import com.example.certificates.repository.TrackRepository;
import com.example.certificates.service.CertificateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class CertificateUniquenessIT extends AbstractIntegrationTest {

    @Autowired CertificateService certificateService;
    @Autowired PersonRepository personRepository;
    @Autowired ProgrammeRepository programmeRepository;
    @Autowired ProgrammeRuleRepository ruleRepository;
    @Autowired TrackRepository trackRepository;
    @Autowired TrackProgrammeRepository trackProgrammeRepository;

    private Person priya;
    private Programme sql;

    private void baseData() {
        priya = personRepository.save(new Person(UUID.randomUUID(), "Priya"));
        sql = programmeRepository.save(new Programme(UUID.randomUUID(), "Advanced SQL", null));
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), sql.getId(), RuleType.ANYONE, null));
    }

    private Track trackWithSql(String name) {
        Track t = trackRepository.save(new Track(UUID.randomUUID(), name, null));
        trackProgrammeRepository.save(new TrackProgramme(UUID.randomUUID(), t.getId(), sql.getId(), 0));
        return t;
    }

    @Test
    void secondCertificateThroughSameTrackIsRefused() {
        baseData();
        Track track = trackWithSql("Data Engineering Track");
        certificateService.issue(new IssueCertificateRequest(priya.getId(), sql.getId(), track.getId(), null, null));

        assertThatThrownBy(() -> certificateService.issue(
                new IssueCertificateRequest(priya.getId(), sql.getId(), track.getId(), null, null)))
                .isInstanceOf(DuplicateCertificateException.class);
    }

    @Test
    void secondCertificateOnItsOwnWithNoTrackIsRefused() {
        baseData();
        certificateService.issue(new IssueCertificateRequest(priya.getId(), sql.getId(), null, null, null));

        // The awkward "on its own" case: no track at all must still be caught.
        assertThatThrownBy(() -> certificateService.issue(
                new IssueCertificateRequest(priya.getId(), sql.getId(), null, null, null)))
                .isInstanceOf(DuplicateCertificateException.class);
    }

    @Test
    void sameProgrammeThroughTwoDifferentTracksIsAllowed() {
        baseData();
        Track dataEng = trackWithSql("Data Engineering Track");
        Track analytics = trackWithSql("Analytics Track");

        var first = certificateService.issue(
                new IssueCertificateRequest(priya.getId(), sql.getId(), dataEng.getId(), null, null));
        var second = certificateService.issue(
                new IssueCertificateRequest(priya.getId(), sql.getId(), analytics.getId(), null, null));

        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void ownAndTrackRoutesCoexist() {
        baseData();
        Track dataEng = trackWithSql("Data Engineering Track");

        assertThatCode(() -> {
            certificateService.issue(new IssueCertificateRequest(priya.getId(), sql.getId(), null, null, null));
            certificateService.issue(new IssueCertificateRequest(priya.getId(), sql.getId(), dataEng.getId(), null, null));
        }).doesNotThrowAnyException();
    }
}
