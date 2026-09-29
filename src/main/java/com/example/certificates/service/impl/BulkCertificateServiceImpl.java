package com.example.certificates.service.impl;

import com.example.certificates.dto.request.BulkIssueRequest;
import com.example.certificates.dto.request.IssueCertificateRequest;
import com.example.certificates.dto.response.BulkIssueResponse;
import com.example.certificates.dto.response.BulkItemResult;
import com.example.certificates.dto.response.CertificateResponse;
import com.example.certificates.entity.BulkCertificateJob;
import com.example.certificates.entity.BulkCertificateJobItem;
import com.example.certificates.enums.BulkItemStatus;
import com.example.certificates.exception.BusinessRuleException;
import com.example.certificates.exception.DuplicateCertificateException;
import com.example.certificates.exception.EligibilityNotMetException;
import com.example.certificates.exception.NotFoundException;
import com.example.certificates.repository.BulkCertificateJobItemRepository;
import com.example.certificates.repository.BulkCertificateJobRepository;
import com.example.certificates.repository.ProgrammeRepository;
import com.example.certificates.service.BulkCertificateService;
import com.example.certificates.service.CertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bulk issuing decision: each person is issued in its own independent transaction
 * (many small commits, not one big one). Successes are durable immediately, so if
 * the process dies after 300 of 500, those 300 certificates remain and re-running
 * the request simply skips them as duplicates. This method is intentionally NOT
 * @Transactional so one failing item never rolls back the successful ones.
 */
@Service
public class BulkCertificateServiceImpl implements BulkCertificateService {

    private static final Logger log = LoggerFactory.getLogger(BulkCertificateServiceImpl.class);

    private final CertificateService certificateService;
    private final ProgrammeRepository programmeRepository;
    private final BulkCertificateJobRepository jobRepository;
    private final BulkCertificateJobItemRepository jobItemRepository;

    public BulkCertificateServiceImpl(CertificateService certificateService,
                                      ProgrammeRepository programmeRepository,
                                      BulkCertificateJobRepository jobRepository,
                                      BulkCertificateJobItemRepository jobItemRepository) {
        this.certificateService = certificateService;
        this.programmeRepository = programmeRepository;
        this.jobRepository = jobRepository;
        this.jobItemRepository = jobItemRepository;
    }

    @Override
    public BulkIssueResponse bulkIssue(BulkIssueRequest request) {
        if (!programmeRepository.existsById(request.programmeId())) {
            throw NotFoundException.of("Programme", request.programmeId());
        }

        // Each repository.save() is its own transaction (this method is not transactional),
        // so committed certificates and items survive a mid-run crash.
        BulkCertificateJob job = jobRepository.save(new BulkCertificateJob(
                UUID.randomUUID(), request.programmeId(), request.trackId(), request.people().size()));

        List<BulkItemResult> results = new ArrayList<>();
        int issued = 0;
        int failed = 0;

        for (BulkIssueRequest.BulkIssueItem item : request.people()) {
            BulkItemResult result = processItem(job.getId(), request, item);
            results.add(result);
            if (result.status() == BulkItemStatus.ISSUED) {
                issued++;
            } else {
                failed++;
            }
        }

        job.complete(issued, failed);
        jobRepository.save(job);
        log.info("Bulk job {} finished: {} issued, {} failed", job.getId(), issued, failed);

        return new BulkIssueResponse(job.getId(), job.getStatus(),
                request.people().size(), issued, failed, results);
    }

    private BulkItemResult processItem(UUID jobId, BulkIssueRequest request,
                                       BulkIssueRequest.BulkIssueItem item) {
        BulkItemStatus status;
        UUID certificateId = null;
        String reason = null;
        try {
            // Each call is its own transaction (this method is not transactional).
            CertificateResponse response = certificateService.issue(new IssueCertificateRequest(
                    item.personId(), request.programmeId(), request.trackId(),
                    item.score(), item.tasksCompleted()));
            certificateId = response.id();
            status = BulkItemStatus.ISSUED;
        } catch (DuplicateCertificateException e) {
            status = BulkItemStatus.SKIPPED_DUPLICATE;
            reason = e.getMessage();
        } catch (EligibilityNotMetException e) {
            status = BulkItemStatus.FAILED_ELIGIBILITY;
            reason = e.getMessage();
        } catch (NotFoundException | BusinessRuleException e) {
            status = BulkItemStatus.FAILED_VALIDATION;
            reason = e.getMessage();
        }

        jobItemRepository.save(new BulkCertificateJobItem(
                UUID.randomUUID(), jobId, item.personId(), status, certificateId, reason));
        return new BulkItemResult(item.personId(), status, certificateId, reason);
    }
}
