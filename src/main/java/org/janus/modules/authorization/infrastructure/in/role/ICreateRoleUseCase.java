package org.janus.modules.authorization.infrastructure.in.role;

import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.shared.domain.result.Result;

public interface ICreateRoleUseCase {
    Result<RoleEntity> execute(CreateRoleDTO dto);
}
