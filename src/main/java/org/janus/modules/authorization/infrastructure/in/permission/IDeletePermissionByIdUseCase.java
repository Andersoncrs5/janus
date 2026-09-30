package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IDeletePermissionByIdUseCase {
    @PermissionsAllowed("permissions:delete")
    Result<Void> execute(UUID id);
}
