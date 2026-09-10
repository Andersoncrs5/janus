package org.janus.modules.bootstrap.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class LinkRoleMasterToUserMasterUseCase implements IApplicationBootstrap {

    private final BootstrapInBoundGateway gateway;
    private final BootstrapProperties properties;

    @Override
    public void execute() {
        Result<UserEntity> userResult = gateway.userOutboundGateway()
                .findUserByEmailUseCase().execute(properties.master().email());

        if (userResult.isFailure()) {
            log.error("Failed to find master user during role assignment: {}", userResult.getFirstError());
            return;
        }

        Result<RoleEntity> roleResult = gateway.roleOutboundGateway()
                .findRoleByNameUseCase().execute("MASTER");

        if (roleResult.isFailure()) {
            log.error("Failed to find 'MASTER' role during master assignment: {}", roleResult.getFirstError());
            return;
        }

        UserEntity user = userResult.getData();
        RoleEntity role = roleResult.getData();

        Result<Boolean> checkResult = gateway.userRoleOutBoundGateway()
                .existsByUserIdAndRoleIdUseCase().execute(user.getId(), role.getId());

        if (checkResult.isFailure()) {
            log.error("Failed to check user-role existence: {}", checkResult.getFirstError());
            return;
        }

        if (Boolean.TRUE.equals(checkResult.getData())) {
            log.info("Role 'MASTER' is already assigned to master user.");
            return;
        }

        Result<UserRoleEntity> createResult = gateway.userRoleOutBoundGateway()
                .createUserRoleUseCase()
                .execute(new CreateUserRoleDTO(user.getId(), role.getId()));

        if (createResult.isFailure()) {
            log.error("Failed to link 'MASTER' role to master user: {}", createResult.getFirstError());
            return;
        }

        log.info("Role 'MASTER' successfully linked to master user (ID: {}).", user.getId());
    }
}