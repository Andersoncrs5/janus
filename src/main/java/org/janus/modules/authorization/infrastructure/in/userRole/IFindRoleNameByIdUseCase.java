package org.janus.modules.authorization.infrastructure.in.userRole;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IFindRoleNameByIdUseCase {
    Result<String> execute(UUID id);
}
