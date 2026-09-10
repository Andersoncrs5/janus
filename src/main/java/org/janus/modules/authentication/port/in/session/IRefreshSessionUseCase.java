package org.janus.modules.authentication.port.in.session;

import org.janus.modules.authentication.application.dto.session.request.RefreshSessionDTO;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.shared.domain.result.Result;

public interface IRefreshSessionUseCase {
    Result<SessionEntity> execute(RefreshSessionDTO dto);
}