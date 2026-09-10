package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.shared.domain.result.Result;

public interface IExistsPermissionBySlugUseCase {
    Result<Boolean> execute(String slug);
}
