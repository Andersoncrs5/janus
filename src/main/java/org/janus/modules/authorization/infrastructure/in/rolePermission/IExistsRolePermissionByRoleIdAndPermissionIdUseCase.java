package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IExistsRolePermissionByRoleIdAndPermissionIdUseCase {
    Result<Boolean> execute(UUID roleId, UUID permissionId);
}
