package org.janus.modules.bootstrap.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.modules.bootstrap.contract.IApplicationBootstrap;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.BootstrapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
@RequiredArgsConstructor
public class CreatePermissionUseCaseBootstrap implements IApplicationBootstrap {

    private static final Logger log =
            LoggerFactory.getLogger(CreatePermissionUseCaseBootstrap.class);

    private final BootstrapProperties properties;
    private final PermissionRepository repository;

    @Override
    @Transactional
    public void execute() {

        if (properties.permissions() == null || properties.permissions().isEmpty()) {
            log.info("No initial permissions defined for bootstrap.");
            return;
        }

        for (BootstrapProperties.PermissionConfig item : properties.permissions()) {

            boolean existsResult =
                    repository.existsByName(item.name());

            if (existsResult) {
                log.info(
                        "Permission '{}' already exists. Skipping creation.",
                        item.name()
                );
                continue;
            }

            PermissionEntity permission = PermissionEntity.builder()
                    .name(item.name())
                    .slug(item.slug())
                    .description(item.description())
                    .module(item.module())
                    .resource(item.resource())
                    .action(item.action())
                    .riskLevel(item.riskLevel())
                    .isActive(item.isActive())
                    .isSystem(item.isSystem())
                    .metadata(item.metadata().orElse(null))
                    .createdBy(null)
                    .build();

            PermissionEntity inserted = repository.insert(permission);

            log.info(
                    "Permission '{}' created successfully with ID: {}",
                    inserted.getName(),
                    inserted.getId()
            );
        }
    }
}