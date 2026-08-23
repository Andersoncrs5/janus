package org.janus.modules.reliability.port.in.inbox;

import org.janus.modules.reliability.application.dto.inbox.request.UpdateInboxDTO;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

public interface IUpdateInboxUseCase {
    Result<InboxEntity> execute(UUID id, UpdateInboxDTO dto);
}
