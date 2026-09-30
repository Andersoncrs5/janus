package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.modules.authorization.application.dto.permission.request.UpdatePermissionDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IUpdatePermissionUseCase {
    @PermissionsAllowed("permissions:update")
    Result<PermissionEntity> execute(UpdatePermissionDTO dto, UUID id);
}
