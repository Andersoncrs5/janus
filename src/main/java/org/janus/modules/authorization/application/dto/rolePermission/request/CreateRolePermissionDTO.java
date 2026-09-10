package org.janus.modules.authorization.application.dto.rolePermission.request;

import jakarta.validation.constraints.NotNull;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateRolePermissionDTO(
        @NotNull(message = "Role ID is required")
        UUID roleId,

        @NotNull(message = "Permission ID is required")
        UUID permissionId,

        PermissionEffectEnum effect,

        String conditions,

        OffsetDateTime expiresAt,

        UUID assignedBy
) {
}