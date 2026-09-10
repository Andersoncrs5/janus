package org.janus.modules.identity.ports.in.user;

import jakarta.validation.constraints.Email;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.result.Result;

public interface IFindUserByEmailUseCase {
    Result<UserEntity> execute(@Email String email);
}
