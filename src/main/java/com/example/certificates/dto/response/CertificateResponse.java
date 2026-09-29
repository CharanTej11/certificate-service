package com.example.certificates.dto.response;

import com.example.certificates.enums.CertificateStatus;
import com.example.certificates.enums.RouteType;

import java.time.Instant;
import java.util.UUID;

/**
 * The public view of a certificate. Every display value is a stored snapshot.
 */
public record CertificateResponse(
        UUID id,
        String publicCode,
        String personName,
        String programmeName,
        String programmeDescription,
        String designName,
        String designContent,
        String trackName,
        RouteType route,
        Instant issuedAt,
        CertificateStatus status,
        String cancellationReason,
        Instant cancelledAt
) {
}
