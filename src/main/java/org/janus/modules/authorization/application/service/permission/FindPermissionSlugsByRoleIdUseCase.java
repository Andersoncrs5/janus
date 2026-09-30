package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.modules.authorization.infrastructure.in.permission.IFindPermissionSlugsByRoleIdUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;

import java.util.List;
import java.util.UUID;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class FindPermissionSlugsByRoleIdUseCase implements IFindPermissionSlugsByRoleIdUseCase {

    private final PermissionRepository permissionRepository;

    @Override
    public Result<List<String>> execute(UUID roleId) {
        if (roleId == null) {
            return Result.ok(List.of());

        }

        List<String> permissions = permissionRepository.findPermissionSlugsByRoleId(roleId);
        return Result.success(permissions);
    }

    @Override
    public Result<List<String>> execute(List<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Result.ok(List.of());

        }

        List<String> permissions = permissionRepository.findPermissionSlugsByRoleIds(roleIds);
        return Result.success(permissions);
    }
}