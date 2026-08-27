package org.janus.modules.authorization.application.dto.userRole.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRoleDTO {
    private UUID userId;
    private UUID roleId;
    private @Nullable UUID assignedById;
    private @Nullable OffsetDateTime expiresAt;

    public CreateUserRoleDTO(UUID userId, UUID roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }
}