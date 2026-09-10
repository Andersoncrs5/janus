package org.janus.modules.authorization.application.dto.permission.filter;

import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.janus.shared.domain.base.filter.FilterBaseDTO;
import org.janus.shared.domain.enums.permission.PermissionModule;
import org.janus.shared.domain.enums.permission.PermissionResource;
import org.janus.shared.domain.enums.permission.PermissionRiskLevel;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionFilterDTO extends FilterBaseDTO {

    @QueryParam("name")
    private String name;

    @QueryParam("slug")
    private String slug;

    @QueryParam("module")
    private Set<PermissionModule> module;

    @QueryParam("resource")
    private Set<PermissionResource> resource;

    @QueryParam("action")
    private String action;

    @QueryParam("riskLevel")
    private Set<PermissionRiskLevel> riskLevel;

    @QueryParam("isActive")
    private Boolean isActive;

    @QueryParam("isSystem")
    private Boolean isSystem;

    @QueryParam("createdBy")
    private UUID createdBy;

    @QueryParam("orders")
    private List<PermissionOrder> orders = List.of(PermissionOrder.CREATED_AT);
}