package org.janus.modules.bootstrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.janus.modules.bootstrap.service.role.CreateRolesUseCase;
import org.janus.modules.bootstrap.service.user.CreateUserMasterUseCase;
import org.janus.modules.bootstrap.service.userRole.LinkRoleMasterToUserMasterUseCase;

@JBossLog
@ApplicationScoped
@RequiredArgsConstructor
public class ApplicationBootstrap {

    private final CreateUserMasterUseCase createMasterUser;
    private final CreateRolesUseCase createRolesUseCase;
    private final LinkRoleMasterToUserMasterUseCase linkRoleMasterToUserMasterUseCase;

    public void onStart(@Observes StartupEvent event) {
        log.info("Starting application data bootstrap...");

        createMasterUser.execute();
        createRolesUseCase.execute();
        linkRoleMasterToUserMasterUseCase.execute();

        log.info("Application data bootstrap completed successfully.");
    }

}
