package org.janus.modules.authorization.application.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.in.rolePermission.IFindRolePermissionByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindRolePermissionByIdUseCase implements IFindRolePermissionByIdUseCase {

    private final RolePermissionRepository repository;

    @Override
    public Result<RolePermissionEntity> execute(UUID id) {
        if (id == null) {
            return Result.badRequest("Role Permission Id should be defined");
        }

        return repository.findById(id)
                .map(Result::success)
                .orElseGet(() -> Result.notFound("Role Permission not found"));
    }
}