package org.janus.modules.authentication.application.dto.refreshToken.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRefreshTokenDTO(
        @NotNull(message = "Session ID cannot be null")
        UUID sessionId,
        @NotNull(message = "User ID cannot be null")
        UUID userId
) {
}