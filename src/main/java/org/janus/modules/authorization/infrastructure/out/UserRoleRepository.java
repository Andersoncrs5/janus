package org.janus.modules.authorization.infrastructure.out;

import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.shared.domain.base.repository.GenericRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends GenericRepository<UserRoleEntity, UUID> {
    List<String> findRolesOnlyNameByUserId(UUID userId);

    List<RoleEntity> findRolesByUserId(UUID userId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    List<UUID> findAllRoleIdsByUserId(UUID id);

    int deleteByUserIdAndRoleId(
            UUID userId,
            UUID roleId
    );

    int deleteAllByUserId(
            UUID userId
    );

    boolean existsActiveByUserIdAndRoleId(
            UUID userId,
            UUID roleId
    );

    Optional<UserRoleEntity> findByUserIdAndRoleId(
            UUID userId,
            UUID roleId
    );
}
