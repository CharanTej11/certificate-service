package com.example.certificates.seed;

import com.example.certificates.entity.Certificate;
import com.example.certificates.entity.Design;
import com.example.certificates.entity.Person;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeDesignVersion;
import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.entity.Track;
import com.example.certificates.entity.TrackProgramme;
import com.example.certificates.enums.RouteType;
import com.example.certificates.enums.RuleType;
import com.example.certificates.repository.CertificateRepository;
import com.example.certificates.repository.DesignRepository;
import com.example.certificates.repository.PersonRepository;
import com.example.certificates.repository.ProgrammeDesignVersionRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.ProgrammeRuleRepository;
import com.example.certificates.repository.TrackProgrammeRepository;
import com.example.certificates.repository.TrackRepository;
import com.example.certificates.util.PublicCodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Loads a self-contained demo dataset:
 *  - two tracks that share a programme (Advanced SQL),
 *  - all three rule kinds in use,
 *  - a few old certificates with no design.
 * Idempotent: running it twice does not duplicate data.
 */
@Service
public class DataSeedService {

    private static final Logger log = LoggerFactory.getLogger(DataSeedService.class);

    private final PersonRepository personRepository;
    private final ProgrammeRepository programmeRepository;
    private final DesignRepository designRepository;
    private final ProgrammeDesignVersionRepository versionRepository;
    private final ProgrammeRuleRepository ruleRepository;
    private final TrackRepository trackRepository;
    private final TrackProgrammeRepository trackProgrammeRepository;
    private final CertificateRepository certificateRepository;

    public DataSeedService(PersonRepository personRepository,
                           ProgrammeRepository programmeRepository,
                           DesignRepository designRepository,
                           ProgrammeDesignVersionRepository versionRepository,
                           ProgrammeRuleRepository ruleRepository,
                           TrackRepository trackRepository,
                           TrackProgrammeRepository trackProgrammeRepository,
                           CertificateRepository certificateRepository) {
        this.personRepository = personRepository;
        this.programmeRepository = programmeRepository;
        this.designRepository = designRepository;
        this.versionRepository = versionRepository;
        this.ruleRepository = ruleRepository;
        this.trackRepository = trackRepository;
        this.trackProgrammeRepository = trackProgrammeRepository;
        this.certificateRepository = certificateRepository;
    }

    @Transactional
    public Map<String, Object> seed() {
        Map<String, Object> summary = new LinkedHashMap<>();

        boolean alreadySeeded = programmeRepository.findAll().stream()
                .anyMatch(p -> "Advanced SQL".equals(p.getName()));
        if (alreadySeeded) {
            summary.put("status", "already_seeded");
            return summary;
        }

        Instant now = Instant.now();

        // Designs
        Design goldV1 = designRepository.save(new Design(UUID.randomUUID(),
                "Gold Border v1", "<div class='gold-v1'>{{name}}</div>"));
        Design goldV2 = designRepository.save(new Design(UUID.randomUUID(),
                "Gold Border v2", "<div class='gold-v2'>{{name}}</div>"));

        // Programmes
        Programme advancedSql = programmeRepository.save(new Programme(UUID.randomUUID(),
                "Advanced SQL", "Deep dive into SQL performance and query tuning."));
        Programme dataModelling = programmeRepository.save(new Programme(UUID.randomUUID(),
                "Data Modelling", "Relational and dimensional modelling."));
        Programme reporting = programmeRepository.save(new Programme(UUID.randomUUID(),
                "Reporting", "Building and sharing reports."));
        Programme onboarding = programmeRepository.save(new Programme(UUID.randomUUID(),
                "Onboarding", "Company onboarding basics."));

        // Rules (all three kinds)
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), advancedSql.getId(), RuleType.MINIMUM_SCORE, 70));
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), dataModelling.getId(), RuleType.ALL_TASKS_DONE, 8));
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), reporting.getId(), RuleType.ANYONE, null));
        ruleRepository.save(new ProgrammeRule(UUID.randomUUID(), onboarding.getId(), RuleType.ANYONE, null));

        // Attach Gold Border v1 to Advanced SQL (current version).
        versionRepository.save(new ProgrammeDesignVersion(
                UUID.randomUUID(), advancedSql.getId(), goldV1.getId(), 1, now.minus(30, ChronoUnit.DAYS)));

        // Tracks sharing Advanced SQL.
        Track dataEngineering = trackRepository.save(new Track(UUID.randomUUID(),
                "Data Engineering Track", "SQL + modelling."));
        Track analytics = trackRepository.save(new Track(UUID.randomUUID(),
                "Analytics Track", "SQL + reporting."));

        trackProgrammeRepository.save(new TrackProgramme(UUID.randomUUID(),
                dataEngineering.getId(), advancedSql.getId(), 0));
        trackProgrammeRepository.save(new TrackProgramme(UUID.randomUUID(),
                dataEngineering.getId(), dataModelling.getId(), 1));
        trackProgrammeRepository.save(new TrackProgramme(UUID.randomUUID(),
                analytics.getId(), advancedSql.getId(), 0));
        trackProgrammeRepository.save(new TrackProgramme(UUID.randomUUID(),
                analytics.getId(), reporting.getId(), 1));

        // People
        Person priya = personRepository.save(new Person(UUID.randomUUID(), "Priya Sharma"));
        Person alex = personRepository.save(new Person(UUID.randomUUID(), "Alex Chen"));
        Person sam = personRepository.save(new Person(UUID.randomUUID(), "Sam Okoro"));

        // A few old certificates with no design (issued before designs existed).
        int oldCount = 0;
        for (Person p : new Person[]{priya, alex, sam}) {
            Certificate legacy = Certificate.builder()
                    .id(UUID.randomUUID())
                    .publicCode(PublicCodeGenerator.generate())
                    .personId(p.getId())
                    .programmeId(onboarding.getId())
                    .personNameSnapshot(p.getName())
                    .programmeNameSnapshot(onboarding.getName())
                    .programmeDescriptionSnapshot(onboarding.getDescription())
                    .routeType(RouteType.OWN)
                    .routeKey("OWN")
                    .issuedAt(now.minus(365, ChronoUnit.DAYS))
                    .build();
            certificateRepository.save(legacy);
            oldCount++;
        }

        summary.put("status", "seeded");
        summary.put("designs", new String[]{goldV1.getName(), goldV2.getName()});
        summary.put("programmes", Map.of(
                "Advanced SQL", advancedSql.getId(),
                "Data Modelling", dataModelling.getId(),
                "Reporting", reporting.getId(),
                "Onboarding", onboarding.getId()));
        summary.put("tracks", Map.of(
                "Data Engineering Track", dataEngineering.getId(),
                "Analytics Track", analytics.getId()));
        summary.put("people", Map.of(
                "Priya Sharma", priya.getId(),
                "Alex Chen", alex.getId(),
                "Sam Okoro", sam.getId()));
        summary.put("oldCertificatesWithoutDesign", oldCount);

        log.info("Seed data loaded");
        return summary;
    }
}
