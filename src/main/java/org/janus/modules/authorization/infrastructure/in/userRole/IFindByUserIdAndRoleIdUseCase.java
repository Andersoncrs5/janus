package org.janus.modules.authorization.infrastructure.in.userRole;

import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IFindByUserIdAndRoleIdUseCase {
    Result<UserRoleEntity> execute(UUID userId, UUID roleId);
}
