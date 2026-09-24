package org.janus.modules.authentication.port.in.refreshToken;

import org.janus.modules.authentication.application.dto.refreshToken.request.CreateRefreshTokenDTO;
import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.shared.domain.result.Result;

public interface ICreateRefreshTokenUseCase {
    Result<RefreshTokenEntity> execute(CreateRefreshTokenDTO dto);
}
