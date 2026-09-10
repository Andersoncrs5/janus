package org.janus.modules.bootstrap.service.user;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.modules.identity.application.user.dto.CreateUserDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CreateUserMasterUseCase implements IApplicationBootstrap {

    private final BootstrapInBoundGateway gateway;
    private final BootstrapProperties properties;

    public void execute() {
        Result<Boolean> booleanResult = gateway.userOutboundGateway().existsUserByEmailUseCase().execute(properties.master().email());

        if (booleanResult.isFailure()) {
            log.error("Failed to check master user existence: {}", booleanResult.getFirstError());
            return;
        }

        if (Boolean.TRUE.equals(booleanResult.getData())) {
            log.info("User master already exists.");
            return;
        }

        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail(properties.master().email());
        dto.setFullName(properties.master().fullName());
        dto.setUsername(properties.master().username());
        dto.setPassword(properties.master().password());

        Result<UserEntity> executed = gateway.userOutboundGateway().createUserUseCase().execute(dto);

        if (executed.isFailure()) {
            log.error("Failed to create user master: {}", executed.getFirstError());
            return;
        }

        UserEntity masterUser = executed.getData();
        log.info("User master created successfully with ID: {}", masterUser.getId());
    }
}