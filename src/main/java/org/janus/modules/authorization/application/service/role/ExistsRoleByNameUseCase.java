package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.infrastructure.in.role.IExistsRoleByNameUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.result.Result;

@ApplicationScoped
@RequiredArgsConstructor
public class ExistsRoleByNameUseCase implements IExistsRoleByNameUseCase {

    private final RoleRepository repository;

    public Result<Boolean> execute(String name) {
        return Result.success(repository.existsByName(name));
    }

}
