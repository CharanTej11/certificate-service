package com.example.certificates.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PersonResponse(
        UUID id,
        String name,
        Instant createdAt
) {
}
