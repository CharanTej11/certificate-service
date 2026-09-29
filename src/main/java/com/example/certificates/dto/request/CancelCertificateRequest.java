package com.example.certificates.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelCertificateRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
