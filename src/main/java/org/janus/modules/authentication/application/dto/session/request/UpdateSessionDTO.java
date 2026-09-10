package org.janus.modules.authentication.application.dto.session.request;

import java.time.OffsetDateTime;

public record UpdateSessionDTO(
        String ipAddress,
        String userAgent,
        Boolean isRevoked,
        OffsetDateTime expiresAt
) {}