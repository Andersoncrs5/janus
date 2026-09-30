package org.janus.modules.authorization.application.service.userRole;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.userRole.request.CreateUserRoleDTO;
import org.janus.modules.authorization.application.mapper.UserRoleMapper;
import org.janus.modules.authorization.domain.entity.RoleEntity;
import org.janus.modules.authorization.domain.entity.UserRoleEntity;
import org.janus.modules.authorization.infrastructure.in.role.IFindRoleByIdUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.ICreateUserRoleUseCase;
import org.janus.modules.authorization.infrastructure.in.userRole.IFindRolesOnlyNameByUserIdUseCase;
import org.janus.modules.authorization.infrastructure.out.UserRoleRepository;
import org.janus.shared.domain.base.service.BaseUseCase;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

@ApplicationScoped
public class CreateUserRoleUseCase
        extends BaseUseCase<UserRoleEntity>
        implements ICreateUserRoleUseCase {

    @Inject
    private UserRoleRepository repository;
    @Inject
    private UserRoleMapper mapper;
    @Inject
    private IFindRoleByIdUseCase findRoleById;
    @Inject
    private IFindRolesOnlyNameByUserIdUseCase findRolesOnlyNameByUserId;

    @Override
    @ResultTransaction
    public Result<UserRoleEntity> execute(
            CreateUserRoleDTO dto,
            UUID assignId
    ) {
        if (dto == null) {
            return Result.badRequest("CreateUserRoleDTO cannot be null");
        }

        if (dto.getUserId() == null || dto.getRoleId() == null || assignId == null) {

            return Result.badRequest(
                    "User, role, and operator IDs are required"
            );
        }

        if (Objects.equals(dto.getUserId(), assignId)) {
            return Result.failure(
                    "Cannot assign roles to yourself",
                    403
            );
        }

        Result<RoleEntity> roleResult =
                findRoleById.execute(dto.getRoleId());

        if (roleResult.isFailure()) {
            return Result.failure(
                    roleResult.getFirstError(),
                    roleResult.getStatus()
            );
        }

        RoleEntity role = roleResult.getData();

        if (Boolean.FALSE.equals(role.getActive())) {
            return Result.failure(
                    "The requested role is inactive",
                    400
            );
        }

        if ("MASTER".equalsIgnoreCase(role.getName())) {
            return Result.failure(
                    "Assigning the MASTER role is not allowed",
                    403
            );
        }

        Result<List<String>> operatorRolesResult =
                findRolesOnlyNameByUserId.execute(assignId);

        if (operatorRolesResult.isFailure()) {
            return Result.failure(
                    operatorRolesResult.getFirstError(),
                    operatorRolesResult.getStatus()
            );
        }

        List<String> operatorRoles = operatorRolesResult.getData();

        boolean isOperatorMaster = operatorRoles.stream()
                .anyMatch("MASTER"::equalsIgnoreCase);

        if (role.nameEquals(List.of("ADMINISTRADOR"))
                && !isOperatorMaster) {

            return Result.failure(
                    "Only MASTER users can assign the ADMINISTRADOR role",
                    403
            );
        }

        UserRoleEntity entity = mapper.toEntity(dto);
        entity.setAssignedById(assignId);

        return executeSafely(() -> {
            UserRoleEntity inserted = repository.insert(entity);
            return Result.created(inserted);
        });
    }

    @Override
    protected Map<String, Supplier<Result<UserRoleEntity>>>
    getConstraintHandlers() {

        return Map.ofEntries(

                Map.entry(
                        "uk_user_role",
                        () -> Result.failure(
                                "The user already has this role assigned",
                                409
                        )
                ),

                Map.entry(
                        "fk_user_roles_user",
                        () -> Result.notFound(
                                "User not found with ID: '"
                                        + currentUserId
                                        + "'"
                        )
                ),

                Map.entry(
                        "fk_user_roles_role",
                        () -> Result.notFound(
                                "Role not found with ID: '"
                                        + currentRoleId
                                        + "'"
                        )
                ),

                Map.entry(
                        "fk_user_roles_assigned_by",
                        () -> Result.notFound(
                                "Assigned-by user not found with ID: '"
                                        + currentAssignId
                                        + "'"
                        )
                )
        );
    }

    private UUID currentUserId;
    private UUID currentRoleId;
    private UUID currentAssignId;
}