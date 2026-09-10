package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.in.permission.IDeletePermissionByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class DeletePermissionByIdUseCase implements IDeletePermissionByIdUseCase {

    private final PermissionRepository repository;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id) {
        if (id == null) {
            return Result.badRequest("Permission id should be defined");
        }

        PermissionEntity permission = repository.findById(id).orElse(null);

        if (permission == null) {
            return Result.notFound("Permission not found");
        }

        if (Boolean.TRUE.equals(permission.getIsSystem())) {
            return Result.badRequest("System permissions cannot be deleted");
        }

        int deletedCount = repository.deleteById(id);

        if (deletedCount <= 0) {
            return Result.notFound("Permission not found during deletion");
        }

        return Result.success();
    }
}