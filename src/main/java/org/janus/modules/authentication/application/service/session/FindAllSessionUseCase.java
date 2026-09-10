package org.janus.modules.authentication.application.service.session;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authentication.application.dto.session.filter.SessionFilterDTO;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.modules.authentication.port.in.session.IFindAllSessionUseCase;
import org.janus.modules.authentication.port.out.SessionRepository;
import org.janus.shared.domain.page.Page;

@ApplicationScoped
@RequiredArgsConstructor
public class FindAllSessionUseCase implements IFindAllSessionUseCase {
    private final SessionRepository repository;

    @Override
    public Page<SessionEntity> execute(SessionFilterDTO dto) {
        return repository.findAll(dto);
    }
}
