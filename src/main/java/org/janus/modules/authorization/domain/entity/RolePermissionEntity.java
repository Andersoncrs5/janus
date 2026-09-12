package org.janus.modules.authorization.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionEntity extends BaseEntity {

    public static final String TABLE_NAME = "role_permissions";

    private UUID roleId;
    private UUID permissionId;

    @Builder.Default
    private PermissionEffectEnum effect = PermissionEffectEnum.ALLOW;

    private String conditions;
    private OffsetDateTime expiresAt;
    private UUID assignedBy;

    @Builder.Default
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isAllowed() {
        return PermissionEffectEnum.ALLOW.equals(effect) && !isExpired() && !isDeleted();
    }

    public boolean isDenied() {
        return PermissionEffectEnum.DENY.equals(effect) && !isDeleted();
    }

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