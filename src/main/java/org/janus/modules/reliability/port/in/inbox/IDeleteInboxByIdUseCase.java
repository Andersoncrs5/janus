package org.janus.modules.reliability.port.in.inbox;

import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IDeleteInboxByIdUseCase {
    @PermissionsAllowed("reliability:inbox:purge")
    Result<Void> execute(UUID id);
}