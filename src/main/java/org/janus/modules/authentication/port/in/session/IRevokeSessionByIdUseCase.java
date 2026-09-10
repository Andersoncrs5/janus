package org.janus.modules.authentication.port.in.session;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IRevokeSessionByIdUseCase {
    Result<Void> execute(UUID sessionId, UUID userId);
}
