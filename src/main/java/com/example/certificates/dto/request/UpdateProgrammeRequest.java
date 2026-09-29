package com.example.certificates.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Partial update for a programme (rename / re-describe). Null fields are left unchanged.
 */
public record UpdateProgrammeRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String description
) {
}
