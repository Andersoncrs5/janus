package org.janus.modules.authentication.port.in.refreshToken;

import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.shared.domain.result.Result;

public interface IFindRefreshTokenByHashUseCase {
    Result<RefreshTokenEntity> execute(String tokenHash);
}