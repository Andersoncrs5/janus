package org.janus.modules.identity.ports.in.user;

import org.janus.modules.identity.application.user.dto.CreateUserDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.result.Result;

public interface ICreateUserUseCase {
    Result<UserEntity> execute(CreateUserDTO dto);
}
