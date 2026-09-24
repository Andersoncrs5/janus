package org.janus.modules.authorization.application.dto.rolePermission.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.dto.BaseDTO;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionDTO extends BaseDTO {

    private UUID roleId;
    private UUID permissionId;
    private PermissionEffectEnum effect;
    private String conditions;
    private OffsetDateTime expiresAt;
    private UUID assignedBy;
    private OffsetDateTime assignedAt;

    public UUID getRoleId() {
        return roleId;
    }

    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }

    public PermissionEffectEnum getEffect() {
        return effect;
    }

    public void setEffect(PermissionEffectEnum effect) {
        this.effect = effect;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(UUID assignedBy) {
        this.assignedBy = assignedBy;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}