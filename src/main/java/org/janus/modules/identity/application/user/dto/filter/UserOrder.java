package org.janus.modules.identity.application.user.dto.filter;

import java.util.Arrays;

public enum UserOrder {
    ID("id", "ASC"),
    USERNAME("username", "ASC"),
    EMAIL("email", "ASC"),
    FULL_NAME("full_name", "ASC"),
    VERSION("version", "ASC"),
    CREATED_AT("created_at", "DESC"),
    UPDATED_AT("updated_at", "DESC"),
    DELETED_AT("deleted_at", "DESC");

    private final String column;
    private final String direction;

    UserOrder(String column, String direction) {
        this.column = column;
        this.direction = direction;
    }

    public static UserOrder fromColumn(String column) {
        if (column == null || column.isBlank()) {
            return CREATED_AT;
        }

        return Arrays.stream(values())
                .filter(order -> order.column.equalsIgnoreCase(column) || order.name().equalsIgnoreCase(column))
                .findFirst()
                .orElse(CREATED_AT);
    }

    public String getColumn() {
        return column;
    }

    public String getDirection() {
        return direction;
    }
}