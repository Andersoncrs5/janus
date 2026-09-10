package org.janus.modules.authentication.application.dto.session.request;

import java.util.UUID;

public record RefreshSessionDTO(
        UUID sessionId,
        UUID userId,
        Long durationInMinutes
) {
}