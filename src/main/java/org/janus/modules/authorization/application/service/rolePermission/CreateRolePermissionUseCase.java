package org.janus.modules.authorization.application.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.rolePermission.request.CreateRolePermissionDTO;
import org.janus.modules.authorization.application.mapper.RolePermissionMapper;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.in.rolePermission.ICreateRolePermissionUseCase;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class CreateRolePermissionUseCase implements ICreateRolePermissionUseCase {

    private final RolePermissionRepository repository;
    private final RolePermissionMapper mapper;

    @Override
    public Result<RolePermissionEntity> execute(CreateRolePermissionDTO dto, UUID assignedBy) {
        if (dto == null) {
            return Result.badRequest("CreateRolePermissionDTO must not be null");
        }

        RolePermissionEntity entity = mapper.toEntity(dto);
        entity.setAssignedBy(assignedBy);

        try {
            RolePermissionEntity inserted = repository.insert(entity);
            return Result.created(inserted);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            String lowerMessage = message.toLowerCase();

            if (lowerMessage.contains("uk_role_permissions_active")) {
                return Result.failure(
                        "Permission is already assigned to this role",
                        409
                );
            }

            if (lowerMessage.contains("fk_role_permissions_role")) {
                return Result.notFound(
                        "Role not found with ID: " + dto.roleId()
                );
            }

            if (lowerMessage.contains("fk_role_permissions_permission")) {
                return Result.notFound(
                        "Permission not found with ID: " + dto.permissionId()
                );
            }

            if (lowerMessage.contains("fk_role_permissions_assigned_by")) {
                return Result.badRequest(
                        "User assigned by not found with ID: " + assignedBy
                );
            }

            return DatabaseConstraintHandler.handle(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}