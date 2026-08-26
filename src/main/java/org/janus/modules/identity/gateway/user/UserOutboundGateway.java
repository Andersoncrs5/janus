package org.janus.modules.identity.gateway.user;

import org.janus.modules.identity.ports.in.user.ICreateUserUseCase;
import org.janus.modules.identity.ports.in.user.IExistsUserByEmailUseCase;
import org.janus.modules.identity.ports.in.user.IFindUserByEmailUseCase;
import jakarta.enterprise.context.Dependent;

@Dependent
public record UserOutboundGateway(
        ICreateUserUseCase createUserUseCase,
        IExistsUserByEmailUseCase existsUserByEmailUseCase,
        IFindUserByEmailUseCase findUserByEmailUseCase
) {

}
