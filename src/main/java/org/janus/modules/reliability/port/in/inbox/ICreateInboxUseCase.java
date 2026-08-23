package org.janus.modules.reliability.port.in.inbox;

import org.janus.modules.reliability.application.dto.inbox.request.CreateInboxDTO;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.shared.domain.result.Result;

public interface ICreateInboxUseCase {
    Result<InboxEntity> execute(CreateInboxDTO dto);
}
