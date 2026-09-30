package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janus.modules.authorization.infrastructure.in.permission.IFindPermissionSlugsByRoleNameUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;

import java.util.List;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class FindPermissionSlugsByRoleNameUseCase implements IFindPermissionSlugsByRoleNameUseCase {

    private final PermissionRepository permissionRepository;

    @Override
    public Result<List<String>> execute(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return Result.ok(List.of());
        }

        List<String> permissions = permissionRepository.findPermissionSlugsByRoleName(roleName.trim());
        return Result.success(permissions);
    }

    @Override
    public Result<List<String>> execute(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Result.ok(List.of());
        }

        List<String> cleanRoleNames = roleNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .toList();

        if (cleanRoleNames.isEmpty()) {
            return Result.badRequest("A lista contendo nomes de papéis válidos não pode ser vazia.");
        }

        List<String> permissions = permissionRepository.findPermissionSlugsByRoleNames(cleanRoleNames);
        return Result.success(permissions);
    }
}