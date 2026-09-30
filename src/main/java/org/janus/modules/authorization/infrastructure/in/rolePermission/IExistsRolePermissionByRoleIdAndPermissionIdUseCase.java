package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IExistsRolePermissionByRoleIdAndPermissionIdUseCase {
    @PermissionsAllowed("role-permissions:read")
    Result<Boolean> execute(UUID roleId, UUID permissionId);
}
