package org.janus.modules.authorization.infrastructure.in.role;

import org.janus.shared.domain.result.Result;

public interface IExistsRoleByNameUseCase {
    Result<Boolean> execute(String name);
}
