package org.janus.modules.authentication.gateway;

import jakarta.enterprise.context.Dependent;
import org.janus.modules.authentication.port.in.session.ICreateSessionUseCase;

@Dependent
public record SessionGatewayOutBound(
        ICreateSessionUseCase createSessionUseCase
) {
}
