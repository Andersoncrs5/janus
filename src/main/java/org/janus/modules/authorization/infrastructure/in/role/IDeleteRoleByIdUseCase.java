package org.janus.modules.authorization.infrastructure.in.role;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IDeleteRoleByIdUseCase {
    Result<Void> execute(UUID id);
}
