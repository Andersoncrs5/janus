package org.janus.modules.authorization.application.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.in.rolePermission.IDeleteRolePermissionByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class DeleteRolePermissionByIdUseCase implements IDeleteRolePermissionByIdUseCase {
    private final RolePermissionRepository repository;
    
    @Override
    public Result<RolePermissionEntity> execute(UUID id) {
        if (id == null)
            return Result.badRequest("Role Permission Id should be defined");

        int deleted = repository.deleteById(id);

        if (deleted <= 0)
            return Result.notFound("Role Permission not found");

        if (deleted > 1)
            return Result.failure("More than one role permission was deleted for ID: " + id, 500);

        return Result.success();
    }


}
