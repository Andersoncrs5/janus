package org.janus.modules.authorization.application.dto.permission.filter;

import lombok.Getter;

@Getter
public enum PermissionOrder {
    ID("id"),
    NAME("name"),
    SLUG("slug"),
    MODULE("module"),
    RESOURCE("resource"),
    RISK_LEVEL("risk_level"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at");

    private final String field;

    PermissionOrder(String field) {
        this.field = field;
    }
}