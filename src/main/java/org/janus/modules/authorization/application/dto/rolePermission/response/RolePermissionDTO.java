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

@Getter
@Setter
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
}