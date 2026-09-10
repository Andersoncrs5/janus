package org.janus.modules.authentication.port.in.session;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IDeleteSessionByIdUseCase {
    Result<Void> execute(UUID id);
}
