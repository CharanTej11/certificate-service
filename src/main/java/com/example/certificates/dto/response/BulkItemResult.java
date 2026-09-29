package com.example.certificates.dto.response;

import com.example.certificates.enums.BulkItemStatus;

import java.util.UUID;

public record BulkItemResult(
        UUID personId,
        BulkItemStatus status,
        UUID certificateId,
        String reason
) {
}
