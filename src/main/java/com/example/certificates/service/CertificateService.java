package com.example.certificates.service;

import com.example.certificates.dto.request.IssueCertificateRequest;
import com.example.certificates.dto.response.CertificateResponse;
import com.example.certificates.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CertificateService {

    /**
     * Issue a certificate. Captures immutable snapshots, enforces one-live-per-route
     * uniqueness (in the DB) and is safe under concurrency. Throws on failure.
     */
    CertificateResponse issue(IssueCertificateRequest request);

    CertificateResponse getByPublicCode(String publicCode);

    PageResponse<CertificateResponse> listForPerson(UUID personId, Pageable pageable);

    CertificateResponse cancel(UUID certificateId, String reason);
}
