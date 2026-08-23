package org.janus.modules.reliability.application.service.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.reliability.application.dto.inbox.filter.InboxFilterDTO;
import org.janus.modules.reliability.domain.entity.InboxEntity;
import org.janus.modules.reliability.port.in.inbox.IFindAllInboxUseCase;
import org.janus.modules.reliability.port.out.InboxRepository;
import org.janus.shared.domain.page.Page;

@ApplicationScoped
@RequiredArgsConstructor
public class FindAllInboxUseCase implements IFindAllInboxUseCase {
    private final InboxRepository repository;

    public Page<InboxEntity> execute(InboxFilterDTO dto) {
        return repository.findAll(dto);
    }

}
