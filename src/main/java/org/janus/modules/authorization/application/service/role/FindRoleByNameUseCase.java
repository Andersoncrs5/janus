package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.IFindRoleByNameUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.result.Result;

@ApplicationScoped
@RequiredArgsConstructor
public class FindRoleByNameUseCase implements IFindRoleByNameUseCase {
    private final RoleRepository repository;

    @Override
    public Result<RoleEntity> execute(String name) {
        RoleEntity role = repository.findByName(name).orElse(null);

        if (role == null) {
            return Result.notFound("Role not found");
        }

        return Result.success(role);
    }
}
