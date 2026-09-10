package org.janus.modules.authentication.port.in.session;

import org.janus.modules.authentication.application.dto.session.filter.SessionFilterDTO;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.shared.domain.page.Page;

public interface IFindAllSessionUseCase {
    Page<SessionEntity> execute(SessionFilterDTO dto);
}
