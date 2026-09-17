package org.janus.modules.identity.ports.out;

import org.janus.modules.identity.application.user.dto.filter.UserFilterDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.base.repository.GenericRepository;
import org.janus.shared.domain.page.Page;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends GenericRepository<UserEntity, UUID> {
    Page<UserEntity> findAll(UserFilterDTO dto);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean updateOptimistic(UserEntity entity);
}
