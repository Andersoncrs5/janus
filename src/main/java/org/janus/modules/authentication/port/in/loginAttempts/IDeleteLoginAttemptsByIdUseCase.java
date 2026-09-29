package org.janus.modules.authentication.port.in.loginAttempts;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IDeleteLoginAttemptsByIdUseCase {
    Result<Void> execute(UUID id);
}
