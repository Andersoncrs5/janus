package org.janus.modules.authentication.port.out;

import org.janus.modules.authentication.domain.entity.RefreshTokenEntity;
import org.janus.shared.domain.base.repository.GenericRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends GenericRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    int revokeById(UUID id);
    int revokeAllBySessionId(UUID sessionId);
    int revokeAllByUserId(UUID userId);
    int revokeFamilyBySessionId(UUID sessionId);

    boolean existsActiveBySessionId(UUID sessionId);

    Optional<RefreshTokenEntity> findLatestBySessionId(UUID sessionId);
}
