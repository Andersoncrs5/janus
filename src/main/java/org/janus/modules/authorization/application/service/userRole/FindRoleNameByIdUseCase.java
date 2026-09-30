package org.janus.modules.authorization.application.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRoleNameByIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.result.Result;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FindRoleNameByIdUseCase implements IFindRoleNameByIdUseCase {

    @Inject
    private UserRoleRepository repository;

    @Override
    public Result<String> execute(UUID id) {
        Optional<String> roleName = repository.findRoleNameById(id);

        return roleName.map(Result::ok)
                .orElseGet(() -> Result.notFound("Role name not found"));

    }
}
