package org.janus.modules.reliability.port.in.inbox;

import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IDeleteInboxByIdUseCase {
    Result<Void> execute(UUID id);
}