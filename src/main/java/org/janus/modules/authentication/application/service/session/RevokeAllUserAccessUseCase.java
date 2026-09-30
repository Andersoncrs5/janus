package org.janus.modules.authentication.application.service.session;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.authentication.port.in.refreshToken.IRevokeAllTokenByUserIdUseCase;
import org.janus.modules.authentication.port.in.session.IRevokeAllUserAccessUseCase;
import org.janus.modules.authentication.port.in.session.IRevokeAllUserSessionsUseCase;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
public class RevokeAllUserAccessUseCase implements IRevokeAllUserAccessUseCase {

    private final IRevokeAllUserSessionsUseCase revokeAllUserSessions;
    private final IRevokeAllTokenByUserIdUseCase revokeAllTokenByUserId;

    @Inject
    public RevokeAllUserAccessUseCase(
            IRevokeAllUserSessionsUseCase revokeAllUserSessions,
            IRevokeAllTokenByUserIdUseCase revokeAllTokenByUserId) {
        this.revokeAllUserSessions = revokeAllUserSessions;
        this.revokeAllTokenByUserId = revokeAllTokenByUserId;
    }

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID userId) {
        Result<Integer> result1 = revokeAllTokenByUserId.execute(userId);
        if (result1.isFailure())
            return Result.failure(result1.getFirstError(), result1.getStatus());

        Result<Integer> result = revokeAllUserSessions.execute(userId);
        if (result.isFailure())
            return Result.failure(result.getFirstError(), result.getStatus());

        return Result.success();
    }
}