package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IRestorePermissionByIdUseCase {
    Result<Void> execute(UUID id);
}
