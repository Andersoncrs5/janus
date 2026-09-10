package org.janus.shared.domain.enums.permission;

import lombok.Getter;

@Getter
public enum PermissionRiskLevel {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL");

    private final String value;

    PermissionRiskLevel(String value) {
        this.value = value;
    }
}