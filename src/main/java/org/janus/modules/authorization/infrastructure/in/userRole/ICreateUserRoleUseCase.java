package org.janus.modules.authorization.infrastructure.in.userRole;

import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface ICreateUserRoleUseCase {
    @PermissionsAllowed("user-roles:assign")
    Result<UserRoleEntity> execute(CreateUserRoleDTO dto, UUID assignId);
}
