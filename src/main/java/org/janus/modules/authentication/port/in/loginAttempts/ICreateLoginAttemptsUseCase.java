package org.janus.modules.authentication.port.in.loginAttempts;

import org.janus.modules.authentication.application.dto.loginAttempts.request.CreateLoginAttemptDTO;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface ICreateLoginAttemptsUseCase {
    Result<LoginAttemptEntity> execute(CreateLoginAttemptDTO dto, UUID userId);
}
