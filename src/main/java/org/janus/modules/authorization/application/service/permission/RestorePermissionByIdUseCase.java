package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.infrastructure.in.permission.IRestorePermissionByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class RestorePermissionByIdUseCase implements IRestorePermissionByIdUseCase {

    private final PermissionRepository repository;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id) {
        if (id == null) return Result.badRequest("Permission id should be defined");

        int updatedRows = repository.restoreById(id);

        if (updatedRows == 0)
            return Result.notFound("Permission not found or not deleted with ID: " + id);

        return Result.ok();
    }
}
