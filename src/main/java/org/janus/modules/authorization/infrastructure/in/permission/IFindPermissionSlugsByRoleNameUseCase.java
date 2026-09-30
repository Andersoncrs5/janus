package org.janus.modules.authorization.infrastructure.in.permission;

import org.janus.shared.domain.result.Result;

import java.util.List;

public interface IFindPermissionSlugsByRoleNameUseCase {
    Result<List<String>> execute(String roleName);

    Result<List<String>> execute(List<String> roleNames);
}