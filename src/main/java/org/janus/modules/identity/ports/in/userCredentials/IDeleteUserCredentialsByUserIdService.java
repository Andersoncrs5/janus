package org.janus.modules.identity.ports.in.userCredentials;

import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IDeleteUserCredentialsByUserIdService {
    Result<UserCredentialsEntity> execute(UUID userId);
}
