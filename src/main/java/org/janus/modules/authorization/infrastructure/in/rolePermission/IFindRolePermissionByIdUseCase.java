package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IFindRolePermissionByIdUseCase {
    Result<RolePermissionEntity> execute(UUID id);
}