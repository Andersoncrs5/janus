package org.janus.modules.bootstrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
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

    @Inject
    private CreateUserMasterUseCase createMasterUser;
    @Inject
    private CreateRolesUseCase createRolesUseCase;
    @Inject
    private CreatePermissionUseCaseBootstrap createPermissionUseCase;
    @Inject
    private LinkRoleMasterToUserMasterUseCase linkRoleMasterToUserMasterUseCase;
    @Inject
    private CreateRolePermissionUseCaseBootstrap createRolePermissionUseCase;

    public void onStart(@Observes StartupEvent event) {
        log.info("Starting application data bootstrap...");

        createRolesUseCase.execute();
        createPermissionUseCase.execute();
        createRolePermissionUseCase.execute();
        createMasterUser.execute();
        linkRoleMasterToUserMasterUseCase.execute();

        log.info("Application data bootstrap completed successfully.");
    }

}
