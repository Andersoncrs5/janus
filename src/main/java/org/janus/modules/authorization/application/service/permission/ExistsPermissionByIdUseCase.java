package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.infrastructure.in.permission.IExistsPermissionByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsPermissionByIdUseCase implements IExistsPermissionByIdUseCase {

    private final PermissionRepository repository;

    @Override
    @ResultTransaction
    public Result<Boolean> execute(UUID id) {
        if (id == null)
            return Result.badRequest("Permission id should be defined");

        boolean exists = repository.existsById(id);

        return Result.ok(exists);
    }
}