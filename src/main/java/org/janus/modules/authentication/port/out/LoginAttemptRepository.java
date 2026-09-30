package org.janus.modules.authentication.port.out;

import org.janus.modules.authentication.domain.entity.LoginAttemptEntity;
import org.janus.shared.domain.base.repository.GenericRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LoginAttemptRepository
        extends GenericRepository<LoginAttemptEntity, UUID> {

    long countFailedAttemptsByUserId(
            UUID userId,
            OffsetDateTime since
    );

    long countFailedAttemptsByIpAddress(
            String ipAddress,
            OffsetDateTime since
    );

    long countFailedAttemptsByEmail(
            String email,
            OffsetDateTime since
    );

    List<LoginAttemptEntity> findRecentByUserId(
            UUID userId,
            int limit
    );

    List<LoginAttemptEntity> findRecentByIpAddress(
            String ipAddress,
            int limit
    );

    List<LoginAttemptEntity> findRecentByEmail(
            String email,
            int limit
    );

    boolean existsSuccessfulAttemptByUserIdSince(
            UUID userId,
            OffsetDateTime since
    );
}