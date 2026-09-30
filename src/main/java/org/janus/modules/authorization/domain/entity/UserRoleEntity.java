package org.janus.modules.authorization.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleEntity extends BaseEntity {
    public static final String TABLE_NAME = "user_roles";

    private UUID userId;

    private UUID roleId;

    @Nullable
    private OffsetDateTime expiresAt;

    @Nullable
    private UUID assignedById;

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

    public @Nullable OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(@Nullable OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public @Nullable UUID getAssignedById() {
        return assignedById;
    }

    public void setAssignedById(@Nullable UUID assignedById) {
        this.assignedById = assignedById;
    }
}