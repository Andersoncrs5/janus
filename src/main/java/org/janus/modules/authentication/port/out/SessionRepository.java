package org.janus.modules.authentication.port.out;

import org.janus.modules.authentication.domain.entity.SessionEntity;
import org.janus.shared.domain.base.repository.GenericRepository;

import java.util.List;
import java.util.UUID;

public interface SessionRepository extends GenericRepository<SessionEntity, UUID> {
    List<SessionEntity> findActiveByUserId(UUID userId);

    int revokeById(UUID id);

    int revokeAllByUserId(UUID userId);
}
