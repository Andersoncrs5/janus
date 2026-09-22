package org.janus.modules.authorization.application.service.permission;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.janus.modules.authorization.application.dto.permission.request.CreatePermissionDTO;
import org.janus.modules.authorization.application.mapper.PermissionMapper;
import org.janus.modules.authorization.domain.entity.PermissionEntity;
import org.janus.modules.authorization.infrastructure.in.permission.ICreatePermissionUseCase;
import org.janus.modules.authorization.infrastructure.out.PermissionRepository;
import org.janus.shared.domain.base.service.BaseUseCase;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.persistence.tx.ResultTransaction;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@ApplicationScoped
@RequiredArgsConstructor
public class CreatePermissionUseCase
        extends BaseUseCase<PermissionEntity>
        implements ICreatePermissionUseCase {

    @Inject
    private PermissionRepository repository;

    @Inject
    private PermissionMapper mapper;

    @Override
    protected Map<String, Supplier<Result<PermissionEntity>>> getConstraintHandlers() {
        return Map.ofEntries(
                Map.entry("uk_permissions_slug", () -> Result.conflict("Permission already exists with this slug")),
                Map.entry("uk_permissions_slug_active", () -> Result.conflict("Permission already exists with this slug")),
                Map.entry("uk_permissions_name", () -> Result.conflict("Permission already exists with this name")),
                Map.entry("uk_permissions_name_active", () -> Result.conflict("Permission already exists with this name")),
                Map.entry("uk_permissions_module_resource_action_active", () -> Result.conflict("Permission already exists for this module, resource, and action combination")),

                Map.entry("ck_permissions_slug_not_empty", () -> Result.badRequest("Permission slug cannot be empty or blank")),
                Map.entry("ck_permissions_name_not_empty", () -> Result.badRequest("Permission name cannot be empty or blank")),
                Map.entry("ck_permissions_action_not_empty", () -> Result.badRequest("Permission action cannot be empty or blank")),
                Map.entry("ck_permissions_slug_format", () -> Result.badRequest("Permission slug contains invalid format")),
                Map.entry("ck_permissions_action_format", () -> Result.badRequest("Permission action contains invalid format")),
                Map.entry("ck_permissions_version", () -> Result.badRequest("Invalid record version")),
                Map.entry("ck_permissions_metadata_is_object", () -> Result.badRequest("Metadata must be a valid JSON object")),

                Map.entry("fk_permissions_created_by", () -> Result.badRequest("User specified in 'createdBy' does not exist"))
        );
    }

    @Override
    @ResultTransaction
    public Result<PermissionEntity> execute(CreatePermissionDTO dto, UUID createdBy) {
        if (dto == null) {
            return Result.badRequest("Permission data must be provided");
        }

        return executeSafely(() -> {
            PermissionEntity permission = mapper.toEntity(dto);
            permission.setCreatedBy(createdBy);

            PermissionEntity inserted = repository.insert(permission);
            return Result.created(inserted);
        });
    }
}