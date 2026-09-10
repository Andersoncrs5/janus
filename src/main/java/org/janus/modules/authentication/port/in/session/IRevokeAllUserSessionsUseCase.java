package org.janus.modules.authentication.port.in.session;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IRevokeAllUserSessionsUseCase {
    Result<Integer> execute(UUID userId);
}
