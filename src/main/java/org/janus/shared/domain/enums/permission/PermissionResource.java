package org.janus.shared.domain.enums.permission;

import lombok.Getter;

@Getter
public enum PermissionResource {
    USER("USER"),
    SESSION("SESSION"),
    ROLE("ROLE"),
    PERMISSION("PERMISSION"),
    AUDIT_LOG("AUDIT_LOG"),
    SYSTEM_SETTING("SYSTEM_SETTING");

    private final String value;

    PermissionResource(String value) {
        this.value = value;
    }
}