package org.janus.modules.authentication.port.in.session;

import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

public interface IFindActiveSessionsByUserIdUseCase {

    Result<List<SessionEntity>> execute(UUID userId);
}