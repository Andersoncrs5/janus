package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.in.permission.IFindPermissionByNameUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;

import java.util.Optional;

@ApplicationScoped
@RequiredArgsConstructor
public class FindPermissionByNameUseCase implements IFindPermissionByNameUseCase {

    private final PermissionRepository repository;

    @Override
    public Result<PermissionEntity> execute(String name) {
        if (name == null || name.trim().isEmpty())
            return Result.failure("Permission name cannot be null or empty", 400);

        Optional<PermissionEntity> permissionOpt = repository.findByName(name);

        return permissionOpt.map(Result::success).orElseGet(() -> Result.failure("Permission with name '" + name + "' not found", 404));

    }
}