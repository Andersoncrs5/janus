package org.janus.modules.authorization.gateway.permission;

import jakarta.enterprise.context.Dependent;
import org.janus.modules.authorization.infrastructure.in.permission.ICreatePermissionUseCase;
import org.janus.modules.authorization.infrastructure.in.permission.IExistsPermissionByNameUseCase;

@Dependent
public record PermissionOutboundGateway(
        IExistsPermissionByNameUseCase existsPermissionByNameUseCase,
        ICreatePermissionUseCase createPermissionUseCase
) {
}
