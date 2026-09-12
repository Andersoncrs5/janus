package org.janus.modules.authorization.gateway.rolePermission;

import jakarta.enterprise.context.Dependent;
import org.janus.modules.authorization.infrastructure.in.rolePermission.IExistsRolePermissionByRoleIdAndPermissionIdUseCase;

@Dependent
public record RolePermissionOutboundGateway(
        IExistsRolePermissionByRoleIdAndPermissionIdUseCase existsRolePermissionByRoleIdAndPermissionIdUseCase
) {
}
