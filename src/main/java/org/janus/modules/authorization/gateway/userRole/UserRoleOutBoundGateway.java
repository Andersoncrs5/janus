package org.janus.modules.authorization.gateway.userRole;

import org.janus.modules.authorization.infrastructure.in.userRole.ICreateUserRoleUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IExistsByUserIdAndRoleIdUseCase;
import jakarta.enterprise.context.Dependent;

@Dependent
public record UserRoleOutBoundGateway(
        ICreateUserRoleUseCase createUserRoleUseCase,
        IExistsByUserIdAndRoleIdUseCase existsByUserIdAndRoleIdUseCase
) {
}
