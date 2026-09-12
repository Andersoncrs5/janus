package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.modules.authorization.application.dto.rolePermission.request.UpdateRolePermissionDTO;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IUpdateRolePermissionUseCase {
    Result<RolePermissionEntity> execute(UpdateRolePermissionDTO dto, UUID id);
}
