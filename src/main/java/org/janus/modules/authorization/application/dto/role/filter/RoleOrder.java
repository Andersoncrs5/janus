package org.janus.modules.authorization.application.dto.role.filter;

import lombok.Getter;

@Getter
public enum RoleOrder {
    ID("id"),
    NAME("name"),
    IS_ACTIVE("is_active"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at");

    private final String field;

    RoleOrder(String field) {
        this.field = field;
    }
}