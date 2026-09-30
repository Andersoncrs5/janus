package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.role.request.UpdateRoleDTO;
import org.janus.modules.authorization.application.mapper.RoleMapper;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.IUpdateRoleUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.shared.domain.base.service.BaseUseCase;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@ApplicationScoped
@RequiredArgsConstructor
public class UpdateRoleUseCase
        extends BaseUseCase<RoleEntity>
        implements IUpdateRoleUseCase {

    private final RoleRepository repository;
    private final RoleMapper mapper;

    @Override
    @ResultTransaction
    public Result<RoleEntity> execute(
            UUID id,
            UpdateRoleDTO dto
    ) {
        return executeSafely(() -> {

            if (id == null) {
                return Result.badRequest("Role ID is required");
            }

            if (dto == null) {
                return Result.badRequest("Role data is required");
            }

            RoleEntity role = repository
                    .findById(id)
                    .orElse(null);

            if (role == null) {
                return Result.notFound("Role not found");
            }

            mapper.updateEntityFromDto(dto, role);

            RoleEntity saved = repository.save(role);

            return Result.ok(saved);
        });
    }

    @Override
    protected Map<String, Supplier<Result<RoleEntity>>> getConstraintHandlers() {
        return Map.of(
                "uk_roles_slug",
                () -> Result.conflict(
                        "Role already exists with slug: '"
                                + "slug"
                                + "'"
                ),

                "uk_roles_name",
                () -> Result.conflict(
                        "Role already exists with this name"
                ),

                "ck_roles_name_not_empty",
                () -> Result.badRequest(
                        "Role name cannot be empty"
                ),

                "ck_roles_version",
                () -> Result.conflict(
                        "Invalid role version"
                )
        );
    }
}
