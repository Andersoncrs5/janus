package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.infrastructure.in.role.IExistsRoleByNameUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsRoleByNameUseCase implements IExistsRoleByNameUseCase {
    private final RoleRepository repository;

    @Override
    @ResultTransaction
    public Result<Boolean> execute(String name) {
        if (name == null || name.isBlank())
            return Result.badRequest("Name is required");

        return Result.ok(repository.existsByName(name));
    }

}
