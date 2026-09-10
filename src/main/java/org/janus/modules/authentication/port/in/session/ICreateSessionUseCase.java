package org.janus.modules.authentication.port.in.session;

import org.janus.modules.authentication.application.dto.session.request.CreateSessionDTO;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.shared.domain.result.Result;

public interface ICreateSessionUseCase {
    Result<SessionEntity> execute(CreateSessionDTO command);
}
