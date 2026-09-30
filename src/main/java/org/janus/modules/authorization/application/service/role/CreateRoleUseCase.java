package org.janus.modules.authorization.application.service.role;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.role.request.CreateRoleDTO;
import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.mapper.RoleMapper;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.ICreateRoleUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.ICreateUserRoleUseCase;
import org.janus.modules.authorization.infrastructure.out.RoleRepository;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.user.IFindUserByEmailUseCase;
import org.janus.shared.domain.base.service.BaseUseCase;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;
import org.janus.shared.infrastructure.properties.BootstrapProperties;

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
    @Inject
    private IFindUserByEmailUseCase findUserByEmail;
    @Inject
    private ICreateUserRoleUseCase createUserRole;
    @Inject
    private BootstrapProperties properties;

    @Override
    @ResultTransaction
    public Result<RoleEntity> execute(CreateRoleDTO dto) {
        return executeSafely(() -> {
            RoleEntity role = mapper.toEntity(dto);
            RoleEntity saved = repository.insert(role);
            return Result.created(saved);
        });
    }

    private void linkNewRoleToMaster(
            RoleEntity role
    ) {
        Result<UserEntity> userResult =
                findUserByEmail.execute(properties.master().email());

        if (userResult.isFailure()) {
            throw new InternalServerErrorException(
                    "Failed to find MASTER user"
            );
        }

        UserEntity master = userResult.getData();

        CreateUserRoleDTO dto = new CreateUserRoleDTO();
        dto.setUserId(master.getId());
        dto.setRoleId(role.getId());
        dto.setAssignedById(master.getId());
        dto.setExpiresAt(null);

        Result<UserRoleEntity> execute = createUserRole.execute(dto, master.getId());

        if (execute.isFailure()) {
            throw new InternalServerErrorException(
                    "Failed to link Role to MASTER role: "
                            + execute.getFirstError()
            );
        }
    }

    @Override
    protected Map<String, Supplier<Result<RoleEntity>>> getConstraintHandlers() {
        return Map.of(
                "uk_roles_slug", () -> Result.conflict("Role already exists with this slug"),
                "uk_roles_name", () -> Result.conflict("Role already exists with this name"),
                "ck_roles_name_not_empty", () -> Result.badRequest("Role name cannot be empty"),
                "ck_roles_version", () -> Result.badRequest("Invalid record version")
        );
    }

}