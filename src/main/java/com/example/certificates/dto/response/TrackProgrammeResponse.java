package com.example.certificates.dto.response;

import java.util.UUID;

public record TrackProgrammeResponse(
        UUID programmeId,
        String programmeName,
        int position
) {
}
