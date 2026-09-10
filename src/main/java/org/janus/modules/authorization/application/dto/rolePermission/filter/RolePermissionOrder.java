package org.janus.modules.authorization.application.dto.rolePermission.filter;

import lombok.Getter;

@Getter
public enum RolePermissionOrder {
    ID("id"),
    ROLE_ID("role_id"),
    PERMISSION_ID("permission_id"),
    EFFECT("effect"),
    EXPIRES_AT("expires_at"),
    ASSIGNED_AT("assigned_at"),
    VERSION("version"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at");

    private final String field;

    RolePermissionOrder(String field) {
        this.field = field;
    }
}