package org.janus.modules.authorization.infrastructure.in.userRole;

import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IDeleteUserRoleByIdUseCase {
    @PermissionsAllowed("user-roles:revoke")
    Result<Void> execute(UUID id, UUID userId);
}
