package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface ICreateRolePermissionUseCase {
    @PermissionsAllowed("role-permissions:create")
    Result<RolePermissionEntity> execute(CreateRolePermissionDTO dto, UUID assignedBy);
}
