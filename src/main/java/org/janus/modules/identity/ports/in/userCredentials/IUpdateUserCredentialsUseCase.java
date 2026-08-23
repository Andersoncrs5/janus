package org.janus.modules.identity.ports.in.userCredentials;

import org.janus.modules.identity.application.userCredentials.dto.UpdateUserCredentialsDTO;
import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IUpdateUserCredentialsUseCase {
    Result<UserCredentialsEntity> execute(UUID userId, UpdateUserCredentialsDTO dto);
}
