package org.janus.modules.bootstrap.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateRolePermissionUseCaseBootstrap
        implements IApplicationBootstrap {

    private static final Logger log =
            LoggerFactory.getLogger(CreateRolePermissionUseCaseBootstrap.class);

    @Inject
    private BootstrapProperties properties;
    @Inject
    private RoleRepository roleRepository;
    @Inject
    private PermissionRepository permissionRepository;
    @Inject
    private RolePermissionRepository rolePermissionRepository;

    @Override
    @Transactional
    public void execute() {

        if (properties.permissions() == null
                || properties.permissions().isEmpty()) {

            log.info("No initial permissions defined to link with roles.");
            return;
        }

        RoleEntity masterRole = roleRepository
                .findByName("MASTER")
                .orElseThrow(() ->
                        new InternalServerErrorException(
                                "MASTER role not found during bootstrap"
                        )
                );

        int linkedCount = 0;

        for (BootstrapProperties.PermissionConfig item
                : properties.permissions()) {

            PermissionEntity permission = permissionRepository
                    .findByName(item.name())
                    .orElseThrow(() ->
                            new InternalServerErrorException(
                                    "Permission '" + item.name()
                                            + "' not found during bootstrap"
                            )
                    );

            boolean alreadyExists =
                    rolePermissionRepository
                            .existsByRoleIdAndPermissionId(
                                    masterRole.getId(),
                                    permission.getId()
                            );

            if (alreadyExists) {
                log.debug(
                        "Permission '{}' is already assigned to 'MASTER'.",
                        item.name()
                );
                continue;
            }

            RolePermissionEntity rolePermission =
                    RolePermissionEntity.builder()
                            .roleId(masterRole.getId())
                            .permissionId(permission.getId())
                            .effect(PermissionEffectEnum.ALLOW)
                            .conditions(null)
                            .expiresAt(null)
                            .assignedBy(null)
                            .build();

            rolePermissionRepository.insert(rolePermission);

            linkedCount++;

            log.debug(
                    "Permission '{}' linked to 'MASTER' successfully.",
                    item.name()
            );
        }

        if (linkedCount > 0) {
            log.info(
                    "Successfully linked {} new permissions to the 'MASTER' role.",
                    linkedCount
            );
        } else {
            log.info(
                    "All configured permissions are already linked to the 'MASTER' role."
            );
        }
    }
}