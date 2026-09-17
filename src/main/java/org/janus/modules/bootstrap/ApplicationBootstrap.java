package org.janus.modules.bootstrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import org.janus.modules.bootstrap.service.permission.CreatePermissionUseCaseBootstrap;
import org.janus.modules.bootstrap.service.role.CreateRolesUseCase;
import org.janus.modules.bootstrap.service.rolePermission.CreateRolePermissionUseCaseBootstrap;
import org.janus.modules.bootstrap.service.user.CreateUserMasterUseCase;
import org.janus.modules.bootstrap.service.userRole.LinkRoleMasterToUserMasterUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@RequiredArgsConstructor
public class ApplicationBootstrap {

    private static final Logger log = LoggerFactory.getLogger(ApplicationBootstrap.class);

    private final CreateUserMasterUseCase createMasterUser;
    private final CreateRolesUseCase createRolesUseCase;
    private final CreatePermissionUseCaseBootstrap createPermissionUseCase;
    private final LinkRoleMasterToUserMasterUseCase linkRoleMasterToUserMasterUseCase;
    private final CreateRolePermissionUseCaseBootstrap createRolePermissionUseCase;

    public void onStart(@Observes StartupEvent event) {
        log.info("Starting application data bootstrap...");

        createMasterUser.execute();
        createPermissionUseCase.execute();
        createRolesUseCase.execute();
        createRolePermissionUseCase.execute();
        linkRoleMasterToUserMasterUseCase.execute();

        log.info("Application data bootstrap completed successfully.");
    }

}
