package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IDeleteRolePermissionByIdUseCase {
    @PermissionsAllowed("role-permissions:revoke")
    Result<RolePermissionEntity> execute(UUID id);
}
