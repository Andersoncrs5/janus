package org.janus.modules.authorization.infrastructure.in.role;

import org.janus.modules.authorization.application.dto.role.request.UpdateRoleDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IUpdateRoleUseCase {
    @PermissionsAllowed("roles:update")
    Result<RoleEntity> execute(UUID id, UpdateRoleDTO dto);
}
