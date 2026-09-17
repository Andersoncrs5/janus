package org.janus.modules.authentication.port.in.loginAttempts;

import org.janus.shared.domain.result.Result;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ICountFailedLoginAttemptsByUserIdUseCase {
    Result<Long> execute(UUID userId, OffsetDateTime since);
}