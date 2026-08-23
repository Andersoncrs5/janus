package org.janus.modules.reliability.port.in.inbox;

import org.janus.modules.reliability.application.dto.inbox.filter.InboxFilterDTO;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.shared.domain.page.Page;

public interface IFindAllInboxUseCase {
    Page<InboxEntity> execute(InboxFilterDTO dto);
}
