package org.janus.modules.bootstrap.gateway;

import jakarta.enterprise.context.Dependent;
import org.janus.modules.authorization.gateway.permission.PermissionOutboundGateway;
import org.janus.modules.authorization.gateway.role.RoleOutboundGateway;
import org.janus.modules.authorization.gateway.rolePermission.RolePermissionOutboundGateway;
import org.janus.modules.authorization.gateway.userRole.UserRoleOutBoundGateway;
import org.janus.modules.authorization.infrastructure.in.role.ICreateRoleUseCase;
import org.janus.modules.authorization.infrastructure.in.role.IExistsRoleByNameUseCase;
import org.janus.modules.identity.gateway.user.UserOutboundGateway;

@Dependent
public record BootstrapInBoundGateway(
        UserOutboundGateway userOutboundGateway,

        IExistsRoleByNameUseCase existsRoleByNameUseCase,
        ICreateRoleUseCase createRoleUseCase,

        RoleOutboundGateway roleOutboundGateway,

        UserRoleOutBoundGateway userRoleOutBoundGateway,

        PermissionOutboundGateway permissionOutboundGateway,
        RolePermissionOutboundGateway rolePermissionOutboundGateway
) {
}
