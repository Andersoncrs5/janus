package org.janus.modules.bootstrap.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.modules.authorization.gateway.rolePermission.RolePermissionOutboundGateway;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.shared.infrastructure.properties.BootstrapProperties;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CreateRolePermissionUseCase implements IApplicationBootstrap {
    private final BootstrapProperties properties;
    private final RolePermissionOutboundGateway gateway;

    @Override
    public void execute() {

    }
}
