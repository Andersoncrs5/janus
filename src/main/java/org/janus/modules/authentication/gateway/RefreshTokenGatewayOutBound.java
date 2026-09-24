package org.janus.modules.authentication.gateway;

import jakarta.enterprise.context.Dependent;
import org.janus.modules.authentication.port.in.refreshToken.ICreateRefreshTokenUseCase;

@Dependent
public record RefreshTokenGatewayOutBound(
        ICreateRefreshTokenUseCase CreateRefreshToken
) {
}
