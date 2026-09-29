package com.example.certificates.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DesignResponse(
        UUID id,
        String name,
        String content,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
