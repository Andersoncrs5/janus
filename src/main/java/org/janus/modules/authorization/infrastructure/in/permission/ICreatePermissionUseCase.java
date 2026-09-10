package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface ICreatePermissionUseCase {
    Result<PermissionEntity> execute(CreatePermissionDTO dto, UUID createdBy);
}
