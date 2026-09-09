package org.janus.modules.authentication.port.out;

import org.janus.modules.authentication.application.dto.session.filter.SessionFilterDTO;
import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.shared.domain.base.repository.GenericRepository;
import org.janus.shared.domain.page.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends GenericRepository<SessionEntity, UUID> {
    List<SessionEntity> findActiveByUserId(UUID userId);

    int revokeById(UUID id);

    int revokeAllByUserId(UUID userId);

    Optional<SessionEntity> findBySessionIdAndUserId(UUID sessionId, UUID userId);

    Page<SessionEntity> findAll(SessionFilterDTO dto);
}
