package org.janus.modules.identity.ports.out;

import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.base.repository.GenericRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends GenericRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean updateOptimistic(UserEntity entity);
}
