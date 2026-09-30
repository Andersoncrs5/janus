package org.janus.modules.bootstrap.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.slf4j.helpers.MessageFormatter;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class LinkRoleMasterToUserMasterUseCase implements IApplicationBootstrap {

    private final BootstrapInBoundGateway gateway;
    private final BootstrapProperties properties;
    private final UserRoleRepository repository;

    @Override
    @Transactional
    public void execute() {
        String masterEmail = properties.master().email();

        Result<UserEntity> userResult = gateway.userOutboundGateway()
                .findUserByEmailUseCase().execute(properties.master().email());

        if (userResult.isFailure()) {
            var msg = MessageFormatter.format("Failed to find master user with email '{}': {}", masterEmail, userResult.getFirstError()).getMessage();
            throw new InternalServerErrorException(msg);
        }

        Result<RoleEntity> roleResult = gateway.roleOutboundGateway()
                .findRoleByNameUseCase().execute("MASTER");

        if (roleResult.isFailure()) {
            var msg = MessageFormatter.format("Failed to find 'MASTER' role during master assignment: {}", roleResult.getFirstError()).getMessage();
            throw new InternalServerErrorException(msg);
        }

        UserEntity user = userResult.getData();
        RoleEntity role = roleResult.getData();

        Result<Boolean> checkResult = gateway.userRoleOutBoundGateway()
                .existsByUserIdAndRoleIdUseCase().execute(user.getId(), role.getId());

        if (checkResult.isFailure()) {
            var msg = MessageFormatter.format("Failed to check user-role existence for user '{}': {}", user.getId(), checkResult.getFirstError()).getMessage();
            throw new InternalServerErrorException(msg);
        }

        if (Boolean.TRUE.equals(checkResult.getData())) {
            log.info("Role 'MASTER' is already assigned to master user.");
            return;
        }

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setRoleId(role.getId());
        userRole.setUserId(user.getId());
        userRole.setAssignedById(null);

        UserRoleEntity inserted = repository.insert(userRole);

        log.info("Role 'MASTER' successfully linked to master user (ID: {}).", user.getId());
    }
}