package com.example.certificates.controller;

import com.example.certificates.dto.request.BulkIssueRequest;
import com.example.certificates.dto.request.CancelCertificateRequest;
import com.example.certificates.dto.request.IssueCertificateRequest;
import com.example.certificates.dto.response.BulkIssueResponse;
import com.example.certificates.dto.response.CertificateResponse;
import com.example.certificates.dto.response.PageResponse;
import com.example.certificates.service.BulkCertificateService;
import com.example.certificates.service.CertificateService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CertificateController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CertificateService certificateService;
    private final BulkCertificateService bulkCertificateService;

    public CertificateController(CertificateService certificateService,
                                 BulkCertificateService bulkCertificateService) {
        this.certificateService = certificateService;
        this.bulkCertificateService = bulkCertificateService;
    }

    @PostMapping("/certificates")
    public ResponseEntity<CertificateResponse> issue(@Valid @RequestBody IssueCertificateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(certificateService.issue(request));
    }

    @PostMapping("/certificates/bulk")
    public ResponseEntity<BulkIssueResponse> bulkIssue(@Valid @RequestBody BulkIssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bulkCertificateService.bulkIssue(request));
    }

    /** Public lookup by the code printed on the certificate. Works forever. */
    @GetMapping("/certificates/{publicCode}")
    public CertificateResponse lookup(@PathVariable String publicCode) {
        return certificateService.getByPublicCode(publicCode);
    }

    @PostMapping("/certificates/{id}/cancel")
    public CertificateResponse cancel(@PathVariable UUID id,
                                      @Valid @RequestBody CancelCertificateRequest request) {
        return certificateService.cancel(id, request.reason());
    }

    /** A person's certificates, newest first, paginated. */
    @GetMapping("/people/{personId}/certificates")
    public PageResponse<CertificateResponse> listForPerson(
            @PathVariable UUID personId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return certificateService.listForPerson(personId, pageable);
    }
}
