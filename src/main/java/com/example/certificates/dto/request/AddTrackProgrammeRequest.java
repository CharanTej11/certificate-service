package com.example.certificates.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddTrackProgrammeRequest(
        @NotNull UUID programmeId,
        Integer position
) {
}
