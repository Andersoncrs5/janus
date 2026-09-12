package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.modules.authorization.application.dto.permission.filter.PermissionFilterDTO;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.shared.domain.page.Page;

public interface IFindAllPermissionUseCase {
    Page<PermissionEntity> execute(PermissionFilterDTO dto);
}
