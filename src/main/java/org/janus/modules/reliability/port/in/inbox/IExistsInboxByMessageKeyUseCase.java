package org.janus.modules.reliability.port.in.inbox;

import org.janus.shared.domain.result.Result;

public interface IExistsInboxByMessageKeyUseCase {
    Result<Boolean> execute(String messageKey);
}