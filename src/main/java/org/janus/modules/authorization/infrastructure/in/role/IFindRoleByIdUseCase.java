package org.janus.modules.authorization.infrastructure.in.role;

import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IFindRoleByIdUseCase {
    Result<RoleEntity> execute(UUID id);
}
