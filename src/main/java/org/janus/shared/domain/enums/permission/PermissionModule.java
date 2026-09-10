package org.janus.shared.domain.enums.permission;

import lombok.Getter;

@Getter
public enum PermissionModule {
    AUTHENTICATION("AUTHENTICATION"),
    AUTHORIZATION("AUTHORIZATION"),
    IDENTITY("IDENTITY"),
    AUDIT("AUDIT"),
    MFA("MFA"),
    RELIABILITY("RELIABILITY");

    private final String value;

    PermissionModule(String value) {
        this.value = value;
    }
}