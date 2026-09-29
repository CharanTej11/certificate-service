package com.example.certificates.bulk;

import com.example.certificates.AbstractIntegrationTest;
import com.example.certificates.dto.request.BulkIssueRequest;
import com.example.certificates.dto.response.BulkIssueResponse;
import com.example.certificates.enums.BulkItemStatus;
import com.example.certificates.entity.Person;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.entity.Track;
import com.example.certificates.entity.TrackProgramme;
import com.example.certificates.enums.RuleType;
import com.example.certificates.repository.PersonRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.ProgrammeRuleRepository;
import com.example.certificates.repository.TrackProgrammeRepository;
import com.example.certificates.repository.TrackRepository;
import com.example.certificates.service.BulkCertificateService;
import com.example.certificates.service.CertificateService;
import com.example.certificates.dto.request.IssueCertificateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BulkIssueIT extends AbstractIntegrationTest {

    @Autowired BulkCertificateService bulkCertificateService;
    @Autowired CertificateService certificateService;
    @Autowired PersonRepository personRepository;
    @Autowired ProgrammeRepository programmeRepository;
    @Autowired ProgrammeRuleRepository ruleRepository;
    @Autowired TrackRepository trackRepository;
    @Autowired TrackProgrammeRepository trackProgrammeRepository;

    @Test
    void bulkIssueIssuesGoodEntriesAndReportsBadOnes() {
        Programme sql = programmeRepository.save(new Programme(UUID.randomUUID(), "Advanced SQL", null));
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), sql.getId(), RuleType.MINIMUM_SCORE, 70));
        Track track = trackRepository.save(new Track(UUID.randomUUID(), "Data Engineering Track", null));
        trackProgrammeRepository.save(new TrackProgramme(UUID.randomUUID(), track.getId(), sql.getId(), 0));

        Person ok1 = personRepository.save(new Person(UUID.randomUUID(), "OK One"));
        Person ok2 = personRepository.save(new Person(UUID.randomUUID(), "OK Two"));
        Person lowScore = personRepository.save(new Person(UUID.randomUUID(), "Low Score"));
        Person alreadyHolds = personRepository.save(new Person(UUID.randomUUID(), "Already Holds"));
        UUID unknownPerson = UUID.randomUUID();

        // Pre-issue for one person so they show up as a duplicate in the bulk run.
        certificateService.issue(new IssueCertificateRequest(
                alreadyHolds.getId(), sql.getId(), track.getId(), 90, null));

        BulkIssueRequest request = new BulkIssueRequest(sql.getId(), track.getId(), List.of(
                new BulkIssueRequest.BulkIssueItem(ok1.getId(), 80, null),
                new BulkIssueRequest.BulkIssueItem(ok2.getId(), 75, null),
                new BulkIssueRequest.BulkIssueItem(lowScore.getId(), 64, null),
                new BulkIssueRequest.BulkIssueItem(alreadyHolds.getId(), 90, null),
                new BulkIssueRequest.BulkIssueItem(unknownPerson, 90, null)
        ));

        BulkIssueResponse response = bulkCertificateService.bulkIssue(request);

        assertThat(response.total()).isEqualTo(5);
        assertThat(response.issued()).isEqualTo(2);
        assertThat(response.failed()).isEqualTo(3);

        assertThat(statusOf(response, lowScore.getId())).isEqualTo(BulkItemStatus.FAILED_ELIGIBILITY);
        assertThat(statusOf(response, alreadyHolds.getId())).isEqualTo(BulkItemStatus.SKIPPED_DUPLICATE);
        assertThat(statusOf(response, unknownPerson)).isEqualTo(BulkItemStatus.FAILED_VALIDATION);
        assertThat(statusOf(response, ok1.getId())).isEqualTo(BulkItemStatus.ISSUED);
        assertThat(statusOf(response, ok2.getId())).isEqualTo(BulkItemStatus.ISSUED);
    }

    private BulkItemStatus statusOf(BulkIssueResponse response, UUID personId) {
        return response.results().stream()
                .filter(r -> r.personId().equals(personId))
                .findFirst().orElseThrow().status();
    }
}
