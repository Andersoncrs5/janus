package org.janus.modules.authentication.port.in.loginAttempts;

import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

public interface IFindRecentLoginAttemptsByUserIdUseCase {
    Result<List<LoginAttemptEntity>> execute(UUID userId, int limit);
}