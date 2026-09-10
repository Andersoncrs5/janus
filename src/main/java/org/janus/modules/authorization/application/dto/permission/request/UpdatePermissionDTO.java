package org.janus.modules.authorization.application.dto.permission.request;

import jakarta.validation.constraints.Size;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;

public record UpdatePermissionDTO(
        @Size(max = 100)
        String name,

        @Size(max = 150)
        String slug,

        String description,

        PermissionModule module,

        PermissionResource resource,

        @Size(max = 50)
        String action,

        PermissionRiskLevel riskLevel,

        Boolean isActive,

        Boolean isSystem,

        String metadata
) {
}