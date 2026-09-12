package org.janus.modules.authorization.application.dto.rolePermission.filter;

import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.filter.FilterBaseDTO;
import org.janus.shared.domain.enums.rolePermission.PermissionEffectEnum;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionFilterDTO extends FilterBaseDTO {

    private UUID roleId;
    private UUID permissionId;
    private Set<PermissionEffectEnum> effect;
    private UUID assignedBy;
    private Boolean isExpired;
    private OffsetDateTime expiresAtFrom;
    private OffsetDateTime expiresAtTo;

    @QueryParam("orders")
    private List<RolePermissionOrder> orders = List.of(RolePermissionOrder.CREATED_AT);

}