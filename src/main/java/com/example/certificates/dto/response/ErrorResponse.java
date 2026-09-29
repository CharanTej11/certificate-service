package com.example.certificates.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error envelope for every non-2xx response.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<String> details
) {
    public ErrorResponse(int status, String code, String message, String path, List<String> details) {
        this(Instant.now(), status, code, message, path, details);
    }
}
