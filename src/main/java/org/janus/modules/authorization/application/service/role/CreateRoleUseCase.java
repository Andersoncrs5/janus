package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.application.mapper.RoleMapper;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.ICreateRoleUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.base.service.BaseUseCase;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.Map;
import java.util.function.Supplier;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateRoleUseCase
        extends BaseUseCase<RoleEntity>
        implements ICreateRoleUseCase {

    @Inject
    private RoleRepository repository;
    @Inject
    private RoleMapper mapper;

    @Override
    protected Map<String, Supplier<Result<RoleEntity>>> getConstraintHandlers() {
        return Map.of(
                "uk_roles_slug", () -> Result.conflict("Role already exists with this slug"),
                "uk_roles_name", () -> Result.conflict("Role already exists with this name"),
                "ck_roles_name_not_empty", () -> Result.badRequest("Role name cannot be empty"),
                "ck_roles_version", () -> Result.badRequest("Invalid record version")
        );
    }

    @Override
    @ResultTransaction
    public Result<RoleEntity> execute(CreateRoleDTO dto) {
        return executeSafely(() -> {
            RoleEntity role = mapper.toEntity(dto);
            RoleEntity saved = repository.insert(role);
            return Result.created(saved);
        });
    }
}