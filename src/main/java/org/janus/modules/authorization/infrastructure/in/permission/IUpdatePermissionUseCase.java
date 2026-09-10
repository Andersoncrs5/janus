package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.modules.authorization.application.dto.permission.request.UpdatePermissionDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IUpdatePermissionUseCase {
    Result<PermissionEntity> execute(UpdatePermissionDTO dto, UUID id);
}
