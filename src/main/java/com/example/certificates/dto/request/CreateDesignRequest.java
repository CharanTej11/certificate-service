package com.example.certificates.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDesignRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank String content
) {
}
