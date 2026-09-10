package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.infrastructure.in.permission.IExistsPermissionByNameUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsPermissionByNameUseCase implements IExistsPermissionByNameUseCase {

    private final PermissionRepository repository;

    @Override
    public Result<Boolean> execute(String name) {
        if (name == null || name.isBlank()) {
            return Result.badRequest("Permission name should be defined");
        }

        boolean exists = repository.existsByName(name);
        return Result.success(exists);
    }
}