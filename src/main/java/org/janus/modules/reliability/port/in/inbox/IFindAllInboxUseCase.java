package org.janus.modules.reliability.port.in.inbox;

import org.janus.modules.reliability.application.dto.inbox.filter.InboxFilterDTO;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.shared.domain.page.Page;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

public interface IFindAllInboxUseCase {
    @PermissionsAllowed("reliability:inbox:read")
    Page<InboxEntity> execute(InboxFilterDTO dto);
}
