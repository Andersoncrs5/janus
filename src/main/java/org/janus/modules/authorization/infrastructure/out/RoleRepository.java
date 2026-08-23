package org.janus.modules.authorization.infrastructure.out;

import org.janus.modules.authorization.application.dto.role.filter.RoleFilterDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.base.repository.GenericRepository;
import org.janus.shared.domain.page.Page;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends GenericRepository<RoleEntity, UUID> {
    Page<RoleEntity> findAll(RoleFilterDTO filter);
    Optional<RoleEntity> findByName(String name);
    boolean existsByName(String name);
}
