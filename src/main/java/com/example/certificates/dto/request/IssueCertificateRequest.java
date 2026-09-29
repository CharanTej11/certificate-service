package com.example.certificates.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Facts supplied by the caller. trackId is nullable: null means "on its own" (OWN route).
 */
public record IssueCertificateRequest(
        @NotNull UUID personId,
        @NotNull UUID programmeId,
        UUID trackId,
        Integer score,
        Integer tasksCompleted
) {
}
