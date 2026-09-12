package org.janus.modules.authorization.application.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.infrastructure.in.rolePermission.IExistsRolePermissionByRoleIdAndPermissionIdUseCase;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsRolePermissionByRoleIdAndPermissionIdUseCase implements IExistsRolePermissionByRoleIdAndPermissionIdUseCase {

    private final RolePermissionRepository repository;

    @Override
    public Result<Boolean> execute(UUID roleId, UUID permissionId) {
        if (roleId == null) {
            return Result.badRequest("Role ID should be defined");
        }

        if (permissionId == null) {
            return Result.badRequest("Permission ID should be defined");
        }

        try {
            boolean exists = repository.existsByRoleIdAndPermissionId(roleId, permissionId);
            return Result.ok(exists);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}