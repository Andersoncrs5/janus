package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.IFindRoleByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class FindRoleByIdUseCase implements IFindRoleByIdUseCase {

    private final RoleRepository repository;

    @Override
    @ResultTransaction
    public Result<RoleEntity> execute(UUID id) {
        RoleEntity role = repository.findById(id).orElse(null);

        if (role == null) {
            return Result.notFound("Role not found");
        }

        return Result.success(role);
    }

}
