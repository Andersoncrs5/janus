package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

public interface IFindAllRolePermissionsByRoleIdUseCase {
    Result<List<RolePermissionEntity>> execute(UUID roleId);
}
