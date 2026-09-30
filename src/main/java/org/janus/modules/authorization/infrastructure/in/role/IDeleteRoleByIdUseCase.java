package org.janus.modules.authorization.infrastructure.in.role;

import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IDeleteRoleByIdUseCase {
    @PermissionsAllowed("roles:delete")
    Result<Void> execute(UUID id);
}
