package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.IDeleteRoleByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class DeleteRoleByIdUseCase implements IDeleteRoleByIdUseCase {

    private final RoleRepository repository;

    @Override
    @ResultTransaction
    public Result<Void> execute(UUID id) {
        RoleEntity role = repository.findById(id).orElse(null);

        if (role == null) {
            return Result.notFound("Role not found");
        }

        if (role.getIsSystem()) {
            return Result.failure("This role is of system", 403);
        }

        int deleted = repository.deleteById(id);

        if (deleted <= 0) {
            return Result.notFound("Role not found");
        }

        if (deleted > 1) {
            return Result.notFound("More of one role deleted");
        }

        return Result.success();
    }

}
