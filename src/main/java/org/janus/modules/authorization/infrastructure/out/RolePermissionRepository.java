package org.janus.modules.authorization.infrastructure.out;

import org.janus.modules.authorization.application.dto.rolePermission.filter.RolePermissionFilterDTO;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.shared.domain.base.repository.GenericRepository;
import org.janus.shared.domain.page.Page;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePermissionRepository extends GenericRepository<RolePermissionEntity, UUID> {
    Page<RolePermissionEntity> findAll(RolePermissionFilterDTO filter);

    Optional<RolePermissionEntity> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    List<RolePermissionEntity> findAllByRoleId(UUID roleId);

    List<RolePermissionEntity> findAllByPermissionId(UUID permissionId);

    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    List<RolePermissionEntity> findAllActiveByRoleId(UUID roleId, OffsetDateTime now);

    long countByRoleId(UUID roleId);
}
