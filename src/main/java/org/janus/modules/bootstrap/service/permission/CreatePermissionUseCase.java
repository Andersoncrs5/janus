package org.janus.modules.bootstrap.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@RequiredArgsConstructor
public class CreatePermissionUseCase implements IApplicationBootstrap {

    private static final Logger log = LoggerFactory.getLogger(CreatePermissionUseCase.class);
    private final BootstrapProperties properties;
    private final BootstrapInBoundGateway gateway;

    @Override
    public void execute() {
        if (properties.permissions() == null || properties.permissions().isEmpty()) {
            log.info("No initial roles defined for bootstrap.");
            return;
        }

        for (BootstrapProperties.PermissionConfig item : properties.permissions()) {
            Result<Boolean> existsResult = gateway.permissionOutboundGateway().existsPermissionByNameUseCase().execute(item.name());

            if (existsResult.isFailure()) {
                throw new InternalServerErrorException("Failed to check existence for role " + item.name() + " : " + existsResult.getFirstError());
            }

            if (Boolean.TRUE.equals(existsResult.getData())) {
                log.info("Permission '{}' already exists. Skipping creation.", item.name());
                continue;
            }

            CreatePermissionDTO dto = new CreatePermissionDTO(
                    item.name(),
                    item.slug(),
                    item.description(),
                    item.module(),
                    item.resource(),
                    item.action(),
                    item.riskLevel(),
                    item.isActive(),
                    item.isSystem(),
                    item.metadata().orElse(null)
            );

            Result<PermissionEntity>
                    createResult = gateway.permissionOutboundGateway().createPermissionUseCase().execute(dto, null);


            if (createResult.isFailure()) {
                // STR("Failed to create role '{}': {}", item.name(), createResult.getFirstError());
                continue;
            }

            log.info("Role '{}' created successfully with ID: {}", item.name(), createResult.getData().getId());
        }
    }
}
