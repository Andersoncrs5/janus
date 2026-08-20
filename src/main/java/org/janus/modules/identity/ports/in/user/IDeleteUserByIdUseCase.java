package org.janus.modules.identity.ports.in.user;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IDeleteUserByIdUseCase {
    Result<Void> execute(UUID id);
}
