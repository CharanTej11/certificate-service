package com.example.certificates.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Issue one programme (optionally through one track) to many people at once.
 */
public record BulkIssueRequest(
        @NotNull UUID programmeId,
        UUID trackId,
        @NotEmpty @Valid List<BulkIssueItem> people
) {
    public record BulkIssueItem(
            @NotNull UUID personId,
            Integer score,
            Integer tasksCompleted
    ) {
    }
}
