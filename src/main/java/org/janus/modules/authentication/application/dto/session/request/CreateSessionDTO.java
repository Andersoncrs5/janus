package org.janus.modules.authentication.application.dto.session.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateSessionDTO(
        @NotNull(message = "User ID cannot be null")
        UUID userId,

        String ipAddress,

        String userAgent,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        long durationInMinutes
) {}