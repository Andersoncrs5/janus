package org.janus.shared.domain.enums.permission;

import lombok.Getter;

@Getter
public enum PermissionResource {
    USER("USER"),
    SESSION("SESSION"),
    ROLE("ROLE"),
    PERMISSION("PERMISSION"),
    ROLE_PERMISSION("ROLE_PERMISSION"),
    AUDIT_LOG("AUDIT_LOG"),
    SYSTEM_SETTING("SYSTEM_SETTING"),
    SYSTEM_METRIC("SYSTEM_METRIC"),
    API_KEY("API_KEY"),
    USER_ROLE("USER_ROLE");

    private final String value;

    PermissionResource(String value) {
        this.value = value;
    }
}