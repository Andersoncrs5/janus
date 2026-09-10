package org.janus.modules.authorization.application.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.infrastructure.in.userRole.IExistsActiveByUserIdAndRoleIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsActiveByUserIdAndRoleIdUseCase implements IExistsActiveByUserIdAndRoleIdUseCase {
    private final UserRoleRepository repository;

    @Override
    public Result<Boolean> execute(UUID userId, UUID roleId) {
        return Result.success(repository.existsActiveByUserIdAndRoleId(userId, roleId));
    }
}
