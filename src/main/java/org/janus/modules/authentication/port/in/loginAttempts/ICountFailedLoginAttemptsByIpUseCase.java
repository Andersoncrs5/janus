package org.janus.modules.authentication.port.in.loginAttempts;

import org.janus.shared.domain.result.Result;

import java.time.OffsetDateTime;

public interface ICountFailedLoginAttemptsByIpUseCase {
    Result<Long> execute(String ipAddress, OffsetDateTime since);
}