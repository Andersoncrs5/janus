package org.janus.modules.authorization.application.dto.userRole.request;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRoleDTO {
    private UUID userId;
    private UUID roleId;
    private @Nullable UUID assignedById;
    private @Nullable OffsetDateTime expiresAt;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public @Nullable UUID getAssignedById() {
        return assignedById;
    }

    public void setAssignedById(@Nullable UUID assignedById) {
        this.assignedById = assignedById;
    }

    public @Nullable OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(@Nullable OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}