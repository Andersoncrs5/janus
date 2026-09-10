package org.janus.modules.authentication.port.in.session;

import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IFindSessionByIdUseCase {
    Result<SessionEntity> execute(UUID id);
}
