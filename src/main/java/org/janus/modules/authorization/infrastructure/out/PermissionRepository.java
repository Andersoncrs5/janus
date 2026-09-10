package org.janus.modules.authorization.infrastructure.out;

import org.janus.modules.authorization.application.dto.permission.filter.PermissionFilterDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.shared.domain.base.repository.GenericRepository;
import org.janus.shared.domain.page.Page;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends GenericRepository<PermissionEntity, UUID> {
    Page<PermissionEntity> findAll(PermissionFilterDTO filter);

    Optional<PermissionEntity> findByName(String name);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);
}
