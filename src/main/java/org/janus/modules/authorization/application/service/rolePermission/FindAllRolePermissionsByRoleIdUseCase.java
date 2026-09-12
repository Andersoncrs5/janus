package org.janus.modules.authorization.application.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.in.rolePermission.IFindAllRolePermissionsByRoleIdUseCase;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindAllRolePermissionsByRoleIdUseCase implements IFindAllRolePermissionsByRoleIdUseCase {

    private final RolePermissionRepository repository;

    @Override
    public Result<List<RolePermissionEntity>> execute(UUID roleId) {
        if (roleId == null) {
            return Result.badRequest("Role ID should be defined");
        }

        try {
            List<RolePermissionEntity> result = repository.findAllByRoleId(roleId);
            return Result.ok(result);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}