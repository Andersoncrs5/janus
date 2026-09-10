package org.janus.modules.authentication.application.service.session;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.in.session.IFindActiveSessionsByUserIdUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindActiveSessionsByUserIdUseCase implements IFindActiveSessionsByUserIdUseCase {
    private final SessionRepository repository;

    @Override
    public Result<List<SessionEntity>> execute(UUID userId) {
        if (userId == null) return Result.badRequest("User ID cannot be null");
        
        return Result.success(repository.findActiveByUserId(userId));
    }

}
