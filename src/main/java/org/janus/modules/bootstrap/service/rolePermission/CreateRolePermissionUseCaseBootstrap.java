package org.janus.modules.bootstrap.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.modules.bootstrap.gateway.BootstrapInBoundGateway;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CreateRolePermissionUseCaseBootstrap implements IApplicationBootstrap {

    private final BootstrapProperties properties;
    private final BootstrapInBoundGateway gateway;

    @Override
    @Transactional
    public void execute() {
        if (properties.permissions() == null || properties.permissions().isEmpty()) {
            log.info("No initial permissions defined to link with roles.");
            return;
        }

        Result<RoleEntity> roleResult = gateway.roleOutboundGateway()
                .findRoleByNameUseCase().execute("MASTER");

        if (roleResult.isFailure()) {
            log.error("Failed to find 'MASTER' role to assign permissions: {}", roleResult.getFirstError());
            return;
        }

        RoleEntity masterRole = roleResult.getData();
        int linkedCount = 0;

        for (BootstrapProperties.PermissionConfig permConfig : properties.permissions()) {

            Result<PermissionEntity> permissionResult = gateway.permissionOutboundGateway()
                    .findPermissionByNameUseCase().execute(permConfig.name());

            if (permissionResult.isFailure()) {
                log.warn("Permission '{}' not found in database. Skipping assignment.", permConfig.name());
                continue;
            }

            PermissionEntity permission = permissionResult.getData();

            Result<Boolean> checkResult = gateway.rolePermissionOutboundGateway()
                    .existsRolePermissionByRoleIdAndPermissionIdUseCase()
                    .execute(masterRole.getId(), permission.getId());

            if (checkResult.isFailure()) {
                log.error("Failed to check if permission '{}' is linked to MASTER: {}", permConfig.name(), checkResult.getFirstError());
                continue;
            }

            if (Boolean.TRUE.equals(checkResult.getData())) {
                log.debug("Permission '{}' is already assigned to 'MASTER'.", permConfig.name());
                continue;
            }

            CreateRolePermissionDTO dto = new CreateRolePermissionDTO(
                    masterRole.getId(),
                    permission.getId(),
                    PermissionEffectEnum.ALLOW,
                    null,
                    null
            );

            Result<RolePermissionEntity> createResult = gateway.rolePermissionOutboundGateway()
                    .createRolePermissionUseCase().execute(dto, null);

            if (createResult.isFailure()) {
                log.error("Failed to link permission '{}' to 'MASTER': {}", permConfig.name(), createResult.getFirstError());
            } else {
                linkedCount++;
                log.debug("Permission '{}' linked to 'MASTER' successfully.", permConfig.name());
            }
        }

        if (linkedCount > 0) {
            log.info("Successfully linked {} new permissions to the 'MASTER' role.", linkedCount);
        }
    }
}