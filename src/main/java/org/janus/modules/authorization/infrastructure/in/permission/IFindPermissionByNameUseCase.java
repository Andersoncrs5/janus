package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.shared.domain.result.Result;

public interface IFindPermissionByNameUseCase {
    Result<PermissionEntity> execute(String name);
}
