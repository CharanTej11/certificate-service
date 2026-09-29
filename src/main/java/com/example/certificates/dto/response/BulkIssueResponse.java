package com.example.certificates.dto.response;

import com.example.certificates.enums.BulkJobStatus;

import java.util.List;
import java.util.UUID;

public record BulkIssueResponse(
        UUID jobId,
        BulkJobStatus status,
        int total,
        int issued,
        int failed,
        List<BulkItemResult> results
) {
}
