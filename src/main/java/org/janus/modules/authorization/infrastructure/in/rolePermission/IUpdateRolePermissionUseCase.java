package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.modules.authorization.application.dto.rolePermission.request.UpdateRolePermissionDTO;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IUpdateRolePermissionUseCase {
    @PermissionsAllowed("role-permissions:update")
    Result<RolePermissionEntity> execute(UpdateRolePermissionDTO dto, UUID id);
}
