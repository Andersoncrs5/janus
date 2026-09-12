package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface ICreateRolePermissionUseCase {
    Result<RolePermissionEntity> execute(CreateRolePermissionDTO dto, UUID assignedBy);
}
