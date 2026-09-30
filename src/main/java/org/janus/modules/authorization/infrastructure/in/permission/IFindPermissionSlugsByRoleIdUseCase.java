package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

public interface IFindPermissionSlugsByRoleIdUseCase {
    Result<List<String>> execute(UUID roleId);

    Result<List<String>> execute(List<UUID> roleIds);
}
