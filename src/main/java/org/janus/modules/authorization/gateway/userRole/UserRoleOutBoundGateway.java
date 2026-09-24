package org.janus.modules.authorization.gateway.userRole;

import jakarta.enterprise.context.Dependent;
import org.janus.modules.authorization.infrastructure.in.userRole.ICreateUserRoleUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IExistsByUserIdAndRoleIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRolesOnlyNameByUserIdUseCase;

@Dependent
public record UserRoleOutBoundGateway(
        ICreateUserRoleUseCase createUserRoleUseCase,
        IExistsByUserIdAndRoleIdUseCase existsByUserIdAndRoleIdUseCase,
        IFindRolesOnlyNameByUserIdUseCase findRolesOnlyNameByUserId
) {
}
