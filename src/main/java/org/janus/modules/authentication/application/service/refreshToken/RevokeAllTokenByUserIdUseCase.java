package org.janus.modules.authentication.application.service.refreshToken;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.port.in.refreshToken.IRevokeAllTokenByUserIdUseCase;
import org.janus.modules.authentication.port.out.RefreshTokenRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class RevokeAllTokenByUserIdUseCase implements IRevokeAllTokenByUserIdUseCase {

    private final RefreshTokenRepository repository;

    @Override
    @ResultTransaction
    public Result<Integer> execute(UUID userId) {
        if (userId == null) return Result.badRequest("User ID cannot be null");

        int revokedCount = repository.revokeAllByUserId(userId);

        return Result.success(revokedCount);
    }
}