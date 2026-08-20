package org.janus.modules.identity.ports.in.user;

import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IFindUserByIdUseCase {
    Result<UserEntity> execute(UUID id);
}
