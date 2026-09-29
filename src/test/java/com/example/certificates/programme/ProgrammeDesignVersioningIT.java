package com.example.certificates.programme;

import com.example.certificates.AbstractIntegrationTest;
import com.example.certificates.dto.request.AttachDesignRequest;
import com.example.certificates.entity.Design;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeDesignVersion;
import com.example.certificates.repository.DesignRepository;
import com.example.certificates.repository.ProgrammeDesignVersionRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.service.ProgrammeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProgrammeDesignVersioningIT extends AbstractIntegrationTest {

    @Autowired ProgrammeService programmeService;
    @Autowired ProgrammeRepository programmeRepository;
    @Autowired DesignRepository designRepository;
    @Autowired ProgrammeDesignVersionRepository versionRepository;

    @Test
    void answersWhichDesignAppliedAtAGivenInstant() {
        Programme sql = programmeRepository.save(new Programme(UUID.randomUUID(), "Advanced SQL", null));
        Design v1 = designRepository.save(new Design(UUID.randomUUID(), "Gold Border v1", "<v1/>"));
        Design v2 = designRepository.save(new Design(UUID.randomUUID(), "Gold Border v2", "<v2/>"));

        Instant march1 = Instant.parse("2026-03-01T00:00:00Z");
        Instant march5 = Instant.parse("2026-03-05T00:00:00Z");
        Instant march12 = Instant.parse("2026-03-12T00:00:00Z");
        Instant march15 = Instant.parse("2026-03-15T00:00:00Z");

        // v1 effective from 1 March.
        versionRepository.save(new ProgrammeDesignVersion(UUID.randomUUID(), sql.getId(), v1.getId(), 1, march1));
        // Attaching v2 closes v1 and opens v2. Simulate the 12 March change by editing effective dates.
        ProgrammeDesignVersion open = versionRepository.findByProgrammeIdAndEffectiveToIsNull(sql.getId()).orElseThrow();
        open.close(march12);
        versionRepository.save(new ProgrammeDesignVersion(UUID.randomUUID(), sql.getId(), v2.getId(), 2, march12));

        // On 5 March the applicable design is v1.
        assertThat(versionRepository.findApplicableAt(sql.getId(), march5).orElseThrow().getDesignId())
                .isEqualTo(v1.getId());
        // On 15 March it is v2.
        assertThat(versionRepository.findApplicableAt(sql.getId(), march15).orElseThrow().getDesignId())
                .isEqualTo(v2.getId());
    }

    @Test
    void attachDesignClosesPreviousVersionAndOpensNew() {
        Programme sql = programmeRepository.save(new Programme(UUID.randomUUID(), "SQL " + UUID.randomUUID(), null));
        Design v1 = designRepository.save(new Design(UUID.randomUUID(), "D1", "<d1/>"));
        Design v2 = designRepository.save(new Design(UUID.randomUUID(), "D2", "<d2/>"));

        programmeService.attachDesign(sql.getId(), new AttachDesignRequest(v1.getId()));
        programmeService.attachDesign(sql.getId(), new AttachDesignRequest(v2.getId()));

        ProgrammeDesignVersion current =
                versionRepository.findByProgrammeIdAndEffectiveToIsNull(sql.getId()).orElseThrow();
        assertThat(current.getDesignId()).isEqualTo(v2.getId());
        assertThat(current.getVersion()).isEqualTo(2);
        assertThat(versionRepository.findApplicableAt(sql.getId(), Instant.now().minus(1, ChronoUnit.SECONDS)))
                .isPresent();
    }
}
