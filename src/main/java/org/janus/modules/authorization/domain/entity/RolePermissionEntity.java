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
}