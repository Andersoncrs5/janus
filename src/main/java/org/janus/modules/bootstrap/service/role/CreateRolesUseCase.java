package org.janus.modules.bootstrap.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.slf4j.helpers.MessageFormatter;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CreateRolesUseCase implements IApplicationBootstrap {

    private final BootstrapInBoundGateway gateway;
    private final BootstrapProperties properties;

    @Override
    public void execute() {
        if (properties.roles() == null || properties.roles().isEmpty()) {
            throw new InternalServerErrorException("No initial roles defined for bootstrap.");
        }

        for (BootstrapProperties.RoleConfig item : properties.roles()) {
            Result<Boolean> existsResult = this.gateway.existsRoleByNameUseCase().execute(item.name());

            if (existsResult.isFailure()) {
                var msg = MessageFormatter.format("Failed to check existence for role '{}': {}", item.name(), existsResult.getFirstError()).getMessage();
                throw new InternalServerErrorException(msg);
            }

            if (Boolean.TRUE.equals(existsResult.getData())) {
                log.info("Role '{}' already exists. Skipping creation.", item.name());
                continue;
            }

            CreateRoleDTO dto = new CreateRoleDTO(
                    item.name(),
                    item.description(),
                    item.slug()
            );

            Result<RoleEntity> createResult = gateway.createRoleUseCase().execute(dto);

            if (createResult.isFailure()) {
                var msg = MessageFormatter.format("Failed to create role '{}': {}", item.name(), createResult.getFirstError()).getMessage();
                throw new InternalServerErrorException(msg);
            }

            log.info("Role '{}' created successfully with ID: {}", item.name(), createResult.getData().getId());
        }
    }
}