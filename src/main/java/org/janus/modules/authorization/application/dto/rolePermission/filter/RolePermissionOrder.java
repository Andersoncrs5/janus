package org.janus.modules.authorization.application.dto.rolePermission.filter;

import lombok.Getter;

@Getter
public enum RolePermissionOrder {
    ID("id", "ASC"),
    ROLE_ID("role_id", "ASC"),
    PERMISSION_ID("permission_id", "ASC"),
    EFFECT("effect", "ASC"),
    EXPIRES_AT("expires_at", "DESC"),
    ASSIGNED_AT("assigned_at", "DESC"),
    VERSION("version", "ASC"),
    CREATED_AT("created_at", "DESC"),
    UPDATED_AT("updated_at", "DESC");

    private final String column;
    private final String direction;

    RolePermissionOrder(String column, String direction) {
        this.column = column;
        this.direction = direction;
    }
}