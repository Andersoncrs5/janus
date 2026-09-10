package org.janus.modules.authorization.infrastructure.in.userRole;

import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.shared.domain.result.Result;

public interface ICreateUserRoleUseCase {
    Result<UserRoleEntity> execute(CreateUserRoleDTO dto);
}
