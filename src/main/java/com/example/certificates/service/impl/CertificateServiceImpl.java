package com.example.certificates.service.impl;

import com.example.certificates.dto.request.IssueCertificateRequest;
import com.example.certificates.dto.response.CertificateResponse;
import com.example.certificates.dto.response.PageResponse;
import com.example.certificates.entity.Certificate;
import com.example.certificates.entity.Design;
import com.example.certificates.entity.Person;
import com.example.certificates.entity.Programme;
import com.example.certificates.entity.ProgrammeDesignVersion;
import com.example.certificates.entity.ProgrammeRule;
import com.example.certificates.entity.Track;
import com.example.certificates.enums.CertificateStatus;
import com.example.certificates.enums.RouteType;
import com.example.certificates.exception.BusinessRuleException;
import com.example.certificates.exception.DuplicateCertificateException;
import com.example.certificates.exception.EligibilityNotMetException;
import com.example.certificates.exception.NotFoundException;
import com.example.certificates.mapper.DtoMapper;
import com.example.certificates.repository.CertificateRepository;
import com.example.certificates.repository.DesignRepository;
import com.example.certificates.repository.PersonRepository;
import com.example.certificates.repository.ProgrammeDesignVersionRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.repository.ProgrammeRuleRepository;
import com.example.certificates.repository.TrackProgrammeRepository;
import com.example.certificates.repository.TrackRepository;
import com.example.certificates.service.CertificateService;
import com.example.certificates.util.PublicCodeGenerator;
import com.example.certificates.validation.EligibilityFacts;
import com.example.certificates.validation.EligibilityResult;
import com.example.certificates.validation.EligibilityRuleEvaluatorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CertificateServiceImpl implements CertificateService {

    private static final Logger log = LoggerFactory.getLogger(CertificateServiceImpl.class);

    /** Normalized route key for the "on its own" (no track) case. */
    static final String OWN_ROUTE_KEY = "OWN";

    private final CertificateRepository certificateRepository;
    private final PersonRepository personRepository;
    private final ProgrammeRepository programmeRepository;
    private final TrackRepository trackRepository;
    private final TrackProgrammeRepository trackProgrammeRepository;
    private final DesignRepository designRepository;
    private final ProgrammeDesignVersionRepository versionRepository;
    private final ProgrammeRuleRepository ruleRepository;
    private final EligibilityRuleEvaluatorRegistry evaluatorRegistry;
    private final DtoMapper mapper;

    public CertificateServiceImpl(CertificateRepository certificateRepository,
                                  PersonRepository personRepository,
                                  ProgrammeRepository programmeRepository,
                                  TrackRepository trackRepository,
                                  TrackProgrammeRepository trackProgrammeRepository,
                                  DesignRepository designRepository,
                                  ProgrammeDesignVersionRepository versionRepository,
                                  ProgrammeRuleRepository ruleRepository,
                                  EligibilityRuleEvaluatorRegistry evaluatorRegistry,
                                  DtoMapper mapper) {
        this.certificateRepository = certificateRepository;
        this.personRepository = personRepository;
        this.programmeRepository = programmeRepository;
        this.trackRepository = trackRepository;
        this.trackProgrammeRepository = trackProgrammeRepository;
        this.designRepository = designRepository;
        this.versionRepository = versionRepository;
        this.ruleRepository = ruleRepository;
        this.evaluatorRegistry = evaluatorRegistry;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public CertificateResponse issue(IssueCertificateRequest request) {
        Certificate certificate = doIssue(request);
        return mapper.toCertificateResponse(certificate);
    }

    /**
     * Core issue logic. Runs inside the caller's transaction. Package-visible so the
     * bulk service can call it per-item, each in its own transaction.
     */
    Certificate doIssue(IssueCertificateRequest request) {
        Instant issuedAt = Instant.now();

        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> NotFoundException.of("Person", request.personId()));
        Programme programme = programmeRepository.findById(request.programmeId())
                .orElseThrow(() -> NotFoundException.of("Programme", request.programmeId()));

        // Resolve route.
        RouteType routeType;
        String routeKey;
        Track track = null;
        if (request.trackId() == null) {
            routeType = RouteType.OWN;
            routeKey = OWN_ROUTE_KEY;
        } else {
            track = trackRepository.findById(request.trackId())
                    .orElseThrow(() -> NotFoundException.of("Track", request.trackId()));
            if (!trackProgrammeRepository.existsByTrackIdAndProgrammeId(track.getId(), programme.getId())) {
                throw new BusinessRuleException(
                        "Programme " + programme.getId() + " does not belong to track " + track.getId());
            }
            routeType = RouteType.TRACK;
            routeKey = track.getId().toString();
        }

        // Fast duplicate check (the DB unique index is the real guarantee below).
        if (certificateRepository.existsByPersonIdAndProgrammeIdAndRouteTypeAndRouteKeyAndStatus(
                person.getId(), programme.getId(), routeType, routeKey, CertificateStatus.LIVE)) {
            throw new DuplicateCertificateException(
                    "A live certificate already exists for this person, programme and route");
        }

        // Evaluate eligibility using the configured rule (no hardcoded thresholds).
        ProgrammeRule rule = ruleRepository.findByProgrammeId(programme.getId())
                .orElseThrow(() -> new BusinessRuleException(
                        "No eligibility rule configured for programme " + programme.getId()));
        EligibilityResult result = evaluatorRegistry.forType(rule.getRuleType())
                .evaluate(rule, new EligibilityFacts(request.score(), request.tasksCompleted()));
        if (!result.eligible()) {
            throw new EligibilityNotMetException(result.reason());
        }

        // Snapshot the design applicable at issue time (may be none — old-style certs).
        Design design = null;
        ProgrammeDesignVersion version =
                versionRepository.findApplicableAt(programme.getId(), issuedAt).orElse(null);
        if (version != null) {
            design = designRepository.findById(version.getDesignId()).orElse(null);
        }

        Certificate certificate = Certificate.builder()
                .id(UUID.randomUUID())
                .publicCode(PublicCodeGenerator.generate())
                .personId(person.getId())
                .programmeId(programme.getId())
                .trackId(track != null ? track.getId() : null)
                .designId(design != null ? design.getId() : null)
                .programmeDesignVersionId(version != null ? version.getId() : null)
                .personNameSnapshot(person.getName())
                .programmeNameSnapshot(programme.getName())
                .programmeDescriptionSnapshot(programme.getDescription())
                .designNameSnapshot(design != null ? design.getName() : null)
                .designContentSnapshot(design != null ? design.getContent() : null)
                .trackNameSnapshot(track != null ? track.getName() : null)
                .routeType(routeType)
                .routeKey(routeKey)
                .issuedAt(issuedAt)
                .build();

        try {
            // saveAndFlush so the unique-index violation surfaces here, not at commit.
            Certificate saved = certificateRepository.saveAndFlush(certificate);
            log.info("Issued certificate {} (person={}, programme={}, route={})",
                    saved.getPublicCode(), person.getId(), programme.getId(), routeType);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // A concurrent request won the race for this person+programme+route.
            throw new DuplicateCertificateException(
                    "A live certificate already exists for this person, programme and route");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateResponse getByPublicCode(String publicCode) {
        Certificate certificate = certificateRepository.findByPublicCode(publicCode)
                .orElseThrow(() -> NotFoundException.of("Certificate", publicCode));
        // Cancelled certificates are still returned, clearly marked, so verifiers see the truth.
        return mapper.toCertificateResponse(certificate);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CertificateResponse> listForPerson(UUID personId, Pageable pageable) {
        if (!personRepository.existsById(personId)) {
            throw NotFoundException.of("Person", personId);
        }
        return PageResponse.from(
                certificateRepository.findByPersonIdOrderByIssuedAtDesc(personId, pageable),
                mapper::toCertificateResponse);
    }

    @Override
    @Transactional
    public CertificateResponse cancel(UUID certificateId, String reason) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> NotFoundException.of("Certificate", certificateId));
        if (certificate.getStatus() == CertificateStatus.CANCELLED) {
            throw new BusinessRuleException("Certificate is already cancelled");
        }
        certificate.cancel(reason, Instant.now());
        log.info("Cancelled certificate {} (reason={})", certificate.getPublicCode(), reason);
        return mapper.toCertificateResponse(certificate);
    }
}
