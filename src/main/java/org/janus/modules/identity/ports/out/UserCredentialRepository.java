package org.janus.modules.identity.ports.out;

import org.janus.modules.identity.domain.entity.UserCredentialsEntity;
import org.janus.shared.domain.base.repository.GenericRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository extends GenericRepository<UserCredentialsEntity, UUID> {
    Optional<UserCredentialsEntity> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
    boolean deleteByUserId(UUID userId);
}
