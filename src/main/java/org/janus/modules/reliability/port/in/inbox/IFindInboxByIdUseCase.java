package org.janus.modules.reliability.port.in.inbox;

import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

import java.util.UUID;

public interface IFindInboxByIdUseCase {
    @PermissionsAllowed("reliability:inbox:read")
    Result<InboxEntity> execute(UUID id);
}