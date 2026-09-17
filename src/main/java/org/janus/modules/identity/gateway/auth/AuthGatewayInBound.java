package org.janus.modules.identity.gateway.auth;

import jakarta.enterprise.context.Dependent;
import org.janus.modules.authentication.gateway.RefreshTokenGatewayOutBound;
import org.janus.modules.authentication.gateway.SessionGatewayOutBound;

@Dependent
public record AuthGatewayInBound(
        SessionGatewayOutBound sessionGateway,
        RefreshTokenGatewayOutBound refreshTokenGatewayOutBound
) {
}
