package org.janus.modules.bootstrap.service.user;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.modules.identity.application.user.dto.request.CreateUserDTO;
import org.janus.modules.identity.application.userCredentials.dto.CreateUserCredentialsDTO;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.userCredentials.ICreateUserCredentialUseCase;
import org.janus.shared.domain.enums.PasswordAlgorithmEnum;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class CreateUserMasterUseCase implements IApplicationBootstrap {

    private static final Logger log = LoggerFactory.getLogger(CreateUserMasterUseCase.class);
    @Inject
    private BootstrapInBoundGateway gateway;
    @Inject
    private BootstrapProperties properties;
    @Inject
    private ICreateUserCredentialUseCase createUserCredential;

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
            throw new InternalServerErrorException(
                    MessageFormatter.format(
                            "Failed to create user master: {}", executed.getFirstError()).getMessage()
            );
        }

        UserEntity masterUser = executed.getData();

        CreateUserCredentialsDTO dtoCredential = new CreateUserCredentialsDTO(
                properties.master().password(),
                PasswordAlgorithmEnum.ARGON2ID
        );

        Result<UserCredentialsEntity> credentialsEntityResult = createUserCredential.execute(dtoCredential, masterUser.getId());

        if (credentialsEntityResult.isFailure()) {
            throw new InternalServerErrorException(MessageFormatter.format("Failed to create credential to user master: {}", credentialsEntityResult.getFirstError()).getMessage());
        }

        log.info("User master created successfully with ID: {}", masterUser.getId());
    }
}