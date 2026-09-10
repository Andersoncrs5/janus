package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.shared.domain.result.Result;

public interface IExistsPermissionByNameUseCase {
    Result<Boolean> execute(String name);
}
