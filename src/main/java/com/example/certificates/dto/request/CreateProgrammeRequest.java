package com.example.certificates.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProgrammeRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description
) {
}
