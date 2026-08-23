package org.janus.modules.authorization.infrastructure.in.role;

import org.janus.modules.authorization.application.dto.role.filter.RoleFilterDTO;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.shared.domain.page.Page;

public interface IFindAllRoleUseCase {
    Page<RoleEntity> execute(RoleFilterDTO dto);
}
