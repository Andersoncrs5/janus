package org.janus.modules.authorization.gateway.role;

import org.janus.modules.authorization.infrastructure.in.role.ICreateRoleUseCase;
import org.janus.modules.authorization.infrastructure.in.role.IExistsRoleByNameUseCase;
import org.janus.modules.authorization.infrastructure.in.role.IFindRoleByNameUseCase;
import jakarta.enterprise.context.Dependent;

@Dependent
public record RoleOutboundGateway(
        IExistsRoleByNameUseCase existsRoleByNameUseCase,
        ICreateRoleUseCase createRoleUseCase,
        IFindRoleByNameUseCase findRoleByNameUseCase
) {
}
