package org.janus.modules.authorization.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.model.BaseEntity;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionEntity extends BaseEntity {

    private String name;
    private String slug;
    private String description;

    private PermissionModule module;
    private PermissionResource resource;
    private String action;
    private PermissionRiskLevel riskLevel;

    private Boolean isActive;
    private Boolean isSystem;

    private String metadata;
    private UUID createdBy;

}