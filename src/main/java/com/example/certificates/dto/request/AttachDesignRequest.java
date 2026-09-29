package com.example.certificates.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachDesignRequest(
        @NotNull UUID designId
) {
}
