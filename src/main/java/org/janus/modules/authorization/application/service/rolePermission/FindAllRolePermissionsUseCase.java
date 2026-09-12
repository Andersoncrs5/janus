package org.janus.modules.authorization.application.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.rolePermission.filter.RolePermissionFilterDTO;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.in.rolePermission.IFindAllRolePermissionsUseCase;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.result.Result;

@ApplicationScoped
@RequiredArgsConstructor
public class FindAllRolePermissionsUseCase implements IFindAllRolePermissionsUseCase {

    private final RolePermissionRepository repository;

    @Override
    public Result<Page<RolePermissionEntity>> execute(RolePermissionFilterDTO filter) {
        if (filter == null) {
            return Result.badRequest("RolePermissionFilterDTO must not be null");
        }

        try {
            Page<RolePermissionEntity> result = repository.findAll(filter);
            return Result.ok(result);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}