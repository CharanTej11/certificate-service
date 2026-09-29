package com.example.certificates.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TrackResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        List<TrackProgrammeResponse> programmes,
        Instant createdAt
) {
}
