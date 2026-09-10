package org.janus.modules.authorization.application.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindAllRoleIdsByUserIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindAllRoleIdsByUserIdUseCase implements IFindAllRoleIdsByUserIdUseCase {
    private final UserRoleRepository repository;

    @Override
    public Result<List<UUID>> execute(UUID userId) {
        List<UUID> list = this.repository.findAllRoleIdsByUserId(userId);

        return Result.success(list);
    }

}
