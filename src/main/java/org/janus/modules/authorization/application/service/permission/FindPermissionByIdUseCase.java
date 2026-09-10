package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.in.permission.IFindPermissionByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindPermissionByIdUseCase implements IFindPermissionByIdUseCase {

    private final PermissionRepository repository;

    @Override
    public Result<PermissionEntity> execute(UUID id) {
        if (id == null) return Result.badRequest("Permission id should be defined");

        PermissionEntity permission = repository.findById(id).orElse(null);

        if (permission == null) {
            return Result.notFound("Permission not found");
        }

        return Result.success(permission);
    }
}
