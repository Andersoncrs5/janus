package org.janus.modules.identity.ports.in.user;

import org.janus.modules.identity.application.user.dto.filter.UserFilterDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.shared.domain.page.Page;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.security.abac.PermissionsAllowed;

public interface IFindAllUsersUseCase {
    @PermissionsAllowed("users:read")
    Result<Page<UserEntity>> execute(UserFilterDTO filter);
}
