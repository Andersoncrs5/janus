package org.janus.modules.authorization.infrastructure.in.rolePermission;

import org.janus.modules.authorization.application.dto.rolePermission.filter.RolePermissionFilterDTO;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.result.Result;

public interface IFindAllRolePermissionsUseCase {
    Result<Page<RolePermissionEntity>> execute(RolePermissionFilterDTO filter);
}
