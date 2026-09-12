package org.janus.modules.authorization.infrastructure.in.role;

import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.shared.domain.result.Result;

public interface IFindRoleByNameUseCase {
    Result<RoleEntity> execute(String name);
}
