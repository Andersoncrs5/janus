package org.janus.modules.authentication.port.in.refreshToken;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IDeleteRefreshTokenById {
    Result<Void> execute(UUID id);
}
