package org.janus.modules.authorization.application.dto.role.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoleDTO(
        @NotBlank(message = "Role name cannot be empty")
        @Size(max = 100, message = "Role name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        Boolean isActive,

        String slug
) {
}