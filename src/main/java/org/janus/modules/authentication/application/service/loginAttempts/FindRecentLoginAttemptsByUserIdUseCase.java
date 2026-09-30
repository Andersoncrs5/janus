package org.janus.modules.authentication.application.service.loginAttempts;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.modules.authentication.port.in.loginAttempts.IFindRecentLoginAttemptsByUserIdUseCase;
import org.janus.modules.authentication.port.out.LoginAttemptRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindRecentLoginAttemptsByUserIdUseCase implements IFindRecentLoginAttemptsByUserIdUseCase {

    private final LoginAttemptRepository repository;

    @Override
    @ResultTransaction
    public Result<List<LoginAttemptEntity>> execute(UUID userId, int limit) {
        if (userId == null) {
            return Result.badRequest("User ID cannot be null");
        }

        if (limit <= 0) {
            return Result.badRequest("Limit must be greater than zero");
        }

        List<LoginAttemptEntity> attempts = repository.findRecentByUserId(userId, limit);

        return Result.success(attempts);
    }
}