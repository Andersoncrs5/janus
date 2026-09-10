package org.janus.modules.authorization.application.dto.rolePermission.request;


import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;

import java.time.OffsetDateTime;

public record UpdateRolePermissionDTO(
        PermissionEffectEnum effect,
        String conditions,
        OffsetDateTime expiresAt
) {
}