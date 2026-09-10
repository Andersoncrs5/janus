package org.janus.modules.authorization.application.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.in.userRole.IExistsByUserIdAndRoleIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindByUserIdAndRoleIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.result.Result;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindByUserIdAndRoleIdUseCase implements IFindByUserIdAndRoleIdUseCase {

    private final UserRoleRepository repository;

    @Override
    public Result<UserRoleEntity> execute(UUID userId, UUID roleId) {
        Optional<UserRoleEntity> optional = repository.findByUserIdAndRoleId(userId, roleId);

        return optional.map(Result::success).orElseGet(() -> Result.notFound("User Role not found"));
    }

}
