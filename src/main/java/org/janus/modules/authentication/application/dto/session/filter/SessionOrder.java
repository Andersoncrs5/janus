package org.janus.modules.authentication.application.dto.session.filter;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SessionOrder {
    ID("id"),
    USER_ID("user_id"),
    IP_ADDRESS("ip_address"),
    USER_AGENT("user_agent"),
    IS_REVOKED("is_revoked"),
    EXPIRES_AT("expires_at"),
    VERSION("version"),
    CREATED_AT("created_at"),
    UPDATED_AT("updated_at"),
    DELETED_AT("deleted_at");

    private final String field;

    SessionOrder(String field) {
        this.field = field;
    }

    public static SessionOrder fromField(String field) {
        if (field == null || field.isBlank()) {
            return CREATED_AT;
        }

        return Arrays.stream(values())
                .filter(order -> order.field.equalsIgnoreCase(field) || order.name().equalsIgnoreCase(field))
                .findFirst()
                .orElse(CREATED_AT);
    }
}