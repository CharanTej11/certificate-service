package com.example.certificates.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProgrammeResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
