package org.janus.modules.authorization.application.service.rolePermission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.rolePermission.request.UpdateRolePermissionDTO;
import org.janus.modules.authorization.application.mapper.RolePermissionMapper;
import org.janus.modules.authorization.domain.entity.RolePermissionEntity;
import org.janus.modules.authorization.infrastructure.in.rolePermission.IUpdateRolePermissionUseCase;
import org.janus.modules.authorization.infrastructure.out.RolePermissionRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class UpdateRolePermissionUseCase implements IUpdateRolePermissionUseCase {

    private final RolePermissionRepository repository;
    private final RolePermissionMapper mapper;

    @Override
    public Result<RolePermissionEntity> execute(UpdateRolePermissionDTO dto, UUID id) {
        if (dto == null)
            return Result.badRequest("CreateRolePermissionDTO must not be null");

        if (id == null)
            return Result.badRequest("Role Permission Id should be defined");

        RolePermissionEntity entity = repository.findById(id).orElse(null);

        if (entity == null)
            return Result.badRequest("Role Permission not found");

        mapper.updateEntityFromDto(dto, entity);

        try {
            RolePermissionEntity saved = repository.save(entity);

            return Result.created(saved);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }
            
            return DatabaseConstraintHandler.handle(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}