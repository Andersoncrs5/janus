package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.application.mapper.PermissionMapper;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.in.permission.ICreatePermissionUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.database.DatabaseConstraintHandler;
import org.janus.shared.domain.exception.DataIntegrityViolationException;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.UUID;

@ApplicationScoped
@RequiredArgsConstructor
public class CreatePermissionUseCase implements ICreatePermissionUseCase {

    private final PermissionRepository repository;
    private final PermissionMapper mapper;

    @Override
    @ResultTransaction
    public Result<PermissionEntity> execute(CreatePermissionDTO dto, UUID createdBy) {
        if (dto == null)
            return Result.badRequest("Permission data must be provided");

        PermissionEntity permission = mapper.toEntity(dto);
        permission.setCreatedBy(createdBy);

        try {
            PermissionEntity inserted = repository.insert(permission);
            return Result.created(inserted);
        } catch (DataIntegrityViolationException e) {
            String message = e.getMessage();

            if (message == null) {
                return DatabaseConstraintHandler.handle(e);
            }

            String lowerMessage = message.toLowerCase();

            if (lowerMessage.contains("uk_permissions_slug")) {
                return Result.failure(
                        "Permission already exists with slug: '" + dto.slug() + "'",
                        409
                );
            }

            if (lowerMessage.contains("uk_permissions_name")) {
                return Result.failure(
                        "Permission already exists with name: '" + dto.name() + "'",
                        409
                );
            }

            if (lowerMessage.contains("ck_permissions_slug_not_empty")) {
                return Result.badRequest("Permission slug cannot be empty or blank");
            }

            if (lowerMessage.contains("fk_permissions_created_by")) {
                return Result.badRequest("User specified in 'createdBy' does not exist");
            }

            return DatabaseConstraintHandler.handle(e);
        } catch (Exception e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }
}