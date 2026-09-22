package org.janus.modules.authorization.application.dto.permission.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.dto.BaseDTO;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;

import java.util.UUID;

@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDTO extends BaseDTO {

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PermissionModule getModule() {
        return module;
    }

    public void setModule(PermissionModule module) {
        this.module = module;
    }

    public PermissionResource getResource() {
        return resource;
    }

    public void setResource(PermissionResource resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public PermissionRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(PermissionRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Boolean getSystem() {
        return isSystem;
    }

    public void setSystem(Boolean system) {
        isSystem = system;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }
}
